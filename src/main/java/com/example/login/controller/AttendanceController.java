package com.example.login.controller;

import com.example.login.dto.AttendanceResponse;
import com.example.login.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * POST /api/attendance/checkin
     * Body: { "lectureId": 1 }  (NFC 태그 접근 시 선택적으로 "seatId" 포함 가능)
     */
    @PostMapping("/checkin")
    public ResponseEntity<AttendanceResponse> checkIn(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null)
            return ResponseEntity.status(401).body(
                new AttendanceResponse(false, "로그인이 필요합니다.", false, null));

        Object lecIdObj = body.get("lectureId");
        if (lecIdObj != null) {
            Long lectureId = ((Number) lecIdObj).longValue();
            return ResponseEntity.ok(attendanceService.checkIn(studentId, lectureId));
        }

        // lectureId 없으면 활성 강의 자동 탐색
        return ResponseEntity.ok(attendanceService.checkInAuto(studentId));
    }

    /**
     * GET /api/attendance/history/{lectureId}
     * 학생 본인의 출석 이력 (강의 일정 날짜 기준, 출석/결석 포함)
     */
    @GetMapping("/history/{lectureId}")
    public ResponseEntity<?> history(@PathVariable Long lectureId, HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(studentId, lectureId));
    }

    /**
     * GET /api/attendance/stream/{lectureId}
     * 강사 브라우저가 SSE 구독 — 학생 출석 시 실시간 알림
     */
    @GetMapping(value = "/stream/{lectureId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long lectureId) {
        return attendanceService.subscribe(lectureId);
    }
}
