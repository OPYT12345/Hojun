package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_email")
    private String studentEmail;

    /** 자유 서술 텍스트 (선택) */
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    /** 제출 항목 JSON: [{type, url, name}] */
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 강사 채점 점수 (0~100, null = 미채점) */
    @Column(name = "grade")
    private Integer grade;

    /** 강사 정성 피드백 */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    /** 채점 완료 시각 */
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        updatedAt = submittedAt;
    }

    public String getSubmittedAtStr() {
        return submittedAt != null ? submittedAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : null;
    }

    public String getUpdatedAtStr() {
        return updatedAt != null ? updatedAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : null;
    }

    public Long getId()             { return id; }
    public Long getAssignmentId()   { return assignmentId; }
    public Long getLectureId()      { return lectureId; }
    public Long getStudentId()      { return studentId; }
    public String getStudentName()  { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getTextContent()  { return textContent; }
    public String getItemsJson()    { return itemsJson; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }
    public Integer getGrade()      { return grade; }
    public String getFeedback()    { return feedback; }
    public LocalDateTime getGradedAt() { return gradedAt; }

    public String getGradedAtStr() {
        return gradedAt != null ? gradedAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : null;
    }

    public void setAssignmentId(Long v)    { assignmentId = v; }
    public void setLectureId(Long v)       { lectureId = v; }
    public void setStudentId(Long v)       { studentId = v; }
    public void setStudentName(String v)   { studentName = v; }
    public void setStudentEmail(String v)  { studentEmail = v; }
    public void setTextContent(String v)   { textContent = v; }
    public void setItemsJson(String v)     { itemsJson = v; }
    public void setSubmittedAt(LocalDateTime v) { submittedAt = v; }
    public void setUpdatedAt(LocalDateTime v)   { updatedAt = v; }
    public void setGrade(Integer v)        { grade = v; }
    public void setFeedback(String v)      { feedback = v; }
    public void setGradedAt(LocalDateTime v) { gradedAt = v; }
}
