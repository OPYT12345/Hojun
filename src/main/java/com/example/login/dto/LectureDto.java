package com.example.login.dto;

public class LectureDto {
    private Long id;
    private String name;
    private String code;
    private String room;
    private String schedule;
    private String teacherName;
    private int rows;
    private int cols;
    private boolean active;
    private String lectureStart;
    private String lectureEnd;
    // 학생용: 오늘 출석 여부
    private Boolean attendedToday;
    // 강사용: 오늘 출석/전체 인원
    private Integer presentCount;
    private Integer totalCount;

    public LectureDto() {}

    public LectureDto(Long id, String name, String code, String room,
                      String schedule, String teacherName, int rows, int cols) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.room = room;
        this.schedule = schedule;
        this.teacherName = teacherName;
        this.rows = rows;
        this.cols = cols;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getRoom() { return room; }
    public String getSchedule() { return schedule; }
    public String getTeacherName() { return teacherName; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public boolean isActive() { return active; }
    public String getLectureStart() { return lectureStart; }
    public String getLectureEnd()   { return lectureEnd; }
    public Boolean getAttendedToday() { return attendedToday; }
    public Integer getPresentCount() { return presentCount; }
    public Integer getTotalCount() { return totalCount; }

    public void setActive(boolean active) { this.active = active; }
    public void setLectureStart(String lectureStart) { this.lectureStart = lectureStart; }
    public void setLectureEnd(String lectureEnd)     { this.lectureEnd = lectureEnd; }
    public void setAttendedToday(Boolean attendedToday) { this.attendedToday = attendedToday; }
    public void setPresentCount(Integer presentCount) { this.presentCount = presentCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
}
