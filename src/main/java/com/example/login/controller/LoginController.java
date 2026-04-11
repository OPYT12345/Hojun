package com.example.login.controller;

import com.example.login.dto.LoginRequest;
import com.example.login.dto.LoginResponse;
import com.example.login.entity.User;
import com.example.login.service.LoginAttemptService;
import com.example.login.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final LoginService loginService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.redirect.success-url}")
    private String successUrl;

    public LoginController(LoginService loginService, LoginAttemptService loginAttemptService) {
        this.loginService = loginService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * POST /api/teacher/login
     *
     * 요청 Body (JSON):
     * { "username": "이메일", "password": "비밀번호" }
     *
     * 응답 (JSON):
     * - 성공:  { success: true,  locked: false, redirectUrl: "/main", ... }
     * - 실패:  { success: false, locked: false, remainingAttempts: N, ... }
     * - 잠금:  { success: false, locked: true,  remainingSeconds: N, ... }
     */
    @PostMapping("/teacher/login")
    public ResponseEntity<LoginResponse> teacherLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpSession session
    ) {
        String key = "teacher:" + request.getUsername();

        if (loginAttemptService.isLocked(key)) {
            long remaining = loginAttemptService.getRemainingSeconds(key);
            return ResponseEntity.status(423).body(LoginResponse.locked(remaining));
        }

        User teacher = loginService.authenticateTeacher(request.getUsername(), request.getPassword());

        if (teacher == null) {
            loginAttemptService.recordFailure(key);
            if (loginAttemptService.isLocked(key)) {
                long remaining = loginAttemptService.getRemainingSeconds(key);
                return ResponseEntity.status(423).body(LoginResponse.locked(remaining));
            }
            int remaining = loginAttemptService.getRemainingAttempts(key);
            return ResponseEntity.status(401).body(
                LoginResponse.failure("이메일 또는 비밀번호가 일치하지 않습니다. (남은 시도: " + remaining + "회)", remaining)
            );
        }

        loginAttemptService.recordSuccess(key);
        session.invalidate();
        session = httpRequest.getSession(true);
        session.setAttribute("loginUser", teacher.getEmail());
        session.setAttribute("userId", teacher.getId());

        return ResponseEntity.ok(LoginResponse.success(successUrl));
    }

    /**
     * POST /api/student/login
     */
    @PostMapping("/student/login")
    public ResponseEntity<LoginResponse> studentLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpSession session
    ) {
        String key = "student:" + request.getUsername();

        if (loginAttemptService.isLocked(key)) {
            long remaining = loginAttemptService.getRemainingSeconds(key);
            return ResponseEntity.status(423).body(LoginResponse.locked(remaining));
        }

        User student = loginService.authenticateStudent(request.getUsername(), request.getPassword());

        if (student == null) {
            loginAttemptService.recordFailure(key);
            if (loginAttemptService.isLocked(key)) {
                long remaining = loginAttemptService.getRemainingSeconds(key);
                return ResponseEntity.status(423).body(LoginResponse.locked(remaining));
            }
            int remaining = loginAttemptService.getRemainingAttempts(key);
            return ResponseEntity.status(401).body(
                LoginResponse.failure("이메일 또는 비밀번호가 일치하지 않습니다. (남은 시도: " + remaining + "회)", remaining)
            );
        }

        loginAttemptService.recordSuccess(key);
        session.invalidate();
        session = httpRequest.getSession(true);
        session.setAttribute("loginUser", student.getEmail());
        session.setAttribute("userId", student.getId());

        return ResponseEntity.ok(LoginResponse.success(successUrl));
    }

    /**
     * GET /api/login/status?username=xxx&userType=teacher|student
     * 페이지 로드 시 잠금 상태를 확인하는 용도
     */
    @GetMapping("/login/status")
    public ResponseEntity<LoginResponse> checkStatus(
            @RequestParam String username,
            @RequestParam String userType
    ) {
        String key = userType + ":" + username;
        if (loginAttemptService.isLocked(key)) {
            long remaining = loginAttemptService.getRemainingSeconds(key);
            return ResponseEntity.status(423).body(LoginResponse.locked(remaining));
        }
        return ResponseEntity.ok(LoginResponse.failure("정상 상태입니다.", loginAttemptService.getRemainingAttempts(key)));
    }

    /**
     * POST /api/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(LoginResponse.success("/login"));
    }
}
