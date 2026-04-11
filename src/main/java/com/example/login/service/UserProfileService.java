package com.example.login.service;

import com.example.login.dto.UserProfileResponse;
import com.example.login.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(u -> new UserProfileResponse(u.getEmail(), u.getName()))
                .orElse(null);
    }
}
