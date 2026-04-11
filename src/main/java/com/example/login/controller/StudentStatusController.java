package com.example.login.controller;

import com.example.login.dto.SeatDto;
import com.example.login.entity.LectureEnrollment;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentStatusController {

    private final LectureEnrollmentRepository enrollmentRepo;
    private final AttendanceService attendanceService;

    public StudentStatusController(LectureEnrollmentRepository enrollmentRepo,
                                   AttendanceService attendanceService) {
        this.enrollmentRepo    = enrollmentRepo;
        this.attendanceService = attendanceService;
    }

    /** PUT /api/student/status — 상태 설정 */
    @Transactional
    @PutMapping("/status")
    public ResponseEntity<?> setStatus(@RequestBody Map<String, Object> body, HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) return ResponseEntity.status(401).body(fail(null));

        Long lectureId = body.get("lectureId") != null
            ? ((Number) body.get("lectureId")).longValue() : null;
        String status = (String) body.get("status");

        if (lectureId == null || status == null || status.isBlank())
            return ResponseEntity.badRequest().body(fail("잘못된 요청입니다."));

        var opt = enrollmentRepo.findByStudentIdAndLectureId(studentId, lectureId);
        if (opt.isEmpty()) return ResponseEntity.status(403).body(fail("수강 등록된 강의가 아닙니다."));

        LectureEnrollment e = opt.get();
        e.setStatus(status);
        e.setStatusUpdatedAt(LocalDateTime.now());
        enrollmentRepo.save(e);
        broadcastStatus(lectureId, e);
        return ResponseEntity.ok(ok());
    }

    /** DELETE /api/student/status?lectureId=1 — 상태 해제 */
    @Transactional
    @DeleteMapping("/status")
    public ResponseEntity<?> clearStatus(@RequestParam Long lectureId, HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) return ResponseEntity.status(401).body(fail(null));

        var opt = enrollmentRepo.findByStudentIdAndLectureId(studentId, lectureId);
        if (opt.isEmpty()) return ResponseEntity.status(403).body(fail("수강 등록된 강의가 아닙니다."));

        LectureEnrollment e = opt.get();
        e.setStatus(null);
        e.setStatusUpdatedAt(null);
        enrollmentRepo.save(e);
        broadcastStatus(lectureId, e);
        return ResponseEntity.ok(ok());
    }

    /** GET /api/student/status?lectureId=1 — 현재 내 상태 조회 */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam Long lectureId, HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) return ResponseEntity.status(401).body(fail(null));

        Map<String, Object> result = new HashMap<>();
        var opt = enrollmentRepo.findByStudentIdAndLectureId(studentId, lectureId);
        if (opt.isPresent()) {
            LectureEnrollment e = opt.get();
            result.put("status", e.getStatus() != null ? e.getStatus() : "");
            result.put("updatedAt", e.getStatusUpdatedAt() != null
                ? e.getStatusUpdatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        } else {
            result.put("status", "");
            result.put("updatedAt", "");
        }
        return ResponseEntity.ok(result);
    }

    private void broadcastStatus(Long lectureId, LectureEnrollment e) {
        SeatDto dto = new SeatDto(
            e.getSeatNum(),
            e.getStudent().getId(),
            e.getStudent().getName(),
            e.getStudent().getStudentNumber(),
            false, null
        );
        dto.setStatus(e.getStatus());
        attendanceService.broadcastStatus(lectureId, dto);
    }

    private Map<String, Object> ok() {
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        return m;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        if (message != null) m.put("message", message);
        return m;
    }
}
