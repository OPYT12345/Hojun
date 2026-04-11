package com.example.login.dto;

public class AttendanceResponse {
    private boolean success;
    private String message;
    private boolean alreadyCheckedIn;
    private SeatDto seat;

    public AttendanceResponse() {}

    public AttendanceResponse(boolean success, String message,
                              boolean alreadyCheckedIn, SeatDto seat) {
        this.success = success;
        this.message = message;
        this.alreadyCheckedIn = alreadyCheckedIn;
        this.seat = seat;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public boolean isAlreadyCheckedIn() { return alreadyCheckedIn; }
    public SeatDto getSeat() { return seat; }
}
