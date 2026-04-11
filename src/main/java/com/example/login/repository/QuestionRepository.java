package com.example.login.repository;

import com.example.login.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByLectureIdOrderByCreatedAtDesc(Long lectureId);
}
