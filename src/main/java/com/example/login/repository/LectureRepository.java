package com.example.login.repository;

import com.example.login.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findByTeacherId(Long teacherId);
    boolean existsByIdAndTeacherId(Long id, Long teacherId);
}
