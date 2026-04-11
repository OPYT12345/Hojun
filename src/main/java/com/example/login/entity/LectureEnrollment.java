package com.example.login.entity;

import com.example.login.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_enrollments",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "lecture_id"}),
        @UniqueConstraint(columnNames = {"lecture_id", "seat_num"})
    })
public class LectureEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "seat_num", nullable = false)
    private int seatNum;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Lecture getLecture() { return lecture; }
    public int getSeatNum() { return seatNum; }
    public String getStatus() { return status; }
    public LocalDateTime getStatusUpdatedAt() { return statusUpdatedAt; }

    public void setStudent(User student) { this.student = student; }
    public void setLecture(Lecture lecture) { this.lecture = lecture; }
    public void setSeatNum(int seatNum) { this.seatNum = seatNum; }
    public void setStatus(String status) { this.status = status; }
    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
}
