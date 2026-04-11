package com.example.login.controller;

import com.example.login.entity.Material;
import com.example.login.entity.User;
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

    private final MaterialRepository materialRepository;
    private final UserRepository     userRepository;

    public MaterialController(MaterialRepository materialRepository, UserRepository userRepository) {
        this.materialRepository = materialRepository;
        this.userRepository     = userRepository;
    }

    // -------------------------------------------------------
    // GET /api/lecture/{lectureId}/materials
    // 강의 자료 목록 (강사/학생 모두)
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long lectureId, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
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
        boolean isTeacher = userRepository.findById((Long) session.getAttribute("userId"))
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 자료를 등록할 수 있습니다."));
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
        boolean isTeacher = userRepository.findById((Long) session.getAttribute("userId"))
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 삭제할 수 있습니다."));
        }

        Optional<Material> opt = materialRepository.findById(matId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        materialRepository.delete(opt.get());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
