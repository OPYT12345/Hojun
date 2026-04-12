package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user", columnList = "user_id, is_read, created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** GRADE | QA_ANSWER */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getCreatedAtStr() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    // getters / setters
    public Long getId()            { return id; }
    public Long getUserId()        { return userId; }
    public String getType()        { return type; }
    public String getTitle()       { return title; }
    public String getMessage()     { return message; }
    public boolean isRead()        { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUserId(Long v)    { userId = v; }
    public void setType(String v)    { type = v; }
    public void setTitle(String v)   { title = v; }
    public void setMessage(String v) { message = v; }
    public void setRead(boolean v)   { read = v; }
}
