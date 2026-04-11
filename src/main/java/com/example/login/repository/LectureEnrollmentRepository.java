package com.example.login.repository;

import com.example.login.entity.LectureEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LectureEnrollmentRepository extends JpaRepository<LectureEnrollment, Long> {
    List<LectureEnrollment> findByStudentId(Long studentId);
    List<LectureEnrollment> findByLectureId(Long lectureId);
    Optional<LectureEnrollment> findByStudentIdAndLectureId(Long studentId, Long lectureId);
}
