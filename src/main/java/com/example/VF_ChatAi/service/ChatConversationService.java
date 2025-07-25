package com.example.VF_ChatAi.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.VF_ChatAi.model.ChatConversation;
import com.example.VF_ChatAi.model.ChatMessage;
import com.example.VF_ChatAi.model.User;
import com.example.VF_ChatAi.repository.ChatConversationRepository;
import com.example.VF_ChatAi.repository.ChatMessageRepository;

@Service
@Transactional
public class ChatConversationService {

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    public List<ChatConversation> getUserConversations(User user) {
        return conversationRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    public ChatConversation createNewConversation(User user, String name) {
        return createNewConversation(user, name, ChatConversation.ConversationType.CHAT);
    }

    public ChatConversation createNewConversation(User user, String name, ChatConversation.ConversationType conversationType) {
        if (name == null || name.trim().isEmpty()) {
            String typePrefix = conversationType == ChatConversation.ConversationType.IMAGE ? "Image " : "Chat ";
            name = typePrefix + (conversationRepository.countByUser(user) + 1);
        }

        ChatConversation conversation = new ChatConversation(user, name.trim(), conversationType);
        return conversationRepository.save(conversation);
    }

    public Optional<ChatConversation> getConversation(Long conversationId, User user) {
        return conversationRepository.findByIdAndUserWithMessages(conversationId, user);
    }


    public ChatMessage addMessage(Long conversationId, User user, String content, boolean isUserMessage) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findByIdAndUserWithMessages(conversationId, user);

        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        ChatConversation conversation = conversationOpt.get();
        ChatMessage message = new ChatMessage(conversation, isUserMessage, content);

        conversation.addMessage(message);
        conversation.setUpdatedAt(LocalDateTime.now());

        conversationRepository.save(conversation);
        return messageRepository.save(message);
    }

    public boolean renameConversation(Long conversationId, User user, String newName) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findByIdAndUserWithMessages(conversationId, user);

        if (conversationOpt.isEmpty()) {
            return false;
        }

        ChatConversation conversation = conversationOpt.get();
        conversation.setName(newName.trim());
        conversationRepository.save(conversation);
        return true;
    }

    public boolean deleteConversation(Long conversationId, User user) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findByIdAndUserWithMessages(conversationId, user);

        if (conversationOpt.isEmpty()) {
            return false;
        }

        conversationRepository.delete(conversationOpt.get());
        return true;
    }

    public List<ChatMessage> getConversationMessages(Long conversationId, User user) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findByIdAndUserWithMessages(conversationId, user);

        if (conversationOpt.isEmpty()) {
            return List.of();
        }

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversationOpt.get());
    }


    public Optional<ChatConversation> findById(Long id) {
        return conversationRepository.findById(id);
    }

    public List<ChatMessage> getRecentMessages(Long conversationId, int limit) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findById(conversationId);

        if (conversationOpt.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> allMessages = messageRepository.findByConversationOrderByCreatedAtAsc(conversationOpt.get());


        if (allMessages.size() <= limit) {
            return allMessages;
        }

        return allMessages.subList(allMessages.size() - limit, allMessages.size());
    }

    public void addMessageToConversation(Long conversationId, String content, boolean isUserMessage) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findById(conversationId);

        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        ChatConversation conversation = conversationOpt.get();
        ChatMessage message = new ChatMessage(conversation, isUserMessage, content);

        conversation.addMessage(message);
        conversation.setUpdatedAt(LocalDateTime.now());

        conversationRepository.save(conversation);
        messageRepository.save(message);
    }

    public boolean deleteConversation(Long conversationId) {
        Optional<ChatConversation> conversationOpt = conversationRepository.findById(conversationId);

        if (conversationOpt.isEmpty()) {
            return false;
        }

        conversationRepository.delete(conversationOpt.get());
        return true;
    }
}