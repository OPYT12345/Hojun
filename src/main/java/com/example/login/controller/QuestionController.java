package com.example.login.controller;

import com.example.login.dto.AnswerRequest;
import com.example.login.dto.QuestionRequest;
import com.example.login.entity.Question;
import com.example.login.entity.User;
import com.example.login.repository.LectureEnrollmentRepository;
import com.example.login.repository.LectureRepository;
import com.example.login.repository.QuestionRepository;
import com.example.login.repository.UserRepository;
import com.example.login.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lecture/{lectureId}/questions")
public class QuestionController {

    private final QuestionRepository          questionRepository;
    private final UserRepository              userRepository;
    private final LectureEnrollmentRepository enrollmentRepository;
    private final LectureRepository           lectureRepository;
    private final NotificationService         notificationService;

    public QuestionController(QuestionRepository questionRepository,
                              UserRepository userRepository,
                              LectureEnrollmentRepository enrollmentRepository,
                              LectureRepository lectureRepository,
                              NotificationService notificationService) {
        this.questionRepository   = questionRepository;
        this.userRepository       = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRepository    = lectureRepository;
        this.notificationService  = notificationService;
    }

    // -------------------------------------------------------
    // GET /api/lecture/{lectureId}/questions
    // 강의 질문 목록 조회 (수강 등록된 학생 또는 담당 강사만)
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long lectureId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        // RLS: 수강 등록된 학생 또는 담당 강사만 질문 목록 열람 가능
        if (!canAccessLecture(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 접근 권한이 없습니다."));
        }
        List<Question> questions = questionRepository.findByLectureIdOrderByCreatedAtDesc(lectureId);
        return ResponseEntity.ok(questions);
    }

    // -------------------------------------------------------
    // POST /api/lecture/{lectureId}/questions
    // 학생 질문 등록
    // -------------------------------------------------------
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable Long lectureId,
            @RequestBody QuestionRequest req,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "질문 내용을 입력해주세요."));
        }

        Long studentId = (Long) session.getAttribute("userId");

        // 강사 계정으로는 질문 불가 (강사는 답변하는 역할)
        User requestUser = userRepository.findById(studentId).orElse(null);
        if (requestUser == null || requestUser.getRole() == User.Role.TEACHER) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "학생 계정으로만 질문을 등록할 수 있습니다."));
        }

        // 수강 등록 여부 확인 (수강하지 않은 강의에는 질문 불가)
        if (enrollmentRepository.findByStudentIdAndLectureId(studentId, lectureId).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "수강 등록된 강의가 아닙니다."));
        }

        // 이름은 이미 위에서 조회한 requestUser에서 가져옴
        String studentName = requestUser.getName();

        Question q = new Question();
        q.setLectureId(lectureId);
        q.setStudentId(studentId);
        q.setStudentName(studentName);
        q.setContent(req.getContent());
        q.setAssignmentId(req.getAssignmentId());
        q.setAssignmentTitle(req.getAssignmentTitle());
        questionRepository.save(q);

        return ResponseEntity.ok(Map.of("success", true, "message", "질문이 등록되었습니다."));
    }

    // -------------------------------------------------------
    // POST /api/lecture/{lectureId}/questions/{qId}/answer
    // 강사 답변 등록
    // -------------------------------------------------------
    @PostMapping("/{qId}/answer")
    public ResponseEntity<?> answer(
            @PathVariable Long lectureId,
            @PathVariable Long qId,
            @RequestBody AnswerRequest req,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        Long userId = (Long) session.getAttribute("userId");
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 답변할 수 있습니다."));
        }
        if (!lectureRepository.existsByIdAndTeacherId(lectureId, userId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "해당 강의에 대한 권한이 없습니다."));
        }
        if (req.getAnswer() == null || req.getAnswer().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "답변 내용을 입력해주세요."));
        }

        Optional<Question> opt = questionRepository.findById(qId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Question q = opt.get();
        // 해당 질문이 이 강의의 질문인지 확인
        if (!lectureId.equals(q.getLectureId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "잘못된 접근입니다."));
        }
        q.setAnswer(req.getAnswer());
        q.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(q);

        // 질문한 학생에게 알림
        String lectureName = lectureRepository.findById(lectureId)
                .map(l -> l.getName()).orElse("강의");
        notificationService.createQaAnswerNotification(q.getStudentId(), lectureName, q.getContent(), req.getAnswer());

        return ResponseEntity.ok(Map.of("success", true, "message", "답변이 등록되었습니다."));
    }

    /** RLS 검사: 수강 등록된 학생 또는 담당 강사 */
    private boolean canAccessLecture(Long lectureId, Long userId) {
        if (lectureRepository.existsByIdAndTeacherId(lectureId, userId)) return true;
        return enrollmentRepository.existsByStudentIdAndLectureId(userId, lectureId);
    }
}
