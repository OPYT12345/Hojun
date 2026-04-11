package com.example.login.repository;

import com.example.login.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByLectureIdOrderByIdAsc(Long lectureId);
}
