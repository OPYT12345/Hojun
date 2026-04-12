package com.example.login.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 보안 응답 헤더 추가 필터
 *
 * 방어 범위:
 *   - X-Content-Type-Options   : MIME 타입 스니핑 차단
 *   - X-Frame-Options          : Clickjacking 차단
 *   - X-XSS-Protection         : 레거시 브라우저 XSS 필터 활성화
 *   - Referrer-Policy          : Referer 헤더 유출 최소화
 *   - Strict-Transport-Security: HTTP 다운그레이드 공격 방어 (HTTPS 서버 전용)
 *   - Cache-Control            : API 응답 캐시 금지 (민감 데이터 브라우저 캐시 방지)
 *   - Permissions-Policy       : 불필요한 브라우저 API 비활성화
 */
@Component
@Order(2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        // MIME 타입 스니핑 방지
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Clickjacking 방지 — 이 앱은 iframe에 삽입될 필요가 없음
        res.setHeader("X-Frame-Options", "DENY");

        // 레거시 브라우저 XSS 필터
        res.setHeader("X-XSS-Protection", "1; mode=block");

        // Referer 헤더 유출 최소화
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // HSTS — 이미 HTTPS 전용 서버이므로 적용
        res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 불필요한 브라우저 기능 비활성화 (camera 등은 NFC 기능에 불필요)
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // API 응답은 브라우저/프록시 캐시 금지 (인증 정보 유출 방지)
        if (req.getRequestURI().startsWith("/api/")) {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            res.setHeader("Pragma", "no-cache");
        }

        chain.doFilter(req, res);
    }
}
