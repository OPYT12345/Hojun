package com.example.login.service;

import com.example.login.entity.PushSubscription;
import com.example.login.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;

/**
 * Web Push (VAPID) 기반 모바일 푸시 알림 서비스.
 * VAPID 키가 없으면 시작 시 자동 생성하고 로그에 출력합니다.
 * 환경변수 VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY 로 고정하면
 * 서버 재시작 후에도 기존 구독을 유지합니다.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${vapid.public-key:}")
    private String configPublicKey;

    @Value("${vapid.private-key:}")
    private String configPrivateKey;

    @Value("${vapid.subject:mailto:admin@school.local}")
    private String vapidSubject;

    private final PushSubscriptionRepository subscriptionRepo;

    private String activePublicKey;
    private PushService pushService;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    @PostConstruct
    public void init() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        String pubKey  = configPublicKey.isBlank()  ? null : configPublicKey;
        String privKey = configPrivateKey.isBlank() ? null : configPrivateKey;

        if (pubKey == null || privKey == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(new ECGenParameterSpec("secp256r1"));
                KeyPair kp = kpg.generateKeyPair();
                pubKey  = encodePublicKey((ECPublicKey) kp.getPublic());
                privKey = encodePrivateKey((ECPrivateKey) kp.getPrivate());
                log.info("====================================================");
                log.info("VAPID 키 자동 생성 — 서버 재시작 시 새 키가 생성되어");
                log.info("기존 구독이 초기화됩니다. 아래 값을 환경변수로 고정하세요.");
                log.info("VAPID_PUBLIC_KEY={}", pubKey);
                log.info("VAPID_PRIVATE_KEY={}", privKey);
                log.info("====================================================");
            } catch (Throwable e) {
                log.warn("VAPID 키 생성 실패 — 푸시 알림 비활성화: {}", e.getMessage());
                return;
            }
        }

        activePublicKey = pubKey;
        try {
            pushService = new PushService()
                    .setPublicKey(pubKey)
                    .setPrivateKey(privKey)
                    .setSubject(vapidSubject);
        } catch (Throwable e) {
            log.warn("PushService 초기화 실패 — 푸시 알림 비활성화: {}", e.getMessage());
        }
    }

    /** 브라우저에 전달할 VAPID 공개키 (applicationServerKey) */
    public String getPublicKey() {
        return activePublicKey;
    }

    /** 특정 사용자에게 채점 완료 알림 전송 */
    public void notifyGrade(Long studentId, String assignmentTitle, Integer grade, String feedback) {
        if (pushService == null) return;
        List<PushSubscription> subs = subscriptionRepo.findByUserId(studentId);
        if (subs.isEmpty()) return;

        String gradeText = grade != null ? grade + "점" : "채점 완료";
        String body = "\"" + assignmentTitle + "\" — " + gradeText;
        if (feedback != null && !feedback.isBlank()) {
            body += "\n" + feedback.substring(0, Math.min(feedback.length(), 60))
                    + (feedback.length() > 60 ? "…" : "");
        }

        String payload = "{\"title\":\"📊 과제 채점 결과\","
                + "\"body\":" + jsonStr(body) + ","
                + "\"url\":\"/student-classroom.html\"}";

        for (PushSubscription sub : subs) {
            send(sub, payload);
        }
    }

    @Transactional
    public void send(PushSubscription sub, String payload) {
        try {
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes(StandardCharsets.UTF_8)
            );
            var response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 410 || status == 404) {
                // 구독 만료 — DB에서 제거
                subscriptionRepo.deleteByEndpoint(sub.getEndpoint());
                log.debug("만료된 푸시 구독 제거: {}", sub.getEndpoint());
            }
        } catch (Exception e) {
            log.debug("푸시 전송 실패 (무시): {}", e.getMessage());
        }
    }

    // ── 키 인코딩 헬퍼 ──────────────────────────────────────────

    private static String encodePublicKey(ECPublicKey pub) {
        java.security.spec.ECPoint w = pub.getW();
        byte[] x = toBytes32(w.getAffineX());
        byte[] y = toBytes32(w.getAffineY());
        byte[] uncompressed = new byte[65];
        uncompressed[0] = 0x04;
        System.arraycopy(x, 0, uncompressed, 1,  32);
        System.arraycopy(y, 0, uncompressed, 33, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
    }

    private static String encodePrivateKey(ECPrivateKey priv) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toBytes32(priv.getS()));
    }

    private static byte[] toBytes32(BigInteger n) {
        byte[] raw = n.toByteArray();
        if (raw.length == 32) return raw;
        byte[] out = new byte[32];
        if (raw.length > 32) {
            System.arraycopy(raw, raw.length - 32, out, 0, 32);
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        }
        return out;
    }

    private static String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n") + "\"";
    }
}
