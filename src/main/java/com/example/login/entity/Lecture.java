package com.example.login.entity;

import com.example.login.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "lectures")
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    private String room;

    @Column(name = "class_schedule")
    private String schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "seat_rows")
    private int rows = 5;

    @Column(name = "seat_cols")
    private int cols = 6;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "lecture_start")
    private LocalDate lectureStart;

    @Column(name = "lecture_end")
    private LocalDate lectureEnd;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getRoom() { return room; }
    public String getSchedule() { return schedule; }
    public User getTeacher() { return teacher; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public boolean isActive() { return active; }
    public LocalDate getLectureStart() { return lectureStart; }
    public LocalDate getLectureEnd()   { return lectureEnd; }

    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setRoom(String room) { this.room = room; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public void setRows(int rows) { this.rows = rows; }
    public void setCols(int cols) { this.cols = cols; }
    public void setActive(boolean active) { this.active = active; }
    public void setLectureStart(LocalDate lectureStart) { this.lectureStart = lectureStart; }
    public void setLectureEnd(LocalDate lectureEnd)     { this.lectureEnd = lectureEnd; }
}
