package com.example.login.controller;

import com.example.login.dto.SignupRequest;
import com.example.login.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @PostMapping("/student/signup")
    public ResponseEntity<Map<String, Object>> studentSignup(@Valid @RequestBody SignupRequest req) {
        signupService.signupStudent(req);
        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입이 완료되었습니다."));
    }

    @PostMapping("/teacher/signup")
    public ResponseEntity<Map<String, Object>> teacherSignup(@Valid @RequestBody SignupRequest req) {
        signupService.signupTeacher(req);
        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입이 완료되었습니다."));
    }
}
