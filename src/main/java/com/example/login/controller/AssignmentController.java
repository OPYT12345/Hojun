package com.example.login.controller;

import com.example.login.entity.Assignment;
import com.example.login.entity.Submission;
import com.example.login.entity.User;
import com.example.login.repository.AssignmentRepository;
import com.example.login.repository.SubmissionRepository;
import com.example.login.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/lecture/{lectureId}")
public class AssignmentController {

    private final AssignmentRepository assignmentRepo;
    private final SubmissionRepository submissionRepo;
    private final UserRepository       userRepo;
    private final ObjectMapper         objectMapper;

    public AssignmentController(AssignmentRepository assignmentRepo,
                                SubmissionRepository submissionRepo,
                                UserRepository userRepo,
                                ObjectMapper objectMapper) {
        this.assignmentRepo = assignmentRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo       = userRepo;
        this.objectMapper   = objectMapper;
    }

    // ── GET /api/lecture/{lectureId}/assignments ──────────────
    @GetMapping("/assignments")
    public ResponseEntity<?> listAssignments(
            @PathVariable Long lectureId, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        List<Assignment> list = assignmentRepo.findByLectureIdOrderByCreatedAtDesc(lectureId);
        return ResponseEntity.ok(list.stream().map(this::toMap).toList());
    }

