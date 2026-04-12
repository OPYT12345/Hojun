package com.example.login.controller;

import com.example.login.entity.PushSubscription;
import com.example.login.repository.PushSubscriptionRepository;
import com.example.login.service.PushNotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushNotificationService pushService;
    private final PushSubscriptionRepository subscriptionRepo;

    public PushController(PushNotificationService pushService,
                          PushSubscriptionRepository subscriptionRepo) {
        this.pushService      = pushService;
        this.subscriptionRepo = subscriptionRepo;
    }

    /**
     * GET /api/push/vapid-public-key
     * 브라우저가 구독 시 사용할 VAPID 애플리케이션 서버 공개키 반환
     */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<?> vapidPublicKey() {
        String key = pushService.getPublicKey();
        if (key == null) {
            return ResponseEntity.status(503).body(
                    Map.of("success", false, "message", "푸시 알림이 설정되지 않았습니다."));
        }
        return ResponseEntity.ok(Map.of("publicKey", key));
    }

    /**
     * POST /api/push/subscribe
     * Body: { endpoint, keys: { p256dh, auth } }
     * 브라우저 구독 정보를 DB에 저장 (로그인 필수)
     */
    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<?> subscribe(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(
                    Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        String endpoint = (String) body.get("endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "endpoint가 없습니다."));
        }

        @SuppressWarnings("unchecked")
        Map<String, String> keys = (Map<String, String>) body.get("keys");
        String p256dh = keys != null ? keys.get("p256dh") : null;
        String auth   = keys != null ? keys.get("auth")   : null;

        // 기존 구독이 있으면 소유권 확인 후 갱신, 없으면 신규 저장
        // 보안: 다른 사용자의 endpoint를 자신의 userId로 덮어써 알림을 탈취하는 공격 방어
        Optional<PushSubscription> existing = subscriptionRepo.findByEndpoint(endpoint);
        if (existing.isPresent() && !userId.equals(existing.get().getUserId())) {
            return ResponseEntity.status(409).body(
                    Map.of("success", false, "message", "이미 다른 계정에 등록된 구독입니다."));
        }
        PushSubscription sub = existing.orElseGet(PushSubscription::new);
        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepo.save(sub);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * DELETE /api/push/unsubscribe
     * Body: { endpoint }
     * 구독 해제 시 DB에서 제거
     */
    @DeleteMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<?> unsubscribe(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(
                    Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        Long userId = (Long) session.getAttribute("userId");

        String endpoint = (String) body.get("endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            // 보안: 자신의 구독만 해제 가능 — 다른 사용자의 구독을 임의로 제거하는 공격 방어
            subscriptionRepo.findByEndpoint(endpoint).ifPresent(sub -> {
                if (userId.equals(sub.getUserId())) {
                    subscriptionRepo.deleteByEndpoint(endpoint);
                }
            });
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
