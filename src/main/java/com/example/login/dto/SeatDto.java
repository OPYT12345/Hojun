package com.example.login.dto;

public class SeatDto {
    private int seatNum;
    private Long studentId;
    private String studentName;
    private String studentNumber;
    private boolean present;
    private String attendedAt; // "HH:mm" 형식
    private String status;     // 학생 현재 상태 (null = 없음)

    public SeatDto() {}

    public SeatDto(int seatNum, Long studentId, String studentName,
                   String studentNumber, boolean present, String attendedAt) {
        this.seatNum = seatNum;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentNumber = studentNumber;
        this.present = present;
        this.attendedAt = attendedAt;
    }

    public int getSeatNum() { return seatNum; }
    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentNumber() { return studentNumber; }
    public boolean isPresent() { return present; }
    public String getAttendedAt() { return attendedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
