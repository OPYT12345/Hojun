package com.example.login.repository;

import com.example.login.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByLectureIdAndAttendDate(Long lectureId, LocalDate date);
    boolean existsByStudentIdAndLectureIdAndAttendDate(Long studentId, Long lectureId, LocalDate date);
    List<Attendance> findByStudentIdAndLectureId(Long studentId, Long lectureId);
}
