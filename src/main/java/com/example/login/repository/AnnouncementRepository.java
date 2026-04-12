package com.example.login.repository;

import com.example.login.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByLectureIdOrderByCreatedAtDesc(Long lectureId);
}
