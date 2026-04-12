package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "materials")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "type")
    private String type;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "description")
    private String description;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getCreatedAt() {
        return createdAt != null
            ? createdAt.format(DateTimeFormatter.ofPattern("HH:mm"))
            : null;
    }

    public Long getId() { return id; }
    public Long getLectureId() { return lectureId; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public String getOriginalFilename() { return originalFilename; }

    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }
    public void setTitle(String title) { this.title = title; }
    public void setType(String type) { this.type = type; }
    public void setUrl(String url) { this.url = url; }
    public void setDescription(String description) { this.description = description; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
}
