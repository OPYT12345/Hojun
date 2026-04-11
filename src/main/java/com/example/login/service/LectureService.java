package com.example.login.service;

import com.example.login.dto.LectureDto;
import com.example.login.dto.SeatDto;
import com.example.login.entity.Attendance;
import com.example.login.entity.Lecture;
import com.example.login.entity.LectureEnrollment;
import com.example.login.entity.User;
import com.example.login.repository.AttendanceRepository;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.repository.LectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LectureService {

    private final LectureRepository lectureRepo;
    private final LectureEnrollmentRepository enrollmentRepo;
    private final AttendanceRepository attendanceRepo;

    public LectureService(LectureRepository lectureRepo,
                          LectureEnrollmentRepository enrollmentRepo,
                          AttendanceRepository attendanceRepo) {
        this.lectureRepo = lectureRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.attendanceRepo = attendanceRepo;
    }

    /** 학생이 수강하는 강의 목록 (오늘 출석 여부 포함) */
    @Transactional(readOnly = true)
    public List<LectureDto> getStudentLectures(Long studentId) {
        LocalDate today = LocalDate.now();
        List<LectureEnrollment> enrollments = enrollmentRepo.findByStudentId(studentId);

        return enrollments.stream().map(e -> {
            Lecture l = e.getLecture();
            LectureDto dto = toDto(l);
            dto.setAttendedToday(
                attendanceRepo.existsByStudentIdAndLectureIdAndAttendDate(studentId, l.getId(), today)
            );
            return dto;
        }).collect(Collectors.toList());
    }

    /** 강사가 담당하는 강의 목록 (오늘 출석/전체 인원 포함) */
    @Transactional(readOnly = true)
    public List<LectureDto> getTeacherLectures(Long teacherId) {
        LocalDate today = LocalDate.now();
        List<Lecture> lectures = lectureRepo.findByTeacherId(teacherId);

        return lectures.stream().map(l -> {
            LectureDto dto = toDto(l);
            int total = (int) enrollmentRepo.findByLectureId(l.getId()).stream()
                    .filter(e -> e.getStudent().getRole() == User.Role.STUDENT)
                    .count();
            int present = (int) attendanceRepo.findByLectureIdAndAttendDate(l.getId(), today).stream()
                    .filter(a -> a.getStudent().getRole() == User.Role.STUDENT)
                    .count();
            dto.setTotalCount(total);
            dto.setPresentCount(present);
            return dto;
        }).collect(Collectors.toList());
    }

    /** 특정 강의의 자리 배치 (출석 여부 포함) */
    @Transactional(readOnly = true)
    public List<SeatDto> getSeats(Long lectureId) {
        LocalDate today = LocalDate.now();
        List<LectureEnrollment> enrollments = enrollmentRepo.findByLectureId(lectureId)
                .stream()
                .filter(e -> e.getStudent().getRole() == User.Role.STUDENT)
                .collect(Collectors.toList());
        List<Attendance> todayAtt = attendanceRepo.findByLectureIdAndAttendDate(lectureId, today);

        // studentId → attendedAt 시간 문자열 맵
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        Map<Long, String> attMap = todayAtt.stream().collect(
            Collectors.toMap(
                a -> a.getStudent().getId(),
                a -> a.getAttendedAt().format(fmt)
            )
        );
        Set<Long> presentIds = attMap.keySet();

        return enrollments.stream().map(e -> {
            Long sid = e.getStudent().getId();
            boolean present = presentIds.contains(sid);
            SeatDto dto = new SeatDto(
                e.getSeatNum(),
                sid,
                e.getStudent().getName(),
                e.getStudent().getStudentNumber(),
                present,
                present ? attMap.get(sid) : null
            );
            dto.setStatus(e.getStatus());
            return dto;
        })
        .sorted(Comparator.comparingInt(SeatDto::getSeatNum))
        .collect(Collectors.toList());
    }

    public Lecture findById(Long id) {
        return lectureRepo.findById(id).orElse(null);
    }

    private LectureDto toDto(Lecture l) {
        LectureDto dto = new LectureDto(
            l.getId(), l.getName(), l.getCode(),
            l.getRoom(), l.getSchedule(),
            l.getTeacher().getName(),
            l.getRows(), l.getCols()
        );
        dto.setActive(l.isActive());
        if (l.getLectureStart() != null) dto.setLectureStart(l.getLectureStart().toString());
        if (l.getLectureEnd()   != null) dto.setLectureEnd(l.getLectureEnd().toString());
        return dto;
    }

    @Transactional
    public boolean updatePeriod(Long lectureId, LocalDate start, LocalDate end) {
        return lectureRepo.findById(lectureId).map(l -> {
            l.setLectureStart(start);
            l.setLectureEnd(end);
            lectureRepo.save(l);
            return true;
        }).orElse(false);
    }
}
