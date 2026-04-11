package com.example.login.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 시도 횟수를 추적하고 잠금을 관리하는 서비스
 * - 5회 실패 시 5분간 로그인 잠금
 * - 서버 메모리에 저장 (서버 재시작 시 초기화됨)
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;       // 최대 시도 횟수
    private static final long LOCKOUT_MINUTES = 5;   // 잠금 시간 (분)

    // username → 실패 횟수
    private final Map<String, Integer> attemptCount = new ConcurrentHashMap<>();

    // username → 잠금 시작 시각
    private final Map<String, LocalDateTime> lockoutStartTime = new ConcurrentHashMap<>();

    /** 현재 잠금 상태인지 확인 */
    public boolean isLocked(String username) {
        if (!lockoutStartTime.containsKey(username)) {
            return false;
        }
        LocalDateTime unlockTime = lockoutStartTime.get(username).plusMinutes(LOCKOUT_MINUTES);
        if (LocalDateTime.now().isAfter(unlockTime)) {
            // 잠금 시간이 지났으면 초기화
            reset(username);
            return false;
        }
        return true;
    }

    /** 잠금 해제까지 남은 초 반환 */
    public long getRemainingSeconds(String username) {
        if (!lockoutStartTime.containsKey(username)) {
            return 0;
        }
        LocalDateTime unlockTime = lockoutStartTime.get(username).plusMinutes(LOCKOUT_MINUTES);
        long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), unlockTime);
        return Math.max(remaining, 0);
    }

    /** 로그인 실패 기록 */
    public void recordFailure(String username) {
        int count = attemptCount.getOrDefault(username, 0) + 1;
        attemptCount.put(username, count);
        if (count >= MAX_ATTEMPTS) {
            lockoutStartTime.put(username, LocalDateTime.now());
        }
    }

    /** 남은 시도 횟수 반환 */
    public int getRemainingAttempts(String username) {
        int used = attemptCount.getOrDefault(username, 0);
        return Math.max(MAX_ATTEMPTS - used, 0);
    }

    /** 로그인 성공 시 기록 초기화 */
    public void recordSuccess(String username) {
        reset(username);
    }

    private void reset(String username) {
        attemptCount.remove(username);
        lockoutStartTime.remove(username);
    }
}
