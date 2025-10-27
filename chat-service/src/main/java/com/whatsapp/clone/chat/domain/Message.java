package com.whatsapp.clone.chat.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {@Index(columnList = "chat_id, created_at")})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "chat_id", columnDefinition = "uuid")
    private UUID chatId;
    @Column(name = "sender_id", columnDefinition = "uuid")
    private UUID senderId;
    private String senderName;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Message() {
    }

    public Message(UUID chatId, UUID senderId, String senderName, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public UUID getChatId() {
        return chatId;
    }

    public void setChatId(UUID v) {
        this.chatId = v;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID v) {
        this.senderId = v;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String v) {
        this.senderName = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        this.content = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant t) {
        this.createdAt = t;
    }
}