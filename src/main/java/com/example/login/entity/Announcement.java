package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "announcements", indexes = {
    @Index(name = "idx_ann_lecture", columnList = "lecture_id, created_at")
})
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public String getCreatedAtStr() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    public Long getId()              { return id; }
    public Long getLectureId()       { return lectureId; }
    public String getTitle()         { return title; }
    public String getContent()       { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setLectureId(Long v)  { lectureId = v; }
    public void setTitle(String v)    { title = v; }
    public void setContent(String v)  { content = v; }
}
