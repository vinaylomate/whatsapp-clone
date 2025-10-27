package com.whatsapp.clone.chat.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_members", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}))
public class ChatMember {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;
    @Column(name = "chat_id", columnDefinition = "uuid")
    private UUID chatId;
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBER;
    private Instant joinedAt = Instant.now();

    @PrePersist
    public void pre() {
        if (id == null) id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getChatId() {
        return chatId;
    }

    public void setChatId(UUID c) {
        this.chatId = c;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID u) {
        this.userId = u;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role r) {
        this.role = r;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant t) {
        this.joinedAt = t;
    }

    public enum Role {ADMIN, MEMBER}
}