package com.whatsapp.clone.chat.repo;

import com.whatsapp.clone.chat.domain.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, java.util.UUID> {
    List<ChatMember> findByUserId(java.util.UUID userId);

    Optional<ChatMember> findByChatIdAndUserId(java.util.UUID chatId, java.util.UUID userId);

    List<ChatMember> findByChatId(java.util.UUID chatId);
}