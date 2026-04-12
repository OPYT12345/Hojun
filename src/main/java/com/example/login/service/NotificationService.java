package com.example.login.service;

import com.example.login.entity.Notification;
import com.example.login.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public void createGradeNotification(Long studentId, String lectureName, String assignmentTitle, Integer grade, String feedback) {
        Notification n = new Notification();
        n.setUserId(studentId);
        n.setType("GRADE");
        n.setTitle("📊 과제 채점 결과");
        String gradeText = grade != null ? grade + "점" : "채점 완료";
        String msg = "[" + lectureName + "] \"" + assignmentTitle + "\" — " + gradeText;
        if (feedback != null && !feedback.isBlank()) {
            msg += "\n" + (feedback.length() > 60 ? feedback.substring(0, 60) + "…" : feedback);
        }
        n.setMessage(msg);
        repo.save(n);
    }

    public void createBroadcastNotification(Long studentId, String lectureName, String announcementTitle) {
        Notification n = new Notification();
        n.setUserId(studentId);
        n.setType("BROADCAST");
        n.setTitle("📢 새 공지가 올라왔어요");
        n.setMessage("[" + lectureName + "] " + announcementTitle);
        repo.save(n);
    }

    public void createQaAnswerNotification(Long studentId, String lectureName, String questionContent, String answer) {
        Notification n = new Notification();
        n.setUserId(studentId);
        n.setType("QA_ANSWER");
        n.setTitle("💬 Q&A 답변이 달렸어요");
        String qc = questionContent != null ? questionContent : "";
        String an = answer != null ? answer : "";
        String q = qc.length() > 40 ? qc.substring(0, 40) + "…" : qc;
        String a = an.length() > 60 ? an.substring(0, 60) + "…" : an;
        n.setMessage("[" + lectureName + "] Q: " + q + "\nA: " + a);
        repo.save(n);
    }
}
