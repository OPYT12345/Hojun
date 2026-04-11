package com.example.login.dto;

/**
 * 모든 로그인 응답은 이 구조로 통일합니다.
 * - success          : 로그인 성공 여부
 * - message          : 사용자에게 보여줄 메시지
 * - redirectUrl      : 성공 시 이동할 경로 (실패/잠금 시 null)
 * - locked           : 계정 잠금 여부 (5회 실패 시 true)
 * - remainingSeconds : 잠금 해제까지 남은 초 (잠금 상태일 때만 유효)
 * - remainingAttempts: 남은 로그인 시도 횟수 (실패 시 표시용)
 */
public class LoginResponse {

    private final boolean success;
    private final String message;
    private final String redirectUrl;
    private final boolean locked;
    private final long remainingSeconds;
    private final int remainingAttempts;

    private LoginResponse(boolean success, String message, String redirectUrl,
                          boolean locked, long remainingSeconds, int remainingAttempts) {
        this.success = success;
        this.message = message;
        this.redirectUrl = redirectUrl;
        this.locked = locked;
        this.remainingSeconds = remainingSeconds;
        this.remainingAttempts = remainingAttempts;
    }

    // 로그인 성공
    public static LoginResponse success(String redirectUrl) {
        return new LoginResponse(true, "로그인 성공", redirectUrl, false, 0, 0);
    }

    // 로그인 실패 (잠금 아님)
    public static LoginResponse failure(String message, int remainingAttempts) {
        return new LoginResponse(false, message, null, false, 0, remainingAttempts);
    }

    // 일반 오류 (구조화된 오류 응답 통일용)
    public static LoginResponse error(String message) {
        return new LoginResponse(false, message, null, false, 0, 0);
    }

    // 계정 잠금 (5회 초과)
    public static LoginResponse locked(long remainingSeconds) {
        return new LoginResponse(
            false,
            "로그인 시도 횟수를 초과했습니다. " + remainingSeconds + "초 후 다시 시도할 수 있습니다.",
            null, true, remainingSeconds, 0
        );
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getRedirectUrl() { return redirectUrl; }
    public boolean isLocked() { return locked; }
    public long getRemainingSeconds() { return remainingSeconds; }
    public int getRemainingAttempts() { return remainingAttempts; }
}
