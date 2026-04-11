package com.example.login.dto;

public class InstructorSuggestRequest {
    private Long lectureId;
    private String lectureName;
    private String question;

    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }

    public String getLectureName() { return lectureName; }
    public void setLectureName(String lectureName) { this.lectureName = lectureName; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
