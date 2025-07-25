package com.example.VF_ChatAi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;
    
    @Column(name = "is_user_message", nullable = false)
    private boolean isUserMessage;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ChatMessage(ChatConversation conversation, boolean isUserMessage, String content) {
        this();
        this.conversation = conversation;
        this.isUserMessage = isUserMessage;
        this.content = content;
    }
    

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ChatConversation getConversation() {
        return conversation;
    }
    
    public void setConversation(ChatConversation conversation) {
        this.conversation = conversation;
    }
    
    public boolean isUserMessage() {
        return isUserMessage;
    }
    
    public void setUserMessage(boolean userMessage) {
        isUserMessage = userMessage;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
