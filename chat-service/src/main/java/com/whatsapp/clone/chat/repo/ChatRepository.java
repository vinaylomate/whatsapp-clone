package com.whatsapp.clone.chat.repo;

import com.whatsapp.clone.chat.domain.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, java.util.UUID> {
}