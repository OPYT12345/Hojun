package com.example.login.repository;

import com.example.login.entity.LectureEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LectureEnrollmentRepository extends JpaRepository<LectureEnrollment, Long> {
    List<LectureEnrollment> findByStudentId(Long studentId);
    List<LectureEnrollment> findByLectureId(Long lectureId);
    Optional<LectureEnrollment> findByStudentIdAndLectureId(Long studentId, Long lectureId);

    /** N+1 방지용 배치 조회 — student 즉시 로딩 */
    @Query("SELECT e FROM LectureEnrollment e JOIN FETCH e.student WHERE e.lecture.id IN :ids")
    List<LectureEnrollment> findByLectureIdInWithStudents(@Param("ids") List<Long> ids);

    /** 특정 강의 수강생 ID 목록만 조회 (공지 알림 등 경량 조회용) */
    @Query("SELECT e.student.id FROM LectureEnrollment e WHERE e.lecture.id = :lectureId")
    List<Long> findStudentIdsByLectureId(@Param("lectureId") Long lectureId);

    /** 수강 등록 여부 빠른 확인 (RLS 검사용) */
    boolean existsByStudentIdAndLectureId(Long studentId, Long lectureId);
}
