package com.example.VF_ChatAi.controller;

import com.example.VF_ChatAi.model.ChatConversation;
import com.example.VF_ChatAi.model.ChatMessage;
import com.example.VF_ChatAi.model.User;
import com.example.VF_ChatAi.service.ChatConversationService;
import com.example.VF_ChatAi.service.UserService;
import com.example.VF_ChatAi.ai.provider.GeminiProvider;
import com.example.VF_ChatAi.ai.provider.AIRequest;
import com.example.VF_ChatAi.ai.provider.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatConversationService conversationService;

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private GeminiProvider geminiProvider;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Send a chat message - MAIN ENDPOINT
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        try {
            log.info("📨 Received chat message request");


            User user = getCurrentUser(session);
            if (user == null) {
                log.warn("❌ User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated", "UNAUTHORIZED"));
            }

            String message = (String) request.get("message");
            Long conversationId = request.get("conversationId") != null ?
                    Long.valueOf(request.get("conversationId").toString()) : null;

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Message cannot be empty", "VALIDATION_ERROR"));
            }

            log.info("💬 Processing message from user: {} - Message: {}", user.getEmail(),
                    message.substring(0, Math.min(50, message.length())));


            if (isImageRequest(message)) {
                return handleImageGeneration(message, conversationId, user);
            }


            ChatConversation conversation = getOrCreateConversation(conversationId, user, message);


            conversationService.addMessage(conversation.getId(), user, message, true);


            String aiResponse = generateAIResponse(message, conversation, user);


            conversationService.addMessage(conversation.getId(), user, aiResponse, false);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversationId", conversation.getId());
            response.put("conversationName", conversation.getName());
            response.put("aiResponse", aiResponse);
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ Message processed successfully for conversation: {}", conversation.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error sending chat message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Chat service temporarily unavailable", "INTERNAL_ERROR"));
        }
    }

    /**
     * Generate image endpoint
     */
    @PostMapping("/generate-image")
    public ResponseEntity<Map<String, Object>> generateImage(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        try {
            User user = getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated", "UNAUTHORIZED"));
            }

            String prompt = (String) request.get("prompt");
            Long conversationId = request.get("conversationId") != null ?
                    Long.valueOf(request.get("conversationId").toString()) : null;

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Prompt cannot be empty", "VALIDATION_ERROR"));
            }

            log.info("🎨 Generating image for prompt: {}", prompt);

            return handleImageGeneration(prompt, conversationId, user);

        } catch (Exception e) {
            log.error("❌ Error generating image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Image generation failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get user conversations with pagination
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {

        try {
            User user = getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated", "UNAUTHORIZED"));
            }

            List<ChatConversation> conversations = conversationService.getUserConversations(user);
            List<Map<String, Object>> conversationList = new ArrayList<>();

            for (ChatConversation conv : conversations) {
                Map<String, Object> convData = new HashMap<>();
                convData.put("id", conv.getId());
                convData.put("name", conv.getName());
                convData.put("createdAt", conv.getCreatedAt().toString());
                convData.put("updatedAt", conv.getUpdatedAt().toString());
                convData.put("messageCount", conv.getMessages().size());
                convData.put("conversationType", conv.getConversationType().name());
                convData.put("typeIcon", conv.getTypeIcon());
                convData.put("lastMessage", getLastMessagePreview(conv));
                conversationList.add(convData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversations", conversationList);
            response.put("total", conversations.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to load conversations", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get conversation messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> getConversationMessages(
            @PathVariable Long conversationId,
            HttpSession session) {

        try {
            User user = getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated", "UNAUTHORIZED"));
            }

            List<ChatMessage> messages = conversationService.getConversationMessages(conversationId, user);
            List<Map<String, Object>> messageList = new ArrayList<>();

            for (ChatMessage msg : messages) {
                Map<String, Object> msgData = new HashMap<>();
                msgData.put("id", msg.getId());
                msgData.put("content", msg.getContent());
                msgData.put("isUserMessage", msg.isUserMessage());
                msgData.put("createdAt", msg.getCreatedAt().toString());
                msgData.put("isImageMessage", isImageUrl(msg.getContent()));
                messageList.add(msgData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messageList);
            response.put("conversationId", conversationId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting conversation messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to load messages", "INTERNAL_ERROR"));
        }
    }

    /**
     * Rename conversation - NEW ENDPOINT
     */
    @PutMapping("/conversations/{conversationId}/rename")
    public ResponseEntity<Map<String, Object>> renameConversation(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        try {
            User user = getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated", "UNAUTHORIZED"));
            }

            String newName = (String) request.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Conversation name cannot be empty", "VALIDATION_ERROR"));
            }


            if (newName.trim().length() > 100) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Conversation name too long (max 100 characters)", "VALIDATION_ERROR"));
            }

            boolean renamed = conversationService.renameConversation(conversationId, user, newName.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("success", renamed);
            response.put("message", renamed ? "Conversation renamed successfully" : "Conversation not found");
            response.put("newName", newName.trim());

            if (renamed) {
                log.info("✏️ Conversation {} renamed to: {}", conversationId, newName.trim());
            } else {
                log.warn("❌ Failed to rename conversation {} - not found or access denied", conversationId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error renaming conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to rename conversation", "INTERNAL_ERROR"));
        }
    }

    /**
     * Delete conversation
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @PathVariable Long conversationId,
            HttpSession session) {

        try {
            User user = getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated", "UNAUTHORIZED"));
            }

            boolean deleted = conversationService.deleteConversation(conversationId, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "Conversation deleted successfully" : "Conversation not found");

            if (deleted) {
                log.info("🗑️ Conversation {} deleted successfully", conversationId);
            } else {
                log.warn("❌ Failed to delete conversation {} - not found or access denied", conversationId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error deleting conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete conversation", "INTERNAL_ERROR"));
        }
    }





    private User getCurrentUser(HttpSession session) {
        String email = (String) session.getAttribute("userEmail");
        if (email != null) {
            return userService.findByEmail(email).orElse(null);
        }
        return null;
    }

    private boolean isImageRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("generate image") ||
                lowerMessage.contains("create image") ||
                lowerMessage.contains("draw") ||
                lowerMessage.startsWith("image of") ||
                lowerMessage.contains("picture of") ||
                lowerMessage.contains("photo of") ||
                lowerMessage.contains("🎨");
    }

    private boolean isImageUrl(String content) {
        return content != null && content.startsWith("https://image.pollinations.ai/");
    }

    private ResponseEntity<Map<String, Object>> handleImageGeneration(String prompt, Long conversationId, User user) {
        try {
            log.info("🎨 Processing image generation request");


            ChatConversation conversation = getOrCreateConversation(conversationId, user, prompt, ChatConversation.ConversationType.IMAGE);


            conversationService.addMessage(conversation.getId(), user, "🎨 Generate image: " + prompt, true);


            String imageUrl = generateImageWithPollinations(prompt);


            conversationService.addMessage(conversation.getId(), user, imageUrl, false);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversationId", conversation.getId());
            response.put("conversationName", conversation.getName());
            response.put("imageUrl", imageUrl);
            response.put("aiResponse", "🎨 Image generated successfully!");
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ Image generated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Image generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Image generation failed: " + e.getMessage(), "IMAGE_ERROR"));
        }
    }

    private String generateImageWithPollinations(String prompt) {
        try {

            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            

            String imageUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt +
                    "?width=768&height=768&nologo=true&enhance=true&model=flux&seed=" + 
                    System.currentTimeMillis() + "&steps=30&guidance=7.5";

            log.info("🎨 Generated Pollinations.ai URL: {}", imageUrl);


            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .timeout(Duration.ofSeconds(5))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

                if (response.statusCode() == 200) {
                    log.info("✅ Image URL verified successfully");
                } else {
                    log.warn("⚠️ Image URL returned status: {} (but continuing anyway)", response.statusCode());
                }
            } catch (Exception e) {
                log.warn("⚠️ Could not verify image URL, but returning anyway: {}", e.getMessage());
            }

            return imageUrl;

        } catch (Exception e) {
            log.error("❌ Failed to generate Pollinations.ai URL: {}", e.getMessage(), e);
            throw new RuntimeException("Image generation failed: " + e.getMessage());
        }
    }

    private ChatConversation getOrCreateConversation(Long conversationId, User user, String message) {
        return getOrCreateConversation(conversationId, user, message, ChatConversation.ConversationType.CHAT);
    }

    private ChatConversation getOrCreateConversation(Long conversationId, User user, String message, ChatConversation.ConversationType type) {
        if (conversationId != null) {
            Optional<ChatConversation> convOpt = conversationService.getConversation(conversationId, user);
            if (convOpt.isPresent()) {
                return convOpt.get();
            }
        }


        String conversationName = extractConversationName(message, type);
        ChatConversation conversation = conversationService.createNewConversation(user, conversationName, type);
        log.info("🆕 Created new {} conversation: {}", type, conversation.getId());
        return conversation;
    }

    private String extractConversationName(String message, ChatConversation.ConversationType type) {
        String prefix = type == ChatConversation.ConversationType.IMAGE ? "🎨 " : "💬 ";
        String name = message.length() > 30 ? message.substring(0, 27) + "..." : message;
        return prefix + name.replaceAll("[\\r\\n]+", " ");
    }

    private String generateAIResponse(String message, ChatConversation conversation, User user) {
        try {
            log.info("🧠 Generating AI response for message");


            if (isGeminiAvailable()) {
                return callGeminiAPI(message, conversation, user);
            } else {
                log.warn("⚠️ Gemini API not available, using smart fallback");
                return generateSmartFallbackResponse(message, user);
            }

        } catch (Exception e) {
            log.error("❌ AI response generation failed: {}", e.getMessage(), e);
            return generateFallbackResponse(message);
        }
    }

    private boolean isGeminiAvailable() {
        return geminiApiKey != null &&
                !geminiApiKey.trim().isEmpty() &&
                !geminiApiKey.startsWith("YOUR_") &&
                !geminiApiKey.contains("your-") &&
                geminiApiKey.length() > 20;
    }

    private String callGeminiAPI(String message, ChatConversation conversation, User user) throws Exception {
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> requestBody = buildGeminiRequest(message, conversation, user);
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        log.info("🤖 Calling Gemini API with model: {}", geminiModel);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("📨 Gemini API response: status={}", response.statusCode());

        if (response.statusCode() == 429) {
            log.warn("🚦 Gemini API rate limited, using fallback");
            return "I'm experiencing high demand right now. " + generateSmartFallbackResponse(message, user);
        }

        if (response.statusCode() != 200) {
            log.error("❌ Gemini API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API returned status: " + response.statusCode());
        }

        return parseGeminiResponse(response.body());
    }

    private Map<String, Object> buildGeminiRequest(String message, ChatConversation conversation, User user) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();


        Map<String, Object> systemContext = new HashMap<>();
        systemContext.put("role", "user");
        systemContext.put("parts", List.of(Map.of("text",
                "You are VF Assistant, a helpful AI assistant powered by Google Gemini. " +
                        "You are knowledgeable, friendly, and can help with various tasks including " +
                        "answering questions, writing, coding, analysis, and creative tasks. " +
                        "Always provide helpful, accurate, and engaging responses. " +
                        "The user's name is " + user.getFirstName() + ".")));
        contents.add(systemContext);


        Map<String, Object> systemResponse = new HashMap<>();
        systemResponse.put("role", "model");
        systemResponse.put("parts", List.of(Map.of("text",
                "Hello! I'm VF Assistant, powered by Google Gemini. I'm here to help you with any questions or tasks you have. How can I assist you today?")));
        contents.add(systemResponse);


        try {
            List<ChatMessage> recentMessages = conversationService.getConversationMessages(conversation.getId(), user);
            int startIndex = Math.max(0, recentMessages.size() - 10);

            for (int i = startIndex; i < recentMessages.size(); i++) {
                ChatMessage msg = recentMessages.get(i);
                if (msg.getContent() != null && !msg.getContent().trim().isEmpty() && !isImageUrl(msg.getContent())) {
                    Map<String, Object> contentObj = new HashMap<>();
                    contentObj.put("role", msg.isUserMessage() ? "user" : "model");
                    contentObj.put("parts", List.of(Map.of("text", msg.getContent())));
                    contents.add(contentObj);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load conversation history: {}", e.getMessage());
        }


        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("parts", List.of(Map.of("text", message)));
        contents.add(userMessage);

        requestBody.put("contents", contents);


        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 2048);
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private String parseGeminiResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);

        if (rootNode.has("error")) {
            String errorMsg = rootNode.get("error").get("message").asText();
            throw new RuntimeException("Gemini API error: " + errorMsg);
        }

        String content = "";
        if (rootNode.has("candidates") && rootNode.get("candidates").isArray()) {
            JsonNode candidates = rootNode.get("candidates");
            if (candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);


                if (firstCandidate.has("finishReason") && "SAFETY".equals(firstCandidate.get("finishReason").asText())) {
                    content = "I apologize, but I can't provide a response to that request due to content policy restrictions. Could you please rephrase your question or ask about something else?";
                } else if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
                    JsonNode parts = firstCandidate.get("content").get("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        content = parts.get(0).get("text").asText();
                    }
                }
            }
        }

        if (content.isEmpty()) {
            content = "I apologize, but I couldn't generate a proper response. Could you try rephrasing your question?";
        }

        log.info("✅ Gemini response generated successfully");
        return content;
    }

    private String generateSmartFallbackResponse(String message, User user) {
        String lowerMessage = message.toLowerCase();
        String firstName = user.getFirstName();

        if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Hello " + firstName + "! I'm VF Assistant, powered by Google Gemini. How can I help you today?";
        } else if (lowerMessage.contains("how are you")) {
            return "I'm doing great, thank you for asking! I'm here and ready to help you with any questions, creative projects, or tasks you have in mind.";
        } else if (lowerMessage.contains("what can you do")) {
            return "I can help you with a wide variety of tasks:\n\n" +
                    "💬 **Conversations & Questions** - Ask me anything!\n" +
                    "📝 **Writing & Content** - Help with writing, editing, and creative content\n" +
                    "💻 **Code & Technical** - Programming help, debugging, and technical explanations\n" +
                    "🎨 **Image Generation** - Create images by saying 'generate image of...'\n" +
                    "🧠 **Analysis & Research** - Help analyze information and solve problems\n\n" +
                    "What would you like to explore today?";
        } else if (lowerMessage.contains("code") || lowerMessage.contains("program")) {
            return "I'd be happy to help you with coding! I can assist with:\n\n" +
                    "• Writing code in various programming languages\n" +
                    "• Debugging and fixing errors\n" +
                    "• Explaining programming concepts\n" +
                    "• Code reviews and optimization\n" +
                    "• Architecture and design patterns\n\n" +
                    "What programming challenge are you working on?";
        } else if (lowerMessage.contains("write") || lowerMessage.contains("creative")) {
            return "I love helping with creative writing! I can assist with:\n\n" +
                    "✍️ Stories, poems, and creative content\n" +
                    "📄 Professional documents and emails\n" +
                    "📝 Blog posts and articles\n" +
                    "🎭 Scripts and dialogue\n" +
                    "📊 Reports and summaries\n\n" +
                    "What kind of writing project are you working on?";
        } else {
            return "That's a great question about \"" + (message.length() > 50 ? message.substring(0, 47) + "..." : message) +
                    "\". I'd love to help you explore this topic further!\n\n" +
                    "Could you tell me more about what specifically you'd like to know or what you're trying to accomplish? " +
                    "The more details you provide, the better I can assist you.";
        }
    }

    private String generateFallbackResponse(String message) {
        return "I apologize, but I'm having some technical difficulties right now. " +
                "I received your message about \"" + (message.length() > 30 ? message.substring(0, 27) + "..." : message) +
                "\" and I'll do my best to help once the system is fully operational. Please try again in a moment!";
    }

    private String getLastMessagePreview(ChatConversation conversation) {
        if (conversation.getMessages().isEmpty()) {
            return "No messages yet";
        }

        ChatMessage lastMessage = conversation.getMessages().get(conversation.getMessages().size() - 1);
        String content = lastMessage.getContent();

        if (isImageUrl(content)) {
            return "🎨 Image generated";
        }

        return content.length() > 50 ? content.substring(0, 47) + "..." : content;
    }

    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("errorCode", errorCode);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
