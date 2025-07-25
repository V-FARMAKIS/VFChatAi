package com.example.VF_ChatAi.repository;

import com.example.VF_ChatAi.model.ChatMessage;
import com.example.VF_ChatAi.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByConversationOrderByCreatedAtAsc(ChatConversation conversation);
    
    void deleteByConversation(ChatConversation conversation);
}
