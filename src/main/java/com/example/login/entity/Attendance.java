package com.example.login.entity;

import com.example.login.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "lecture_id", "attend_date"})
    })
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "attended_at", nullable = false)
    private LocalDateTime attendedAt;

    @Column(name = "attend_date", nullable = false)
    private LocalDate attendDate;

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Lecture getLecture() { return lecture; }
    public LocalDateTime getAttendedAt() { return attendedAt; }
    public LocalDate getAttendDate() { return attendDate; }

    public void setStudent(User student) { this.student = student; }
    public void setLecture(Lecture lecture) { this.lecture = lecture; }
    public void setAttendedAt(LocalDateTime attendedAt) { this.attendedAt = attendedAt; }
    public void setAttendDate(LocalDate attendDate) { this.attendDate = attendDate; }
}
