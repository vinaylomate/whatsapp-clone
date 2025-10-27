package com.whatsapp.clone.chat.repo;

import com.whatsapp.clone.chat.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findTop50ByChatIdOrderByCreatedAtDesc(java.util.UUID chatId);
}