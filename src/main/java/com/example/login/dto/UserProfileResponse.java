package com.example.login.dto;

public class UserProfileResponse {

    private final String username;
    private final String name;

    public UserProfileResponse(String username, String name) {
        this.username = username;
        this.name = name;
    }

    public String getUsername() { return username; }
    public String getName() { return name; }
}
