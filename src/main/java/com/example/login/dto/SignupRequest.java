package com.example.login.dto;

public class SignupRequest {
    private String name;
    private String number;
    private String email;
    private String password;
    private String department;

    public String getName() { return name; }
    public String getNumber() { return number; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getDepartment() { return department; }
}
