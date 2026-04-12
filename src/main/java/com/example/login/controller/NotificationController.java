package com.example.login.controller;

import com.example.login.entity.Notification;
import com.example.login.repository.NotificationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    /** 내 알림 목록 (최신순 최대 30개) */
    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        List<Notification> all = repo.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = all.stream().limit(30).map(n -> Map.<String, Object>of(
                "id",        n.getId(),
                "type",      n.getType(),
                "title",     n.getTitle(),
                "message",   n.getMessage() != null ? n.getMessage() : "",
                "read",      n.isRead(),
                "createdAt", n.getCreatedAtStr()
        )).toList();
        return ResponseEntity.ok(result);
    }

    /** 읽지 않은 알림 수 */
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.ok(Map.of("count", 0));
        return ResponseEntity.ok(Map.of("count", repo.countByUserIdAndReadFalse(userId)));
    }

    /** 단일 알림 읽음 처리 */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        repo.findById(id).ifPresent(n -> {
            if (userId.equals(n.getUserId())) {
                n.setRead(true);
                repo.save(n);
            }
        });
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 전체 읽음 처리 */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        repo.markAllReadByUserId(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
