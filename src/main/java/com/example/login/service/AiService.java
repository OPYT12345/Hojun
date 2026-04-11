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
            throw new RuntimeException("OpenAI API 오류: HTTP " + response.statusCode() + " - " + response.body());
        }

        return extractContent(response.body());
    }

    private String buildSingleTurnBody(String systemPrompt, String userMessage) {
        return """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user",   "content": %s}
                  ],
                  "temperature": 0.7,
                  "max_tokens": 1024
                }
                """.formatted(MODEL, jsonString(systemPrompt), jsonString(userMessage));
    }

    private String buildMultiTurnBody(String systemPrompt, List<StudentChatRequest.Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(MODEL).append("\",\"messages\":[");
        sb.append("{\"role\":\"system\",\"content\":").append(jsonString(systemPrompt)).append("}");
        for (StudentChatRequest.Message m : messages) {
            String role = "assistant".equals(m.role()) ? "assistant" : "user";
            sb.append(",{\"role\":\"").append(role).append("\",\"content\":")
              .append(jsonString(m.content())).append("}");
        }
        sb.append("],\"temperature\":0.7,\"max_tokens\":1024}");
        return sb.toString();
    }

    /**
     * JSON 응답에서 choices[0].message.content 추출 (Jackson 사용)
     */
    private String extractContent(String json) throws Exception {
        return objectMapper.readTree(json)
                .path("choices").get(0)
                .path("message")
                .path("content")
                .asText();
    }

    /**
     * Java 문자열을 JSON 문자열 리터럴로 변환 (따옴표 포함)
     */
    private String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
