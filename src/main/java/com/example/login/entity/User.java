package com.example.login.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    public enum Role { STUDENT, TEACHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(name = "student_number")
    private String studentNumber;

    @Column(name = "teacher_number")
    private String teacherNumber;

    @Column
    private String department;

    public Long getId()              { return id; }
    public String getEmail()         { return email; }
    public String getUsername()      { return email; }   // 기존 호환
    public String getPassword()      { return password; }
    public String getName()          { return name; }
    public Role   getRole()          { return role; }
    public String getStudentNumber() { return studentNumber; }
    public String getTeacherNumber() { return teacherNumber; }
    public String getDepartment()    { return department; }

    public void setEmail(String v)          { this.email = v; }
    public void setUsername(String v)       { this.email = v; }  // 기존 호환
    public void setPassword(String v)       { this.password = v; }
    public void setName(String v)           { this.name = v; }
    public void setRole(Role v)             { this.role = v; }
    public void setStudentNumber(String v)  { this.studentNumber = v; }
    public void setTeacherNumber(String v)  { this.teacherNumber = v; }
    public void setDepartment(String v)     { this.department = v; }
}
