package com.example.login.controller;

import com.example.login.dto.LoginResponse;
import com.example.login.dto.UserProfileResponse;
import com.example.login.service.UserProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * GET /api/user/profile
     *
     * 로그인 후 메인 페이지에서 회원 데이터를 불러올 때 호출합니다.
     *
     * 응답 예시 (JSON):
     * {
     *   "username": "hong@example.com",
     *   "name": "홍길동"
     * }
     */
    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).body(LoginResponse.error("로그인이 필요합니다."));
        }

        UserProfileResponse profile = userProfileService.getProfile(userId);

        if (profile == null) {
            return ResponseEntity.status(404).body(LoginResponse.error("회원 정보를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(profile);
    }
}
