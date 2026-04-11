package com.example.login.repository;

import com.example.login.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByLectureIdOrderByCreatedAtDesc(Long lectureId);
}
