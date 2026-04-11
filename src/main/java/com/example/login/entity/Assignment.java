package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "title", nullable = false)
    private String title;

    /** 과제 내용 (자유 서술) */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 강사 첨부자료 JSON: [{type, url, name}] */
    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "due_start")
    private LocalDateTime dueStart;

    @Column(name = "due_end")
    private LocalDateTime dueEnd;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getCreatedAtStr() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : null;
    }

    public Long getId()              { return id; }
    public Long getLectureId()       { return lectureId; }
    public String getTitle()         { return title; }
    public String getContent()       { return content; }
    public String getAttachmentsJson() { return attachmentsJson; }
    public LocalDateTime getDueStart() { return dueStart; }
    public LocalDateTime getDueEnd()   { return dueEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setLectureId(Long v)       { lectureId = v; }
    public void setTitle(String v)         { title = v; }
    public void setContent(String v)       { content = v; }
    public void setAttachmentsJson(String v) { attachmentsJson = v; }
    public void setDueStart(LocalDateTime v) { dueStart = v; }
    public void setDueEnd(LocalDateTime v)   { dueEnd = v; }
}
