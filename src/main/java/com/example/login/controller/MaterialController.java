package com.example.login.controller;

import com.example.login.entity.Material;
import com.example.login.entity.User;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.repository.LectureRepository;
import com.example.login.repository.MaterialRepository;
import com.example.login.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lecture/{lectureId}/materials")
public class MaterialController {

    private final MaterialRepository          materialRepository;
    private final UserRepository              userRepository;
    private final LectureRepository           lectureRepository;
    private final LectureEnrollmentRepository enrollmentRepository;

    public MaterialController(MaterialRepository materialRepository,
                              UserRepository userRepository,
                              LectureRepository lectureRepository,
                              LectureEnrollmentRepository enrollmentRepository) {
        this.materialRepository   = materialRepository;
        this.userRepository       = userRepository;
        this.lectureRepository    = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // -------------------------------------------------------
    // GET /api/lecture/{lectureId}/materials
    // 강의 자료 목록 (강사/학생 모두)
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        // RLS: 수강 등록된 학생 또는 담당 강사만 강의 자료 열람 가능
        if (!canAccessLecture(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 접근 권한이 없습니다."));
        }
        List<Material> materials = materialRepository.findByLectureIdOrderByIdAsc(lectureId);
        return ResponseEntity.ok(materials);
    }

    // -------------------------------------------------------
    // POST /api/lecture/{lectureId}/materials
    // 강의 자료 등록 (강사만)
    // Body: { title, type, url, description }
    // -------------------------------------------------------
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable Long lectureId,
            @RequestBody Map<String, String> body,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        Long userId = (Long) session.getAttribute("userId");
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 자료를 등록할 수 있습니다."));
        }
        if (!lectureRepository.existsByIdAndTeacherId(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }

        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "제목을 입력하세요."));
        }

        Material mat = new Material();
        mat.setLectureId(lectureId);
        mat.setTitle(title);
        mat.setType(body.getOrDefault("type", "etc"));
        mat.setUrl(body.get("url"));
        mat.setDescription(body.get("description"));
        mat.setOriginalFilename(body.get("filename"));
        materialRepository.save(mat);

        return ResponseEntity.ok(Map.of("success", true, "id", mat.getId()));
    }

    // -------------------------------------------------------
    // DELETE /api/lecture/{lectureId}/materials/{matId}
    // 강의 자료 삭제 (강사만)
    // -------------------------------------------------------
    @DeleteMapping("/{matId}")
    public ResponseEntity<?> delete(
            @PathVariable Long lectureId,
            @PathVariable Long matId,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        Long userId = (Long) session.getAttribute("userId");
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 삭제할 수 있습니다."));
        }
        if (!lectureRepository.existsByIdAndTeacherId(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }

        Optional<Material> opt = materialRepository.findById(matId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Material mat = opt.get();
        if (!lectureId.equals(mat.getLectureId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "자료가 해당 강의에 속하지 않습니다."));
        }
        materialRepository.delete(mat);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** RLS 검사: 수강 등록된 학생 또는 담당 강사 */
    private boolean canAccessLecture(Long lectureId, Long userId) {
        if (lectureRepository.existsByIdAndTeacherId(lectureId, userId)) return true;
        return enrollmentRepository.existsByStudentIdAndLectureId(userId, lectureId);
    }
}
