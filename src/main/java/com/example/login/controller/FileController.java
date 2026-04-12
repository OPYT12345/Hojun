package com.example.login.controller;

import com.example.login.entity.Material;
import com.example.login.entity.User;
import com.example.login.repository.MaterialRepository;
import com.example.login.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;

    public FileController(UserRepository userRepository, MaterialRepository materialRepository) {
        this.userRepository = userRepository;
        this.materialRepository = materialRepository;
    }

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * POST /api/files/upload
     * 파일 업로드 (강사만) — PDF, 영상 등
     * Returns: { success, url, filename }
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 파일을 업로드할 수 있습니다."));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "파일이 없습니다."));
        }

        // 허용 확장자 검사
        String original = file.getOriginalFilename();
        if (original == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "파일명이 없습니다."));
        }
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.')).toLowerCase()
                : "";
        if (!ext.matches("\\.(pdf|ppt|pptx|hwp|hwpx|mp4|mov|avi|mkv|webm)")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "PDF, PPT, 한글(HWP), 영상 파일만 업로드 가능합니다."));
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String saved = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), dir.resolve(saved));

            String url = "/api/files/" + saved;
            return ResponseEntity.ok(Map.of("success", true, "url", url, "filename", original));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "파일 저장 실패: " + e.getMessage()));
        }
    }

    /**
     * GET /api/files/{filename}
     * 파일 서빙 (로그인 사용자 모두)
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serve(
            @PathVariable String filename,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Path dir  = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path path = dir.resolve(filename).normalize();

            // 경로 순회(path traversal) 방지: 업로드 디렉터리 밖 접근 차단
            if (!path.startsWith(dir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            String lower = filename.toLowerCase();
            String contentType;
            String disposition;

            if (lower.endsWith(".pdf")) {
                contentType  = "application/pdf";
                disposition  = "inline";
            } else if (lower.endsWith(".ppt")) {
                contentType  = "application/vnd.ms-powerpoint";
                disposition  = "attachment";
            } else if (lower.endsWith(".pptx")) {
                contentType  = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                disposition  = "attachment";
            } else if (lower.endsWith(".hwp")) {
                contentType  = "application/x-hwp";
                disposition  = "attachment";
            } else if (lower.endsWith(".hwpx")) {
                contentType  = "application/hwp+zip";
                disposition  = "attachment";
            } else {
                contentType = Files.probeContentType(path);
                if (contentType == null) contentType = "application/octet-stream";
                disposition = "inline";
            }

            // DB에서 원본 파일명 조회 (없으면 UUID 파일명 그대로 사용)
            String downloadName = materialRepository.findByUrl("/api/files/" + filename)
                    .map(Material::getOriginalFilename)
                    .filter(n -> n != null && !n.isBlank())
                    .orElse(filename);
            // 헤더 인젝션 방지: CR/LF/쌍따옴표/백슬래시 제거
            String safeDownloadName = downloadName.replaceAll("[\\r\\n\"\\\\]", "_");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + safeDownloadName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
