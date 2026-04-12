package com.example.login.util;

import java.util.Optional;

/**
 * JSON Map 본문에서 숫자·문자열을 안전하게 꺼낼 때 사용합니다.
 */
public final class MapRequestUtils {

    private MapRequestUtils() {}

    /** Number, 정수 형태 문자열 등을 Long으로 변환합니다. 실패 시 empty. */
    public static Optional<Long> parseLong(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number n) {
            return Optional.of(n.longValue());
        }
        if (value instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** 상태 등 문자열 필드 — String이 아니면 toString으로 정규화합니다. */
    public static Optional<String> parseStringContent(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String s) {
            return Optional.of(s);
        }
        return Optional.of(String.valueOf(value));
    }
}
