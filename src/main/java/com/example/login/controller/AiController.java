package com.example.login.controller;

import com.example.login.dto.*;
import com.example.login.entity.User;
import com.example.login.repository.UserRepository;
import com.example.login.service.AiBudgetService;
import com.example.login.service.AiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService       aiService;
    private final UserRepository  userRepository;
    private final AiBudgetService aiBudgetService;

    public AiController(AiService aiService, UserRepository userRepository,
                        AiBudgetService aiBudgetService) {
        this.aiService       = aiService;
        this.userRepository  = userRepository;
        this.aiBudgetService = aiBudgetService;
    }

    // -------------------------------------------------------
    // POST /api/ai/student-chat
    // 학생용 AI 튜터 채팅 (다중 턴)
    // -------------------------------------------------------
    @PostMapping("/student-chat")
    public ResponseEntity<AiResponse> studentChat(
            @RequestBody StudentChatRequest req,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new AiResponse(false, "로그인이 필요합니다."));
        }

        List<StudentChatRequest.Message> messages = req.getMessages();
        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.badRequest().body(new AiResponse(false, "메시지가 없습니다."));
        }

        // 예산 한도 검사: 사용자별 시간당 + 전역 일별 한도
        if (!aiBudgetService.tryConsume(userId)) {
            int remaining = aiBudgetService.getRemainingCalls(userId);
            return ResponseEntity.status(429).body(new AiResponse(false,
                    "AI 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요. (남은 횟수: " + remaining + ")"));
        }

        String systemPrompt = buildStudentSystemPrompt(req.getLectureName(), req.getContext());

        try {
            String reply = aiService.callOpenAIWithHistory(systemPrompt, messages);
            return ResponseEntity.ok(new AiResponse(reply));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(new AiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("[AiController/student-chat] " + e.getMessage());
            return ResponseEntity.status(500).body(new AiResponse(false, "AI 응답 중 오류가 발생했습니다."));
        }
    }

    // -------------------------------------------------------
    // POST /api/ai/counsel
    // 학생 전용 비밀 상담 (강사에게 절대 전달 안 됨)
    // -------------------------------------------------------
    @PostMapping("/counsel")
    public ResponseEntity<AiResponse> counsel(
            @RequestBody StudentChatRequest req,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new AiResponse(false, "로그인이 필요합니다."));
        }

        List<StudentChatRequest.Message> messages = req.getMessages();
        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.badRequest().body(new AiResponse(false, "메시지가 없습니다."));
        }

        // 예산 한도 검사
        if (!aiBudgetService.tryConsume(userId)) {
            return ResponseEntity.status(429).body(new AiResponse(false, "AI 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요."));
        }

        try {
            String reply = aiService.callOpenAIWithHistory(buildCounselSystemPrompt(), messages);
            return ResponseEntity.ok(new AiResponse(reply));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(new AiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("[AiController/counsel] " + e.getMessage());
            return ResponseEntity.status(500).body(new AiResponse(false, "AI 응답 중 오류가 발생했습니다."));
        }
    }

    // -------------------------------------------------------
    // POST /api/ai/instructor-analyze
    // 강사용 AI 분석 (제출물 / 질문 / 전체 요약)
    // -------------------------------------------------------
    @PostMapping("/instructor-analyze")
    public ResponseEntity<AiResponse> instructorAnalyze(
            @RequestBody InstructorAnalyzeRequest req,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new AiResponse(false, "로그인이 필요합니다."));
        }
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(new AiResponse(false, "강사만 사용할 수 있습니다."));
        }
        // 예산 한도 검사
        if (!aiBudgetService.tryConsume(userId)) {
            return ResponseEntity.status(429).body(new AiResponse(false, "AI 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요."));
        }

        String systemPrompt = buildInstructorSystemPrompt(req.getLectureName());
        String userMessage  = buildAnalysisPrompt(req.getAnalysisType(), req.getLectureName());

        try {
            String reply = aiService.callOpenAI(systemPrompt, userMessage);
            return ResponseEntity.ok(new AiResponse(reply));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(new AiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("[AiController/instructor-analyze] " + e.getMessage());
            return ResponseEntity.status(500).body(new AiResponse(false, "AI 분석 중 오류가 발생했습니다."));
        }
    }

    // -------------------------------------------------------
    // POST /api/ai/instructor-suggest
    // 강사용 AI 답변 제안 (학생 질문에 대한 모범 답안)
    // -------------------------------------------------------
    @PostMapping("/instructor-suggest")
    public ResponseEntity<AiResponse> instructorSuggest(
            @RequestBody InstructorSuggestRequest req,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(new AiResponse(false, "로그인이 필요합니다."));
        }
        boolean isTeacher = userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.TEACHER).orElse(false);
        if (!isTeacher) {
            return ResponseEntity.status(403).body(new AiResponse(false, "강사만 사용할 수 있습니다."));
        }

        if (req.getQuestion() == null || req.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body(new AiResponse(false, "질문 내용이 없습니다."));
        }

        // 예산 한도 검사
        if (!aiBudgetService.tryConsume(userId)) {
            return ResponseEntity.status(429).body(new AiResponse(false, "AI 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요."));
        }

        String systemPrompt = buildInstructorSystemPrompt(req.getLectureName());
        String userMessage  = "학생 질문: " + req.getQuestion()
                + "\n\n위 질문에 대해 강사가 학생에게 줄 수 있는 명확하고 친절한 답변을 제안해주세요. "
                + "단계별로 설명하고, 관련 핵심 개념도 함께 언급해주세요.";

        try {
            String reply = aiService.callOpenAI(systemPrompt, userMessage);
            return ResponseEntity.ok(new AiResponse(reply));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(new AiResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("[AiController/instructor-suggest] " + e.getMessage());
            return ResponseEntity.status(500).body(new AiResponse(false, "AI 제안 중 오류가 발생했습니다."));
        }
    }

    // -------------------------------------------------------
    // 시스템 프롬프트 빌더
    // -------------------------------------------------------

    private String buildCounselSystemPrompt() {
        return """
                당신은 학생들의 고민을 들어주는 따뜻하고 공감하는 AI 상담사입니다.

                역할:
                - 학생의 이야기를 판단 없이 경청하고 공감합니다.
                - 진로, 인간관계, 학업 스트레스, 일상적 고민 등 모든 이야기를 들어줍니다.
                - 이 대화는 강사나 어느 누구에게도 절대 공유되지 않습니다. 완전한 비밀입니다.
                - 조언보다는 먼저 공감하고, 학생이 스스로 생각할 수 있도록 도와줍니다.
                - 필요하다면 전문 상담사 연결을 권유할 수 있습니다.
                - 한국어로 대화하며, 친근하고 따뜻한 말투를 사용합니다.
                - 응답은 진심으로 대화하듯 작성합니다 (400자 이내 권장).
                """;
    }

    private String buildStudentSystemPrompt(String lectureName, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 대학교 수업을 돕는 친절한 AI 튜터입니다.\n");
        String safeName = sanitizePromptInput(lectureName, 100);
        String safeCtx  = sanitizePromptInput(context, 500);
        if (!safeName.isBlank()) {
            sb.append("현재 강의: ").append(safeName).append("\n");
        }
        if (!safeCtx.isBlank()) {
            sb.append("수업 맥락: ").append(safeCtx).append("\n");
        }
        sb.append("""
                역할:
                - 학생이 이해하기 쉽게 개념을 설명합니다.
                - 단계별로 풀이 방법을 안내합니다.
                - 직접 답을 알려주기보다 스스로 생각할 수 있도록 유도합니다.
                - 한국어로 대화합니다.
                - 응답은 간결하고 명확하게 작성합니다 (300자 이내 권장).
                """);
        return sb.toString();
    }

    private String buildInstructorSystemPrompt(String lectureName) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 대학교 강사를 돕는 AI 교육 분석가입니다.\n");
        String safeName = sanitizePromptInput(lectureName, 100);
        if (!safeName.isBlank()) {
            sb.append("분석 대상 강의: ").append(safeName).append("\n");
        }
        sb.append("""
                역할:
                - 학생들의 학습 현황을 분석하고 인사이트를 제공합니다.
                - 강의 개선을 위한 실질적인 제안을 합니다.
                - 어려움을 겪는 부분을 파악하고 대응 방안을 제시합니다.
                - 한국어로 작성합니다.
                - 분석 결과는 명확한 항목으로 구분하여 제시합니다.
                """);
        return sb.toString();
    }

    /**
     * 프롬프트 인젝션 방어 — 사용자 입력을 시스템 프롬프트에 삽입하기 전에 정제.
     *
     * 제거 대상:
     *   - 개행/탭 문자 : 프롬프트 구조를 깨뜨려 역할 전환(role override) 시도에 사용
     *   - 길이 초과분 : 과도하게 긴 입력으로 시스템 프롬프트를 희석하는 공격 차단
     */
    private String sanitizePromptInput(String input, int maxLen) {
        if (input == null) return "";
        String s = input.replaceAll("[\r\n\t]", " ").trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private String buildAnalysisPrompt(String analysisType, String lectureName) {
        String lecture = (lectureName != null && !lectureName.isBlank())
                ? sanitizePromptInput(lectureName, 100) : "현재 강의";
        return switch (analysisType == null ? "" : analysisType) {
            case "submissions" -> """
                    "%s" 강의의 학생 제출물을 분석해주세요.
                    다음 항목으로 정리해주세요:
                    1. 전반적인 이해도 수준
                    2. 많이 틀린 유형의 문제 또는 개념
                    3. 우수한 학생과 도움이 필요한 학생의 특징
                    4. 다음 수업에서 보완해야 할 내용
                    현재 실제 데이터 연동 전이므로 일반적인 분석 가이드라인을 제시해주세요.
                    """.formatted(lecture);
            case "questions" -> """
                    "%s" 강의에서 학생들이 자주 질문하는 내용을 분석해주세요.
                    다음 항목으로 정리해주세요:
                    1. 가장 많이 질문되는 개념/주제
                    2. 질문 유형 분류 (개념 이해 / 문제 풀이 / 실습 / 기타)
                    3. 반복적으로 나오는 오해나 혼동 지점
                    4. 강의 자료나 설명 방식 개선 제안
                    현재 실제 데이터 연동 전이므로 일반적인 분석 가이드라인을 제시해주세요.
                    """.formatted(lecture);
            default -> """
                    "%s" 강의의 전반적인 수업 현황을 종합 분석해주세요.
                    다음 항목으로 정리해주세요:
                    1. 출석 및 참여도 현황
                    2. 학습 이해도 종합 평가
                    3. 개선이 필요한 핵심 영역
                    4. 다음 수업을 위한 권장 사항
                    현재 실제 데이터 연동 전이므로 일반적인 분석 가이드라인을 제시해주세요.
                    """.formatted(lecture);
        };
    }
}
