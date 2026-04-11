package com.example.login.controller;

import com.example.login.service.LectureService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LectureController {

    private final LectureService lectureService;

    public LectureController(LectureService lectureService) {
        this.lectureService = lectureService;
    }

    /** GET /api/student/lectures — 학생이 수강하는 강의 목록 */
    @GetMapping("/student/lectures")
    public ResponseEntity<?> getStudentLectures(HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(lectureService.getStudentLectures(studentId));
    }

    /** GET /api/teacher/lectures — 강사 담당 강의 목록 */
    @GetMapping("/teacher/lectures")
    public ResponseEntity<?> getTeacherLectures(HttpSession session) {
        Long teacherId = (Long) session.getAttribute("userId");
        if (teacherId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(lectureService.getTeacherLectures(teacherId));
    }

    /** GET /api/teacher/lectures/{lectureId}/seats — 자리 배치 + 출석 현황 */
    @GetMapping("/teacher/lectures/{lectureId}/seats")
    public ResponseEntity<?> getSeats(@PathVariable Long lectureId, HttpSession session) {
        Long teacherId = (Long) session.getAttribute("userId");
        if (teacherId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(lectureService.getSeats(lectureId));
    }

    /** PUT /api/teacher/lectures/{lectureId}/period — 강의 기간 설정 */
    @PutMapping("/teacher/lectures/{lectureId}/period")
    public ResponseEntity<?> updatePeriod(
            @PathVariable Long lectureId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        if (session.getAttribute("userId") == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        LocalDate start = body.get("lectureStart") != null && !body.get("lectureStart").isBlank()
                ? LocalDate.parse(body.get("lectureStart")) : null;
        LocalDate end   = body.get("lectureEnd") != null && !body.get("lectureEnd").isBlank()
                ? LocalDate.parse(body.get("lectureEnd")) : null;

        boolean ok = lectureService.updatePeriod(lectureId, start, end);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
