package com.example.login.controller;

import com.example.login.dto.LoginResponse;
import com.example.login.dto.UserProfileResponse;
import com.example.login.entity.User;
import com.example.login.repository.UserRepository;
import com.example.login.service.UserProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserProfileController(UserProfileService userProfileService,
                                  UserRepository userRepository,
                                  BCryptPasswordEncoder passwordEncoder) {
        this.userProfileService = userProfileService;
        this.userRepository     = userRepository;
        this.passwordEncoder    = passwordEncoder;
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

    /**
     * PUT /api/user/password
     * 로그인된 사용자 비밀번호 변경 (현재 비밀번호 확인 후 변경)
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "현재 비밀번호를 입력하세요."));
        }
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "새 비밀번호는 8자 이상이어야 합니다."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "현재 비밀번호가 일치하지 않습니다."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 변경되었습니다."));
    }
}
