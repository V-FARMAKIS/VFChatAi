package com.example.VF_ChatAi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    private ConversationType conversationType = ConversationType.CHAT;
    
    @Column(name = "ai_provider", length = 50)
    private String aiProvider = "gemini";
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();
    
    public enum ConversationType {
        CHAT("💬", "Chat", "Conversation with AI assistant"),
        IMAGE("🎨", "Image", "AI image generation");
        
        private final String icon;
        private final String displayName;
        private final String description;
        
        ConversationType(String icon, String displayName, String description) {
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getIcon() { return icon; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public ChatConversation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ChatConversation(User user, String name) {
        this();
        this.user = user;
        this.name = name;
    }
    
    public ChatConversation(User user, String name, ConversationType conversationType) {
        this(user, name);
        this.conversationType = conversationType;
        if (conversationType == ConversationType.IMAGE) {
            this.aiProvider = "pollinations";
        }
    }
    

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public ConversationType getConversationType() {
        return conversationType;
    }
    
    public void setConversationType(ConversationType conversationType) {
        this.conversationType = conversationType;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getAiProvider() {
        return aiProvider;
    }
    
    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        message.setConversation(this);
        this.updatedAt = LocalDateTime.now();
    }
    

    public boolean isImageConversation() {
        return this.conversationType == ConversationType.IMAGE;
    }
    
    public boolean isChatConversation() {
        return this.conversationType == ConversationType.CHAT;
    }
    
    public String getTypeIcon() {
        return this.conversationType.getIcon();
    }
    
    public String getTypeDisplayName() {
        return this.conversationType.getDisplayName();
    }
    
    public int getMessageCount() {
        return this.messages.size();
    }
}
