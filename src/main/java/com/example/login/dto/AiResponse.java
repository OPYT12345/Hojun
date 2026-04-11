package com.example.login.dto;

public class AiResponse {
    private boolean success;
    private String message;
    private String error;

    public AiResponse(String message) {
        this.success = true;
        this.message = message;
    }

    public AiResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getError() { return error; }
}
