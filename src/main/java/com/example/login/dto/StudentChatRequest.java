package com.example.login.dto;

import java.util.List;

public class StudentChatRequest {
    private Long lectureId;
    private String lectureName;
    private String context;
    private List<Message> messages;

    public record Message(String role, String content) {}

    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }

    public String getLectureName() { return lectureName; }
    public void setLectureName(String lectureName) { this.lectureName = lectureName; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}
