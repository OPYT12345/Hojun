package com.example.login.service;

import com.example.login.dto.AttendanceResponse;
import com.example.login.dto.SeatDto;
import com.example.login.entity.Attendance;
import com.example.login.entity.Lecture;
import com.example.login.entity.LectureEnrollment;
import com.example.login.entity.User;
import com.example.login.repository.AssignmentRepository;
import com.example.login.repository.AttendanceRepository;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.repository.SubmissionRepository;
import com.example.login.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final LectureEnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final LectureService lectureService;
    private final ObjectMapper objectMapper;
    private final SubmissionRepository submissionRepo;
    private final AssignmentRepository assignmentRepo;

    // lectureId → 연결된 SSE 이미터 목록 (강사 브라우저들)
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public AttendanceService(AttendanceRepository attendanceRepo,
                             LectureEnrollmentRepository enrollmentRepo,
                             UserRepository userRepo,
                             LectureService lectureService,
                             ObjectMapper objectMapper,
                             SubmissionRepository submissionRepo,
                             AssignmentRepository assignmentRepo) {
        this.attendanceRepo = attendanceRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.userRepo = userRepo;
        this.lectureService = lectureService;
        this.objectMapper = objectMapper;
        this.submissionRepo = submissionRepo;
        this.assignmentRepo = assignmentRepo;
    }

    /** 강사 브라우저가 SSE 구독 */
    public SseEmitter subscribe(Long lectureId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = no timeout
        CopyOnWriteArrayList<SseEmitter> list =
            emitters.computeIfAbsent(lectureId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onError(e -> list.remove(emitter));

        // 연결 직후 핑 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {}

        return emitter;
    }

    /** 활성 강의 자동 탐색 후 출석 처리 */
    @Transactional
    public AttendanceResponse checkInAuto(Long studentId) {
        List<LectureEnrollment> enrollments = enrollmentRepo.findByStudentId(studentId);
        List<LectureEnrollment> active = enrollments.stream()
            .filter(e -> e.getLecture().isActive())
            .toList();

        if (active.isEmpty())
            return new AttendanceResponse(false, "현재 진행 중인 강의가 없습니다.", false, null);
        if (active.size() > 1)
            return new AttendanceResponse(false, "진행 중인 강의가 여러 개입니다. 강의실 NFC를 다시 태그해 주세요.", false, null);

        return checkIn(studentId, active.get(0).getLecture().getId());
    }

    /** 학생 출석 처리 */
    @Transactional
    public AttendanceResponse checkIn(Long studentId, Long lectureId) {
        LocalDate today = LocalDate.now();

        // 이미 오늘 출석했는지 확인
        if (attendanceRepo.existsByStudentIdAndLectureIdAndAttendDate(studentId, lectureId, today)) {
            return new AttendanceResponse(false, "오늘은 이미 출석하셨습니다.", true, null);
        }

        // 수강 등록 확인
        Optional<LectureEnrollment> enrollOpt =
            enrollmentRepo.findByStudentIdAndLectureId(studentId, lectureId);
        if (enrollOpt.isEmpty()) {
            return new AttendanceResponse(false, "수강 등록된 강의가 아닙니다.", false, null);
        }

        // 강의 존재 확인
        Lecture lecture = lectureService.findById(lectureId);
        User student = userRepo.findById(studentId).orElse(null);
        if (lecture == null || student == null) {
            return new AttendanceResponse(false, "잘못된 요청입니다.", false, null);
        }
        if (student.getRole() != User.Role.STUDENT) {
            return new AttendanceResponse(false, "학생 계정만 출석할 수 있습니다.", false, null);
        }

        // 출석 저장
        LocalDateTime now = LocalDateTime.now();
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setLecture(lecture);
        attendance.setAttendedAt(now);
        attendance.setAttendDate(today);
        attendanceRepo.save(attendance);

        // SeatDto 생성
        LectureEnrollment enroll = enrollOpt.get();
        String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        SeatDto seat = new SeatDto(
            enroll.getSeatNum(),
            studentId,
            student.getName(),
            student.getStudentNumber(),
            true,
            timeStr
        );

        // 강사에게 SSE 알림
        notifyTeacher(lectureId, seat);

        return new AttendanceResponse(true, "출석이 완료되었습니다! ✅", false, seat);
    }

    /** 학생 출석 이력 (강의 일정상 날짜 기준으로 출석/결석 목록) */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAttendanceHistory(Long studentId, Long lectureId) {
        // 실제 출석 기록을 항상 먼저 조회
        Map<LocalDate, String> attendedMap = attendanceRepo
            .findByStudentIdAndLectureId(studentId, lectureId)
            .stream()
            .collect(Collectors.toMap(
                Attendance::getAttendDate,
                a -> a.getAttendedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                (a, b) -> a
            ));

        var lecture = lectureService.findById(lectureId);

        // 강의 기간 + 일정이 설정된 경우 → 전체 일정 타임라인 (출석+결석 포함)
        if (lecture != null && lecture.getLectureStart() != null) {
            Set<DayOfWeek> scheduledDays = parseScheduleDays(lecture.getSchedule());
            if (!scheduledDays.isEmpty()) {
                LocalDate start = lecture.getLectureStart();
                LocalDate today = LocalDate.now();
                // 미래 날짜는 포함하지 않음 (미래가 전부 "결석"으로 보이는 문제 방지)
                LocalDate end = (lecture.getLectureEnd() != null && lecture.getLectureEnd().isBefore(today))
                        ? lecture.getLectureEnd() : today;

                List<Map<String, Object>> result = new ArrayList<>();
                Set<LocalDate> includedDates = new HashSet<>();

                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    if (!scheduledDays.contains(d.getDayOfWeek())) continue;
                    String time = attendedMap.get(d);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date",     d.toString());
                    row.put("dayName",  koreanDay(d.getDayOfWeek()));
                    row.put("attended", time != null);
                    if (time != null) row.put("time", time);
                    result.add(row);
                    includedDates.add(d);
                }

                // 스케줄 외 날짜에 출석한 기록도 누락 없이 포함
                for (Map.Entry<LocalDate, String> e : attendedMap.entrySet()) {
                    if (!includedDates.contains(e.getKey())) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("date",     e.getKey().toString());
                        row.put("dayName",  koreanDay(e.getKey().getDayOfWeek()));
                        row.put("attended", true);
                        row.put("time",     e.getValue());
                        result.add(row);
                    }
                }

                result.sort(Comparator.comparing(r -> (String) r.get("date")));
                return result;
            }
        }

        // Fallback: 기간/일정 미설정 시 실제 출석 기록만 표시
        return attendedMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date",     e.getKey().toString());
                row.put("dayName",  koreanDay(e.getKey().getDayOfWeek()));
                row.put("attended", true);
                row.put("time",     e.getValue());
                return (Map<String, Object>) row;
            })
            .toList();
    }

    /** 강사용 — 강의 수강생 전체 출석/과제 요약 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentSummaries(Long lectureId) {
        var lecture = lectureService.findById(lectureId);
        if (lecture == null) return List.of();

        // 오늘까지 진행된 수업 횟수 계산
        Set<DayOfWeek> scheduledDays = parseScheduleDays(lecture.getSchedule());
        int totalSessions = 0;
        if (lecture.getLectureStart() != null && !scheduledDays.isEmpty()) {
            LocalDate start = lecture.getLectureStart();
            LocalDate today = LocalDate.now();
            LocalDate end = (lecture.getLectureEnd() != null && lecture.getLectureEnd().isBefore(today))
                    ? lecture.getLectureEnd() : today;
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (scheduledDays.contains(d.getDayOfWeek())) totalSessions++;
            }
        }

        // 전체 과제 수
        int totalAssignments = assignmentRepo.findByLectureIdOrderByCreatedAtDesc(lectureId).size();
        int finalTotal = totalSessions;
        int finalAsgTotal = totalAssignments;

        return enrollmentRepo.findByLectureId(lectureId).stream().map(e -> {
            User s = e.getStudent();
            int attended  = attendanceRepo.findByStudentIdAndLectureId(s.getId(), lectureId).size();
            int submitted = submissionRepo.findByLectureIdAndStudentId(lectureId, s.getId()).size();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId",       s.getId());
            row.put("name",            s.getName());
            row.put("email",           s.getEmail());
            row.put("studentNumber",   s.getStudentNumber());
            row.put("seatNum",         e.getSeatNum());
            row.put("attendedCount",   attended);
            row.put("totalSessions",   finalTotal);
            row.put("submissionCount", submitted);
            row.put("totalAssignments",finalAsgTotal);
            return (Map<String, Object>) row;
        }).toList();
    }

    private Set<DayOfWeek> parseScheduleDays(String schedule) {
        if (schedule == null || schedule.isBlank()) return Set.of();
        Matcher m = Pattern.compile("^([^\\s]+)\\s+").matcher(schedule.trim());
        if (!m.find()) return Set.of();
        Set<DayOfWeek> days = new HashSet<>();
        for (String token : m.group(1).split("[\u00B7\u30FB]")) {
            DayOfWeek dow = koreanToDow(token.trim());
            if (dow != null) days.add(dow);
        }
        return days;
    }

    private DayOfWeek koreanToDow(String k) {
        return switch (k) {
            case "월" -> DayOfWeek.MONDAY;
            case "화" -> DayOfWeek.TUESDAY;
            case "수" -> DayOfWeek.WEDNESDAY;
            case "목" -> DayOfWeek.THURSDAY;
            case "금" -> DayOfWeek.FRIDAY;
            case "토" -> DayOfWeek.SATURDAY;
            case "일" -> DayOfWeek.SUNDAY;
            default  -> null;
        };
    }

    private String koreanDay(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "월";
            case TUESDAY   -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY  -> "목";
            case FRIDAY    -> "금";
            case SATURDAY  -> "토";
            case SUNDAY    -> "일";
        };
    }

    /** SSE로 강사 브라우저에 상태 변경 전송 (학생이 상태 설정/해제 시 호출) */
    public void broadcastStatus(Long lectureId, SeatDto seatDto) {
        sendEvent(lectureId, "status-update", seatDto);
    }

    /** SSE로 강사 브라우저에 자리 업데이트 전송 */
    private void notifyTeacher(Long lectureId, SeatDto seatDto) {
        sendEvent(lectureId, "seat-update", seatDto);
    }

    private void sendEvent(Long lectureId, String eventName, SeatDto seatDto) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(lectureId);
        if (list == null || list.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(seatDto);
        } catch (Exception e) {
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json));
            } catch (Exception ex) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }
}
