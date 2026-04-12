package com.example.login.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI API 호출 예산 및 횟수 제한 서비스
 *
 * 제한 계층:
 *   1. 사용자별 시간당 최대 호출 횟수 (기본: 20회/시간)
 *   2. 전역 일별 최대 호출 횟수     (기본: 300회/일)
 *
 * 두 조건 중 하나라도 초과하면 호출을 거부하여 OpenAI 비용을 보호합니다.
 * 한도는 application.properties에서 조정 가능합니다.
 */
@Service
public class AiBudgetService {

    @Value("${app.ai.max-calls-per-user-per-hour:20}")
    private int maxPerUserPerHour;

    @Value("${app.ai.max-calls-global-per-day:300}")
    private int maxGlobalPerDay;

    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final DateTimeFormatter DAY_FMT  = DateTimeFormatter.BASIC_ISO_DATE;

    /** "userId_yyyyMMddHH" → 호출 횟수 */
    private final ConcurrentHashMap<String, AtomicLong> userCounters   = new ConcurrentHashMap<>();
    /** "yyyyMMdd" → 전역 호출 횟수 */
    private final ConcurrentHashMap<String, AtomicLong> globalCounters = new ConcurrentHashMap<>();

    /**
     * 호출 허용 여부 확인 후 카운터 증가.
     *
     * @param userId 현재 세션 사용자 ID
     * @return 허용되면 true, 한도 초과면 false
     */
    public boolean tryConsume(Long userId) {
        String dayKey  = LocalDate.now().format(DAY_FMT);
        String hourKey = userId + "_" + LocalDateTime.now().format(HOUR_FMT);

        // ── 전역 일별 한도 확인 ──────────────────────────────────
        AtomicLong globalCount = globalCounters.computeIfAbsent(dayKey, k -> {
            // 이전 날짜 항목 정리 (메모리 누수 방지)
            globalCounters.entrySet().removeIf(e -> !e.getKey().equals(k));
            return new AtomicLong(0);
        });
        if (globalCount.get() >= maxGlobalPerDay) {
            return false;
        }

        // ── 사용자 시간당 한도 확인 (CAS로 원자적 체크·증가) ──────
        AtomicLong userCount = userCounters.computeIfAbsent(hourKey, k -> new AtomicLong(0));
        long prev;
        do {
            prev = userCount.get();
            if (prev >= maxPerUserPerHour) return false;
        } while (!userCount.compareAndSet(prev, prev + 1));

        // 사용자 한도 통과 → 전역 카운터도 증가
        globalCount.incrementAndGet();
        return true;
    }

    /**
     * 현재 사용자의 이번 시간 남은 호출 횟수 반환.
     */
    public int getRemainingCalls(Long userId) {
        String hourKey = userId + "_" + LocalDateTime.now().format(HOUR_FMT);
        AtomicLong c = userCounters.get(hourKey);
        if (c == null) return maxPerUserPerHour;
        return (int) Math.max(0, maxPerUserPerHour - c.get());
    }
}
