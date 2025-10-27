package com.whatsapp.clone.chat.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chats")
public class Chat {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;
    @Enumerated(EnumType.STRING)
    private ChatType type;
    private String name; // for groups
    @Column(columnDefinition = "uuid")
    private UUID createdBy;
    private Instant createdAt = Instant.now();

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

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType t) {
        this.type = t;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID u) {
        this.createdBy = u;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant t) {
        this.createdAt = t;
    }

    public enum ChatType {DIRECT, GROUP}
}
