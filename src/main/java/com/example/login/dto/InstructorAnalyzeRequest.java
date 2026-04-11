package com.example.login.dto;

public class InstructorAnalyzeRequest {
    private Long lectureId;
    private String lectureName;
    private String analysisType; // "submissions" | "questions" | "overall"

    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }

    public String getLectureName() { return lectureName; }
    public void setLectureName(String lectureName) { this.lectureName = lectureName; }

    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
}
