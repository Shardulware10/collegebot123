package com.college.chatbot;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false, length = 1000)
    private String message;

    private String intent;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public ChatMessage() {}

    public ChatMessage(String sessionId, String sender, String message, String intent) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.message = message;
        this.intent = intent;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getIntent() { return intent; }
    public Instant getCreatedAt() { return createdAt; }
}
