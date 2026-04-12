package com.example.login.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "push_subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = "endpoint"))
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 브라우저 푸시 엔드포인트 URL */
    @Column(name = "endpoint", columnDefinition = "TEXT", nullable = false)
    private String endpoint;

    /** 클라이언트 P-256 공개키 (base64url) */
    @Column(name = "p256dh", columnDefinition = "TEXT")
    private String p256dh;

    /** 인증 시크릿 (base64url, 16 bytes) */
    @Column(name = "auth", length = 255)
    private String auth;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId()        { return id; }
    public Long getUserId()    { return userId; }
    public String getEndpoint(){ return endpoint; }
    public String getP256dh()  { return p256dh; }
    public String getAuth()    { return auth; }

    public void setUserId(Long v)    { userId = v; }
    public void setEndpoint(String v){ endpoint = v; }
    public void setP256dh(String v)  { p256dh = v; }
    public void setAuth(String v)    { auth = v; }
}
