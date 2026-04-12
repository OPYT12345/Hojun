package com.example.login.controller;

import com.example.login.entity.Assignment;
import com.example.login.entity.Submission;
import com.example.login.entity.User;
import com.example.login.repository.AssignmentRepository;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.repository.LectureRepository;
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

    private final AssignmentRepository       assignmentRepo;
    private final SubmissionRepository       submissionRepo;
    private final UserRepository             userRepo;
    private final LectureRepository          lectureRepo;
    private final LectureEnrollmentRepository enrollmentRepo;
    private final ObjectMapper               objectMapper;
    private final com.example.login.service.PushNotificationService pushService;
    private final com.example.login.service.NotificationService    notificationService;

    public AssignmentController(AssignmentRepository assignmentRepo,
                                SubmissionRepository submissionRepo,
                                UserRepository userRepo,
                                LectureRepository lectureRepo,
                                LectureEnrollmentRepository enrollmentRepo,
                                ObjectMapper objectMapper,
                                com.example.login.service.PushNotificationService pushService,
                                com.example.login.service.NotificationService notificationService) {
        this.assignmentRepo      = assignmentRepo;
        this.submissionRepo      = submissionRepo;
        this.userRepo            = userRepo;
        this.lectureRepo         = lectureRepo;
        this.enrollmentRepo      = enrollmentRepo;
        this.objectMapper        = objectMapper;
        this.pushService         = pushService;
        this.notificationService = notificationService;
    }

    // ── GET /api/lecture/{lectureId}/assignments ──────────────
    @GetMapping("/assignments")
    public ResponseEntity<?> listAssignments(
            @PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        // RLS: 수강 등록된 학생 또는 해당 강의 담당 강사만 조회 가능
        if (!canAccessLecture(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 접근 권한이 없습니다."));
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
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
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
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }
        Optional<Assignment> opt = assignmentRepo.findById(asgId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Assignment a = opt.get();
        if (!lectureId.equals(a.getLectureId())) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "강의와 과제가 일치하지 않습니다."));
        }
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
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }
        Optional<Assignment> opt = assignmentRepo.findById(asgId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Assignment a = opt.get();
        if (!lectureId.equals(a.getLectureId())) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "강의와 과제가 일치하지 않습니다."));
        }
        submissionRepo.findByAssignmentIdOrderBySubmittedAtDesc(asgId)
                      .forEach(submissionRepo::delete);
        assignmentRepo.delete(a);
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
        if (!lectureId.equals(asg.getLectureId())) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "강의와 과제가 일치하지 않습니다."));
        }

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
        sub.setUpdatedAt(LocalDateTime.now());
        submissionRepo.save(sub);

        return ResponseEntity.ok(Map.of("success", true, "updated", isUpdate));
    }

    // ── PUT /api/lecture/{lectureId}/assignments/{asgId}/submissions/{subId}/grade ─
    @PutMapping("/assignments/{asgId}/submissions/{subId}/grade")
    public ResponseEntity<?> grade(
            @PathVariable Long lectureId,
            @PathVariable Long asgId,
            @PathVariable Long subId,
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "강사만 채점할 수 있습니다."));
        }
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }

        Optional<Submission> opt = submissionRepo.findById(subId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Submission sub = opt.get();

        if (!lectureId.equals(sub.getLectureId()) || !asgId.equals(sub.getAssignmentId())) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "잘못된 접근입니다."));
        }

        if (body.containsKey("grade")) {
            Object gradeObj = body.get("grade");
            if (gradeObj == null) {
                sub.setGrade(null);
            } else {
                try {
                    int g = ((Number) gradeObj).intValue();
                    if (g < 0 || g > 100) {
                        return ResponseEntity.badRequest().body(
                                Map.of("success", false, "message", "점수는 0~100 사이여야 합니다."));
                    }
                    sub.setGrade(g);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                            Map.of("success", false, "message", "점수 형식이 올바르지 않습니다."));
                }
            }
        }
        if (body.containsKey("feedback")) {
            sub.setFeedback((String) body.get("feedback"));
        }
        sub.setGradedAt(LocalDateTime.now());
        submissionRepo.save(sub);

        // 학생 알림 (채점 완료)
        String asgTitle = assignmentRepo.findById(asgId)
                .map(com.example.login.entity.Assignment::getTitle).orElse("과제");
        String lectureName = lectureRepo.findById(lectureId)
                .map(com.example.login.entity.Lecture::getName).orElse("강의");
        notificationService.createGradeNotification(sub.getStudentId(), lectureName, asgTitle, sub.getGrade(), sub.getFeedback());

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── GET /api/lecture/{lectureId}/submissions ─────────────── (강사 전용)
    @GetMapping("/submissions")
    public ResponseEntity<?> submissions(
            @PathVariable Long lectureId,
            @RequestParam(required = false) Long assignmentId,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        if (!isTeacher(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 제출 현황을 조회할 수 있습니다."));
        }
        if (!isLectureOwner(lectureId, session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }
        if (assignmentId != null) {
            Optional<Assignment> ao = assignmentRepo.findById(assignmentId);
            if (ao.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!lectureId.equals(ao.get().getLectureId())) {
                return ResponseEntity.status(403).body(
                        Map.of("success", false, "message", "과제가 해당 강의에 속하지 않습니다."));
            }
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
        // RLS: 수강 등록된 학생 또는 해당 강의 담당 강사만 조회 가능
        if (!canAccessLecture(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 접근 권한이 없습니다."));
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

    private boolean isLectureOwner(Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return false;
        return lectureRepo.existsByIdAndTeacherId(lectureId, userId);
    }

    /**
     * RLS 검사: 해당 강의에 수강 등록된 학생이거나 담당 강사인지 확인.
     * 수강하지 않은 강의의 데이터(과제, 제출물 등)는 볼 수 없어야 합니다.
     */
    private boolean canAccessLecture(Long lectureId, Long userId) {
        if (lectureRepo.existsByIdAndTeacherId(lectureId, userId)) return true;
        return enrollmentRepo.existsByStudentIdAndLectureId(userId, lectureId);
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
        m.put("grade",        s.getGrade());
        m.put("feedback",     s.getFeedback());
        m.put("gradedAt",     s.getGradedAtStr());
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