    // ── POST /api/lecture/{lectureId}/assignments ─────────────
    @PostMapping("/assignments")
    public ResponseEntity<?> createAssignment(
            @PathVariable Long lectureId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 과제를 등록할 수 있습니다."));
        }
        String title = (String) body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "제목을 입력하세요."));
        }

        Assignment a = new Assignment();
        a.setLectureId(lectureId);
        a.setTitle(title);
        a.setContent((String) body.getOrDefault("content", ""));
        a.setAttachmentsJson(toJson(body.get("attachments")));
        a.setDueStart(parseDateTime(body.get("dueStart")));
        a.setDueEnd(parseDateTime(body.get("dueEnd")));
        assignmentRepo.save(a);

        return ResponseEntity.ok(Map.of("success", true, "id", a.getId()));
    }

    // ── PUT /api/lecture/{lectureId}/assignments/{asgId} ──────
    @PutMapping("/assignments/{asgId}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable Long lectureId,
            @PathVariable Long asgId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 과제를 수정할 수 있습니다."));
        }
        Optional<Assignment> opt = assignmentRepo.findById(asgId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Assignment a = opt.get();
        if (body.containsKey("title"))       a.setTitle((String) body.get("title"));
        if (body.containsKey("content"))     a.setContent((String) body.get("content"));
        if (body.containsKey("attachments")) a.setAttachmentsJson(toJson(body.get("attachments")));
        if (body.containsKey("dueStart"))    a.setDueStart(parseDateTime(body.get("dueStart")));
        if (body.containsKey("dueEnd"))      a.setDueEnd(parseDateTime(body.get("dueEnd")));
        assignmentRepo.save(a);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── DELETE /api/lecture/{lectureId}/assignments/{asgId} ───
    @DeleteMapping("/assignments/{asgId}")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable Long lectureId,
            @PathVariable Long asgId,
            HttpSession session) {

        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 삭제할 수 있습니다."));
        }
        Optional<Assignment> opt = assignmentRepo.findById(asgId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        submissionRepo.findByAssignmentIdOrderBySubmittedAtDesc(asgId)
                      .forEach(submissionRepo::delete);
        assignmentRepo.delete(opt.get());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── POST /api/lecture/{lectureId}/assignments/{asgId}/submit ─
    @PostMapping("/assignments/{asgId}/submit")
    public ResponseEntity<?> submit(
            @PathVariable Long lectureId,
            @PathVariable Long asgId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        // DB에서 직접 학생 여부 확인
        User student = userRepo.findById(userId)
                .filter(u -> u.getRole() == User.Role.STUDENT)
                .orElse(null);
        if (student == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "학생 계정으로 로그인해주세요."));
        }

        Optional<Assignment> asgOpt = assignmentRepo.findById(asgId);
        if (asgOpt.isEmpty()) return ResponseEntity.notFound().build();
        Assignment asg = asgOpt.get();

        LocalDateTime now = LocalDateTime.now();
        if (asg.getDueStart() != null && now.isBefore(asg.getDueStart())) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "아직 제출 기간이 시작되지 않았습니다."));
        }
        if (asg.getDueEnd() != null && now.isAfter(asg.getDueEnd())) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "제출 기간이 마감되었습니다."));
        }

        String textContent = (String) body.getOrDefault("textContent", "");
        String itemsJson   = toJson(body.get("items"));

        Optional<Submission> existing = submissionRepo.findByAssignmentIdAndStudentId(asgId, userId);
        Submission sub;
        boolean isUpdate = existing.isPresent();
        if (isUpdate) {
            sub = existing.get();
            sub.setTextContent(textContent);
            sub.setItemsJson(itemsJson);
        } else {
            sub = new Submission();
            sub.setAssignmentId(asgId);
            sub.setLectureId(lectureId);
            sub.setStudentId(userId);
            sub.setStudentName(student.getName());
            sub.setStudentEmail(student.getEmail());
            sub.setTextContent(textContent);
            sub.setItemsJson(itemsJson);
        }
        submissionRepo.save(sub);

        return ResponseEntity.ok(Map.of("success", true, "updated", isUpdate));
    }

    // ── GET /api/lecture/{lectureId}/submissions ───────────────
    @GetMapping("/submissions")
    public ResponseEntity<?> submissions(
            @PathVariable Long lectureId,
            @RequestParam(required = false) Long assignmentId,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        List<Submission> list = assignmentId != null
                ? submissionRepo.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)
                : submissionRepo.findByLectureIdOrderBySubmittedAtDesc(lectureId);
        return ResponseEntity.ok(list.stream().map(this::subToMap).toList());
    }

    // ── GET /api/lecture/{lectureId}/my-submissions ────────────
    @GetMapping("/my-submissions")
    public ResponseEntity<?> mySubmissions(
            @PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        List<Submission> list = submissionRepo.findByLectureIdAndStudentId(lectureId, userId);
        return ResponseEntity.ok(list.stream().map(this::subToMap).toList());
    }

    // ── helpers ───────────────────────────────────────────────

    private boolean isTeacher(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return false;
        return userRepo.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER)
                .orElse(false);
    }

    private Map<String, Object> toMap(Assignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          a.getId());
        m.put("lectureId",   a.getLectureId());
        m.put("title",       a.getTitle());
        m.put("content",     a.getContent());
        m.put("dueStart",    fmtDt(a.getDueStart()));
        m.put("dueEnd",      fmtDt(a.getDueEnd()));
        m.put("createdAt",   a.getCreatedAtStr());
        try {
            List<?> attachments = objectMapper.readValue(
                    a.getAttachmentsJson() != null ? a.getAttachmentsJson() : "[]",
                    new TypeReference<List<?>>() {});
            m.put("attachments", attachments);
        } catch (Exception e) {
            m.put("attachments", List.of());
        }
        return m;
    }

    private Map<String, Object> subToMap(Submission s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           s.getId());
        m.put("assignmentId", s.getAssignmentId());
        m.put("lectureId",    s.getLectureId());
        m.put("studentId",    s.getStudentId());
        m.put("studentName",  s.getStudentName());
        m.put("studentEmail", s.getStudentEmail());
        m.put("textContent",  s.getTextContent());
        m.put("submittedAt",  s.getSubmittedAtStr());
        m.put("updatedAt",    s.getUpdatedAtStr());
        try {
            List<?> items = objectMapper.readValue(
                    s.getItemsJson() != null ? s.getItemsJson() : "[]",
                    new TypeReference<List<?>>() {});
            m.put("items", items);
        } catch (Exception e) {
            m.put("items", List.of());
        }
        return m;
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private LocalDateTime parseDateTime(Object obj) {
        if (obj == null) return null;
        String s = String.valueOf(obj).trim();
        if (s.isBlank() || "null".equals(s)) return null;
        try { return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
        catch (Exception e) { return null; }
    }

    private String fmtDt(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}
