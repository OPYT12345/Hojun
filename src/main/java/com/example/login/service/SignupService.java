package com.example.login.service;

import com.example.login.dto.SignupRequest;
import com.example.login.entity.User;
import com.example.login.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SignupService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public SignupService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void signupStudent(SignupRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        User u = new User();
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setName(req.getName());
        u.setRole(User.Role.STUDENT);
        u.setStudentNumber(req.getNumber());
        userRepository.save(u);
    }

    public void signupTeacher(SignupRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        User u = new User();
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setName(req.getName());
        u.setRole(User.Role.TEACHER);
        u.setTeacherNumber(req.getNumber());
        u.setDepartment(req.getDepartment());
        userRepository.save(u);
    }
}
