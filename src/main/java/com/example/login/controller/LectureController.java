package com.example.login.controller;

import com.example.login.entity.User;
import com.example.login.repository.UserRepository;
import com.example.login.service.AttendanceService;
import com.example.login.service.LectureService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LectureController {

    private final LectureService    lectureService;
    private final UserRepository    userRepository;
    private final AttendanceService attendanceService;

    public LectureController(LectureService lectureService,
                             UserRepository userRepository,
                             AttendanceService attendanceService) {
        this.lectureService    = lectureService;
        this.userRepository    = userRepository;
        this.attendanceService = attendanceService;
    }

    /** GET /api/student/lectures — 학생이 수강하는 강의 목록 */
    @GetMapping("/student/lectures")
    public ResponseEntity<?> getStudentLectures(HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        return ResponseEntity.ok(lectureService.getStudentLectures(studentId));
    }

    /** GET /api/teacher/lectures — 강사 담당 강의 목록 */
    @GetMapping("/teacher/lectures")
    public ResponseEntity<?> getTeacherLectures(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        return ResponseEntity.ok(lectureService.getTeacherLectures(userId));
    }

    /** GET /api/teacher/lectures/{lectureId}/seats — 자리 배치 + 출석 현황 (강사 + 해당 강의 소유자만) */
    @GetMapping("/teacher/lectures/{lectureId}/seats")
    public ResponseEntity<?> getSeats(@PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher)
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 조회할 수 있습니다."));

        if (!lectureService.isOwnedByTeacher(lectureId, userId))
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));

        return ResponseEntity.ok(lectureService.getSeats(lectureId));
    }

    /** PUT /api/teacher/lectures/{lectureId}/period — 강의 기간 설정 (강의 소유 강사만) */
    @PutMapping("/teacher/lectures/{lectureId}/period")
    public ResponseEntity<?> updatePeriod(
            @PathVariable Long lectureId,
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher)
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 수정할 수 있습니다."));

        if (!lectureService.isOwnedByTeacher(lectureId, userId))
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));

        LocalDate start, end;
        try {
            start = parseDate(body.get("lectureStart"));
            end   = parseDate(body.get("lectureEnd"));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "날짜 형식이 올바르지 않습니다. (예: 2026-04-01)"));
        }

        boolean ok = lectureService.updatePeriod(lectureId, start, end);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** GET /api/teacher/lectures/{lectureId}/students — 수강 학생 목록 + 출석/과제 요약 (강의 소유 강사만) */
    @GetMapping("/teacher/lectures/{lectureId}/students")
    public ResponseEntity<?> getStudents(@PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        if (!lectureService.isOwnedByTeacher(lectureId, userId))
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));

        return ResponseEntity.ok(attendanceService.getStudentSummaries(lectureId));
    }

    /** GET /api/teacher/lectures/{lectureId}/students/{studentId}/attendance — 특정 학생 출석 이력 (강의 소유 강사만) */
    @GetMapping("/teacher/lectures/{lectureId}/students/{studentId}/attendance")
    public ResponseEntity<?> getStudentAttendance(@PathVariable Long lectureId,
                                                  @PathVariable Long studentId,
                                                  HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        if (!lectureService.isOwnedByTeacher(lectureId, userId))
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));

        return ResponseEntity.ok(attendanceService.getAttendanceHistory(studentId, lectureId));
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s);
    }
}
