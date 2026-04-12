package com.example.login.controller;

import com.example.login.entity.Announcement;
import com.example.login.entity.User;
import com.example.login.repository.AnnouncementRepository;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.repository.LectureRepository;
import com.example.login.repository.UserRepository;
import com.example.login.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lecture/{lectureId}/announcements")
public class AnnouncementController {

    private final AnnouncementRepository      announcementRepo;
    private final LectureRepository           lectureRepo;
    private final LectureEnrollmentRepository enrollmentRepo;
    private final UserRepository              userRepo;
    private final NotificationService         notificationService;

    public AnnouncementController(AnnouncementRepository announcementRepo,
                                  LectureRepository lectureRepo,
                                  LectureEnrollmentRepository enrollmentRepo,
                                  UserRepository userRepo,
                                  NotificationService notificationService) {
        this.announcementRepo  = announcementRepo;
        this.lectureRepo       = lectureRepo;
        this.enrollmentRepo    = enrollmentRepo;
        this.userRepo          = userRepo;
        this.notificationService = notificationService;
    }

    // ── GET /api/lecture/{lectureId}/announcements ─────────────────
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        // RLS: 수강 등록된 학생 또는 담당 강사만 공지 열람 가능
        if (!canAccessLecture(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 접근 권한이 없습니다."));
        }
        List<Announcement> list = announcementRepo.findByLectureIdOrderByCreatedAtDesc(lectureId);
        return ResponseEntity.ok(list.stream().map(this::toMap).toList());
    }

    // ── POST /api/lecture/{lectureId}/announcements ────────────────
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable Long lectureId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 공지를 등록할 수 있습니다."));
        }
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }
        String title = (String) body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "제목을 입력하세요."));
        }

        Announcement ann = new Announcement();
        ann.setLectureId(lectureId);
        ann.setTitle(title.trim());
        ann.setContent((String) body.getOrDefault("content", ""));
        announcementRepo.save(ann);

        // 수강생 전원에게 알림 발송
        String lectureName = lectureRepo.findById(lectureId)
                .map(l -> l.getName()).orElse("강의");
        List<Long> studentIds = enrollmentRepo.findStudentIdsByLectureId(lectureId);
        for (Long studentId : studentIds) {
            notificationService.createBroadcastNotification(studentId, lectureName, title.trim());
        }

        return ResponseEntity.ok(Map.of("success", true, "id", ann.getId()));
    }

    // ── DELETE /api/lecture/{lectureId}/announcements/{id} ─────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long lectureId,
            @PathVariable Long id,
            HttpSession session) {

        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 삭제할 수 있습니다."));
        }
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }
        Optional<Announcement> opt = announcementRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        if (!lectureId.equals(opt.get().getLectureId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "잘못된 접근입니다."));
        }
        announcementRepo.delete(opt.get());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── helpers ───────────────────────────────────────────────────

    /** RLS 검사: 수강 등록된 학생 또는 담당 강사 */
    private boolean canAccessLecture(Long lectureId, Long userId) {
        if (lectureRepo.existsByIdAndTeacherId(lectureId, userId)) return true;
        return enrollmentRepo.existsByStudentIdAndLectureId(userId, lectureId);
    }

    private boolean isTeacher(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return false;
        return userRepo.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER)
                .orElse(false);
    }

    private boolean isLectureOwner(Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return false;
        return lectureRepo.existsByIdAndTeacherId(lectureId, userId);
    }

    private Map<String, Object> toMap(Announcement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        a.getId());
        m.put("lectureId", a.getLectureId());
        m.put("title",     a.getTitle());
        m.put("content",   a.getContent());
        m.put("createdAt", a.getCreatedAtStr());
        return m;
    }
}
