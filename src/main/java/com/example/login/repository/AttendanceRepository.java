package com.example.login.repository;

import com.example.login.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByLectureIdAndAttendDate(Long lectureId, LocalDate date);
    boolean existsByStudentIdAndLectureIdAndAttendDate(Long studentId, Long lectureId, LocalDate date);
    List<Attendance> findByStudentIdAndLectureId(Long studentId, Long lectureId);

    /** N+1 방지용 배치 조회 — student 즉시 로딩 */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student WHERE a.lecture.id IN :ids AND a.attendDate = :date")
    List<Attendance> findByLectureIdInAndAttendDateWithStudents(@Param("ids") List<Long> ids, @Param("date") LocalDate date);
}
