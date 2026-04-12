package com.example.login.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * IP 기반 Rate Limiting 필터 — Hydra / 무차별 대입 / DDoS 방어
 *
 * 슬라이딩 윈도우(1분) 방식:
 *   - 인증/가입 엔드포인트 : IP당 분당 30 요청
 *   - 일반 API             : IP당 분당 200 요청
 *
 * 주의: X-Forwarded-For는 Railway/Render 같은 신뢰된 프록시 환경을 가정합니다.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS      = 60_000L;   // 1분 슬라이딩 윈도우
    private static final int  GENERAL_LIMIT  = 200;       // 일반 IP 한도 (분당)
    private static final int  AUTH_LIMIT     = 30;        // 인증 엔드포인트 한도 (분당)
    private static final long CLEANUP_EVERY  = 5 * 60_000L; // 5분마다 만료 항목 정리

    /** IP → 요청 타임스탬프 슬라이딩 큐 */
    private final Map<String, Deque<Long>> generalBucket = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> authBucket    = new ConcurrentHashMap<>();

    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String ip   = extractClientIp(req);
        String path = req.getRequestURI();
        long   now  = System.currentTimeMillis();

        periodicCleanup(now);

        // 인증/가입 엔드포인트 — 엄격 제한
        if (isAuthPath(path)) {
            if (!allowRequest(authBucket, ip, now, AUTH_LIMIT)) {
                reject(res, "너무 많은 시도입니다. 잠시 후 다시 시도해주세요.");
                return;
            }
        }

        // 전체 엔드포인트 — 일반 제한
        if (!allowRequest(generalBucket, ip, now, GENERAL_LIMIT)) {
            reject(res, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        chain.doFilter(req, res);
    }

    /** 슬라이딩 윈도우 내 요청 허용 여부 판단 + 타임스탬프 기록 */
    private boolean allowRequest(Map<String, Deque<Long>> bucket, String ip,
                                 long now, int limit) {
        Deque<Long> ts = bucket.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        long windowStart = now - WINDOW_MS;

        // 윈도우 밖 타임스탬프 제거
        while (!ts.isEmpty() && ts.peekFirst() <= windowStart) {
            ts.pollFirst();
        }
        if (ts.size() >= limit) {
            return false;
        }
        ts.addLast(now);
        return true;
    }

    /** 인증/가입 경로 판별 */
    private boolean isAuthPath(String path) {
        return path.startsWith("/api/student/login")
            || path.startsWith("/api/teacher/login")
            || path.startsWith("/api/student/signup")
            || path.startsWith("/api/teacher/signup")
            || path.startsWith("/api/login/status")
            || path.startsWith("/api/logout");
    }

    /**
     * 실제 클라이언트 IP 추출.
     *
     * X-Forwarded-For 스푸핑 방어:
     *   remoteAddr 이 사설/루프백 주소일 때(= 신뢰된 프록시를 통한 요청)만
     *   X-Forwarded-For 헤더를 신뢰합니다.
     *   직접 연결된 클라이언트가 X-Forwarded-For를 임의로 설정해도
     *   실제 소켓 주소(remoteAddr)가 사설 IP가 아니면 무시됩니다.
     */
    private String extractClientIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    /**
     * 사설망/루프백 주소 여부 판별.
     * Railway, Render 등 PaaS 프록시는 내부 사설 IP를 사용하므로 true 반환.
     * 공인 IP에서 직접 연결된 경우 false → X-Forwarded-For 무시.
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        return ip.equals("127.0.0.1")
            || ip.equals("0:0:0:0:0:0:0:1")
            || ip.equals("::1")
            || ip.startsWith("10.")
            || ip.startsWith("192.168.")
            || isRfc1918_172(ip);
    }

    /** 172.16.0.0/12 범위 판별 */
    private boolean isRfc1918_172(String ip) {
        if (!ip.startsWith("172.")) return false;
        try {
            int second = Integer.parseInt(ip.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    private void reject(HttpServletResponse res, String message) throws IOException {
        res.setStatus(429);
        res.setContentType("application/json; charset=UTF-8");
        res.setHeader("Retry-After", "60");
        res.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    /** 5분마다 오래된 IP 항목 정리 (메모리 누수 방지) */
    private void periodicCleanup(long now) {
        if (now - lastCleanup < CLEANUP_EVERY) return;
        lastCleanup = now;
        long windowStart = now - WINDOW_MS;
        purgeBucket(generalBucket, windowStart);
        purgeBucket(authBucket, windowStart);
    }

    private void purgeBucket(Map<String, Deque<Long>> bucket, long windowStart) {
        bucket.entrySet().removeIf(entry -> {
            Deque<Long> q = entry.getValue();
            while (!q.isEmpty() && q.peekFirst() <= windowStart) q.pollFirst();
            return q.isEmpty();
        });
    }
}
