package com.example.VF_ChatAi.ai.service;

import com.example.VF_ChatAi.model.ChatConversation;
import com.example.VF_ChatAi.model.ChatMessage;
import com.example.VF_ChatAi.model.User;
import com.example.VF_ChatAi.service.ChatConversationService;
import com.example.VF_ChatAi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatConversationService conversationService;

    @Autowired
    private UserService userService;

    /**
     * Create a new conversation with specific type
     */
    public ChatConversation createConversation(String userId, String name, String aiModel, String category) {
        try {
            User user = getUserFromIdentifier(userId);


            ChatConversation.ConversationType conversationType;
            if ("image".equals(aiModel) || "pollinations".equals(aiModel)) {
                conversationType = ChatConversation.ConversationType.IMAGE;
            } else {
                conversationType = ChatConversation.ConversationType.CHAT;
            }

            return conversationService.createNewConversation(user, name, conversationType);

        } catch (Exception e) {
            log.error("Error creating conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create conversation", e);
        }
    }

    /**
     * Get conversation by ID
     */
    public ChatConversation getConversation(String conversationId) {
        try {
            Long id = Long.parseLong(conversationId);
            return conversationService.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            log.warn("Invalid conversation ID format: {}", conversationId);
            return null;
        } catch (Exception e) {
            log.error("Error getting conversation: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get user conversations
     */
    public List<ChatConversation> getUserConversations(String userId) {
        try {
            User user = getUserFromIdentifier(userId);
            return conversationService.getUserConversations(user);

        } catch (Exception e) {
            log.error("Error getting user conversations: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get conversation history/messages
     */
    public List<ChatMessage> getConversationHistory(String conversationId, int limit) {
        try {
            Long id = Long.parseLong(conversationId);
            return conversationService.getRecentMessages(id, limit);

        } catch (NumberFormatException e) {
            log.warn("Invalid conversation ID format: {}", conversationId);
            return List.of();
        } catch (Exception e) {
            log.error("Error getting conversation history: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Save a message to conversation
     */
    public void saveMessage(String conversationId, String role, String content, Map<String, Object> metadata) {
        try {
            Long id = Long.parseLong(conversationId);


            boolean isUserMessage = "user".equals(role);

            conversationService.addMessageToConversation(id, content, isUserMessage);

        } catch (NumberFormatException e) {
            log.warn("Invalid conversation ID format: {}", conversationId);
        } catch (Exception e) {
            log.error("Error saving message: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete a conversation
     */
    public boolean deleteConversation(String conversationId) {
        try {
            Long id = Long.parseLong(conversationId);
            return conversationService.deleteConversation(id);

        } catch (NumberFormatException e) {
            log.warn("Invalid conversation ID format: {}", conversationId);
            return false;
        } catch (Exception e) {
            log.error("Error deleting conversation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Helper method to get User from identifier (email or ID)
     */
    private User getUserFromIdentifier(String userId) {

        Optional<User> userOpt = userService.findByEmail(userId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }


        try {
            Long userIdLong = Long.parseLong(userId);
            userOpt = userService.findById(userIdLong);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        } catch (NumberFormatException e) {

        }

        log.warn("Could not find user with identifier: {}", userId);
        throw new RuntimeException("User not found: " + userId);
    }
}