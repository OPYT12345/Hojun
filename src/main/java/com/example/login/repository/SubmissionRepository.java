package com.example.login.repository;

import com.example.login.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByLectureIdOrderBySubmittedAtDesc(Long lectureId);
    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    List<Submission> findByLectureIdAndStudentId(Long lectureId, Long studentId);
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
