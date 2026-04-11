package com.example.login.controller;

import com.example.login.dto.AnswerRequest;
import com.example.login.dto.QuestionRequest;
import com.example.login.entity.Question;
import com.example.login.entity.User;
import com.example.login.repository.QuestionRepository;
import com.example.login.repository.UserRepository;
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

    private final QuestionRepository questionRepository;
    private final UserRepository     userRepository;

    public QuestionController(QuestionRepository questionRepository, UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository     = userRepository;
    }

    // -------------------------------------------------------
    // GET /api/lecture/{lectureId}/questions
    // 강의 질문 목록 조회 (학생/강사 모두)
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long lectureId, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
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

        Long studentId     = (Long) session.getAttribute("userId");
        String realName    = (String) session.getAttribute("studentRealName");
        String loginUser   = (String) session.getAttribute("loginUser");
        String studentName = (realName != null && !realName.isBlank()) ? realName : loginUser;

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
        boolean isTeacher = userRepository.findById((Long) session.getAttribute("userId"))
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "강사만 답변할 수 있습니다."));
        }
        if (req.getAnswer() == null || req.getAnswer().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "답변 내용을 입력해주세요."));
        }

        Optional<Question> opt = questionRepository.findById(qId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Question q = opt.get();
        q.setAnswer(req.getAnswer());
        q.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(q);

        return ResponseEntity.ok(Map.of("success", true, "message", "답변이 등록되었습니다."));
    }
}
