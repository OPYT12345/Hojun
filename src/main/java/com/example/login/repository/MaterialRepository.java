package com.example.login.repository;

import com.example.login.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByLectureIdOrderByIdAsc(Long lectureId);
    Optional<Material> findByUrl(String url);
}
