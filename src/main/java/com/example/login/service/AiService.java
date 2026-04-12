package com.example.login.service;

import com.example.login.dto.StudentChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${openai.api-key:}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 단일 사용자 메시지로 OpenAI 호출
     */
    public String callOpenAI(String systemPrompt, String userMessage) throws Exception {
        String body = buildSingleTurnBody(systemPrompt, userMessage);
        return sendRequest(body);
    }

    /**
     * 다중 턴 대화 히스토리로 OpenAI 호출 (학생 채팅용)
     */
    public String callOpenAIWithHistory(String systemPrompt, List<StudentChatRequest.Message> messages) throws Exception {
        String body = buildMultiTurnBody(systemPrompt, messages);
        return sendRequest(body);
    }

    private String sendRequest(String jsonBody) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. OPENAI_API_KEY 환경변수를 설정해주세요.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            // 응답 body는 서버 로그에만 기록 — 클라이언트에 노출 금지
            System.err.println("[AiService] OpenAI API 오류: HTTP " + response.statusCode() + " | " + response.body());
            throw new RuntimeException("OpenAI API 호출 실패 (HTTP " + response.statusCode() + ")");
        }

        return extractContent(response.body());
    }

    private String buildSingleTurnBody(String systemPrompt, String userMessage) throws Exception {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""),
            Map.of("role", "user",   "content", userMessage  != null ? userMessage  : "")
        );
        return objectMapper.writeValueAsString(Map.of(
            "model",       MODEL,
            "messages",    messages,
            "temperature", 0.7,
            "max_tokens",  1024
        ));
    }

    private String buildMultiTurnBody(String systemPrompt, List<StudentChatRequest.Message> messages) throws Exception {
        List<Map<String, String>> msgList = new java.util.ArrayList<>();
        msgList.add(Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""));
        for (StudentChatRequest.Message m : messages) {
            String role    = "assistant".equals(m.role()) ? "assistant" : "user";
            String content = m.content() != null ? m.content() : "";
            msgList.add(Map.of("role", role, "content", content));
        }
        return objectMapper.writeValueAsString(Map.of(
            "model",       MODEL,
            "messages",    msgList,
            "temperature", 0.7,
            "max_tokens",  1024
        ));
    }

    /**
     * JSON 응답에서 choices[0].message.content 추출
     */
    private String extractContent(String json) throws Exception {
        com.fasterxml.jackson.databind.JsonNode choices = objectMapper.readTree(json).path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            throw new RuntimeException("OpenAI 응답에 choices가 없습니다. 응답: " + json);
        }
        return choices.get(0)
                .path("message")
                .path("content")
                .asText();
    }
}
