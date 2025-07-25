package com.example.VF_ChatAi.ai.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * AI Chat Controller - FIXED VERSION FOR IMAGE GENERATION
 * This controller is used by the frontend for AI chat and image generation
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIChatController {

    private static final Logger log = LoggerFactory.getLogger(AIChatController.class);


    @Value("${ai.gemini.enabled:false}")
    private boolean geminiEnabled;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;


    @Value("${ai.image.enabled:true}")
    private boolean imageEnabled;


    private static final int MAX_PROMPT_LENGTH = 4000;
    private static final int MAX_CONVERSATION_HISTORY = 50;


    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    private final ObjectMapper objectMapper = new ObjectMapper();


    private final Map<String, List<Map<String, Object>>> conversations = new ConcurrentHashMap<>();


    private final Map<String, List<Long>> rateLimitMap = new ConcurrentHashMap<>();

    /**
     * Health check endpoint (public access)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        health.put("gemini", Map.of(
                "enabled", geminiEnabled,
                "healthy", geminiEnabled ? checkGeminiHealth() : false,
                "model", geminiModel
        ));

        health.put("image", Map.of(
                "enabled", imageEnabled,
                "healthy", imageEnabled,
                "provider", "pollinations.ai"
        ));

        boolean overallHealthy = (!geminiEnabled || checkGeminiHealth()) && imageEnabled;

        health.put("overall", Map.of(
                "healthy", overallHealthy,
                "activeConversations", conversations.size(),
                "timestamp", System.currentTimeMillis()
        ));

        health.put("status", overallHealthy ? "healthy" : "degraded");

        return ResponseEntity.ok(health);
    }

    /**
     * Main chat endpoint - requires authentication
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        long startTime = System.currentTimeMillis();

        try {

            String userEmail = (String) session.getAttribute("userEmail");
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            String message = (String) request.get("message");
            String conversationId = (String) request.get("conversationId");


            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Message cannot be empty", "VALIDATION_ERROR"));
            }


            message = sanitizeInput(message.trim());
            if (message.length() > MAX_PROMPT_LENGTH) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Message too long", "MESSAGE_TOO_LONG"));
            }


            if (!checkRateLimit(conversationId != null ? conversationId : userEmail)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(createErrorResponse("Rate limit exceeded", "RATE_LIMITED"));
            }

            log.info("Processing chat request: user={}, message length={}", userEmail, message.length());


            addToConversation(conversationId, "user", message);

            Map<String, Object> response;


            if (isImageRequest(message)) {
                response = handleImageRequest(message, request);
            } else {

                if (geminiEnabled && isValidApiKey(geminiApiKey)) {
                    response = callGeminiWithRetry(message, request);
                } else {
                    response = generateEnhancedMockResponse(message, conversationId);
                }
            }


            if (response.containsKey("response")) {
                addToConversation(conversationId, "assistant", response.get("response").toString());
            }

            long responseTime = System.currentTimeMillis() - startTime;
            response.put("totalResponseTime", responseTime);
            response.put("conversationId", conversationId);


            Map<String, Object> standardResponse = new HashMap<>();
            standardResponse.put("success", true);
            standardResponse.put("message", "Response generated successfully");
            standardResponse.put("data", response);

            return ResponseEntity.ok(standardResponse);

        } catch (Exception e) {
            log.error("Chat request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Chat service temporarily unavailable", "INTERNAL_ERROR"));
        }
    }

    /**
     * Image generation endpoint - FIXED VERSION
     */
    @PostMapping("/generate-image")
    public ResponseEntity<Map<String, Object>> generateImage(
            @RequestBody Map<String, Object> request,
            HttpSession session) {

        long startTime = System.currentTimeMillis();

        try {

            String userEmail = (String) session.getAttribute("userEmail");
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            String message = (String) request.get("message");
            String conversationId = (String) request.get("conversationId");


            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Message cannot be empty", "VALIDATION_ERROR"));
            }


            message = sanitizeInput(message.trim());
            if (message.length() > MAX_PROMPT_LENGTH) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Message too long", "MESSAGE_TOO_LONG"));
            }


            if (!checkRateLimit(conversationId != null ? conversationId : userEmail)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(createErrorResponse("Rate limit exceeded", "RATE_LIMITED"));
            }

            log.info("Processing image generation request: user={}, prompt length={}", userEmail, message.length());


            addToConversation(conversationId, "user", message);

            Map<String, Object> response = generateImageWithPollinations(message, conversationId);


            if (response.containsKey("response")) {
                addToConversation(conversationId, "assistant", response.get("response").toString());
            }

            long responseTime = System.currentTimeMillis() - startTime;
            response.put("totalResponseTime", responseTime);
            response.put("conversationId", conversationId);


            Map<String, Object> standardResponse = new HashMap<>();
            standardResponse.put("success", true);
            standardResponse.put("message", "Image generation completed");
            standardResponse.put("data", response);

            return ResponseEntity.ok(standardResponse);

        } catch (Exception e) {
            log.error("Image generation request failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Image generation service temporarily unavailable", "INTERNAL_ERROR"));
        }
    }



    private boolean isImageRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("generate image") ||
                lowerMessage.contains("create image") ||
                lowerMessage.contains("draw") ||
                lowerMessage.startsWith("image of") ||
                lowerMessage.contains("picture of");
    }

    private Map<String, Object> handleImageRequest(String message, Map<String, Object> request) {
        String conversationId = (String) request.get("conversationId");
        return generateImageWithPollinations(message, conversationId);
    }

    private Map<String, Object> generateImageWithPollinations(String prompt, String conversationId) {
        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            String directImageUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt +
                    "?width=512&height=512&nologo=true&enhance=true&seed=" +
                    System.currentTimeMillis();

            log.info("🎨 Generating image with Pollinations.ai: {}", directImageUrl);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(directImageUrl))
                    .timeout(Duration.ofSeconds(15))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                Map<String, Object> result = new HashMap<>();
                result.put("response", "✨ Image generated successfully: " + prompt);
                result.put("imageUrl", directImageUrl);
                result.put("provider", "pollinations.ai");
                result.put("model", "stable-diffusion");
                result.put("success", true);

                log.info("✅ Image generated successfully!");
                return result;
            } else {
                log.error("❌ Pollinations request failed with status: {}", response.statusCode());
                return generateMockImageResponse(prompt, conversationId);
            }

        } catch (Exception e) {
            log.error("Image generation error: {}", e.getMessage(), e);
            return generateMockImageResponse(prompt, conversationId);
        }
    }

    private Map<String, Object> generateMockImageResponse(String prompt, String conversationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("response", "🎨 Mock image generation: I would create an image of \"" + prompt + "\" but image generation is currently unavailable.");
        response.put("imageUrl", "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTEyIiBoZWlnaHQ9IjUxMiIgdmlld0JveD0iMCAwIDUxMiA1MTIiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI1MTIiIGhlaWdodD0iNTEyIiBmaWxsPSIjRjNGNEY2Ii8+CjxwYXRoIGQ9Ik0yNTYgMTkyQzIyMy4yIDc5IDIyMy4yIDI5MiAyNTYgMjkyQzI4OC44IDI5MiAyODguOCA3OSAyNTYgMTkyWiIgZmlsbD0iIzlDQTNBRiIvPgo8L3N2Zz4K");
        response.put("provider", "mock");
        response.put("model", "placeholder");
        response.put("success", true);
        return response;
    }

    private Map<String, Object> callGeminiWithRetry(String message, Map<String, Object> originalRequest) {
        int maxRetries = 2;
        int retryDelay = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return callGemini(message, originalRequest);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("429") && attempt < maxRetries) {
                    log.info("Gemini rate limited, retrying in {}ms", retryDelay);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    retryDelay *= 2;
                } else {
                    log.error("Gemini API call failed on attempt {}: {}", attempt, e.getMessage());
                    if (attempt == maxRetries) {
                        return generateEnhancedMockResponse(message, (String) originalRequest.get("conversationId"));
                    }
                }
            }
        }

        return generateEnhancedMockResponse(message, (String) originalRequest.get("conversationId"));
    }

    private Map<String, Object> callGemini(String message, Map<String, Object> originalRequest) throws Exception {
        long startTime = System.currentTimeMillis();

        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;
        Map<String, Object> requestBody = buildGeminiRequest(message, originalRequest);

        log.info("Calling Gemini API with model: {}", geminiModel);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long responseTime = System.currentTimeMillis() - startTime;

        log.info("Gemini API response: status={}, time={}ms", httpResponse.statusCode(), responseTime);

        if (httpResponse.statusCode() == 429) {
            log.warn("Gemini API rate limited. Falling back to mock response.");
            Map<String, Object> rateLimitResponse = generateEnhancedMockResponse(message, (String) originalRequest.get("conversationId"));
            rateLimitResponse.put("response", "🚦 Gemini API is currently rate limited. Here's a helpful response: " +
                    generateSimpleResponse(message));
            rateLimitResponse.put("provider", "gemini-rate-limited");
            return rateLimitResponse;
        }

        if (httpResponse.statusCode() != 200) {
            log.error("Gemini API error: status={}, body={}", httpResponse.statusCode(), httpResponse.body());
            throw new RuntimeException("Gemini API returned status: " + httpResponse.statusCode() + " - " + httpResponse.body());
        }

        return parseGeminiResponse(httpResponse.body(), responseTime, (String) originalRequest.get("conversationId"));
    }

    private Map<String, Object> buildGeminiRequest(String message, Map<String, Object> originalRequest) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();


        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("parts", List.of(Map.of("text", message)));
        contents.add(userMessage);

        requestBody.put("contents", contents);


        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private Map<String, Object> parseGeminiResponse(String responseBody, long responseTime, String conversationId) throws Exception {
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
                if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
                    JsonNode parts = firstCandidate.get("content").get("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        content = parts.get(0).get("text").asText();
                    }
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("response", content);
        response.put("provider", "gemini");
        response.put("model", geminiModel);
        response.put("responseTimeMs", responseTime);
        response.put("conversationId", conversationId);
        response.put("success", true);

        return response;
    }

    private Map<String, Object> generateEnhancedMockResponse(String message, String conversationId) {
        String response = generateSimpleResponse(message);

        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("provider", "mock");
        result.put("model", "mock-assistant");
        result.put("success", true);
        result.put("conversationId", conversationId);

        return result;
    }

    private String generateSimpleResponse(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Hello! I'm VFChatAI. How can I help you today?";
        } else if (lowerMessage.contains("how are you")) {
            return "I'm doing well, thank you for asking! I'm ready to help you with any questions or tasks.";
        } else if (lowerMessage.contains("what can you do")) {
            return "I can help with various tasks including answering questions, writing, analysis, and more. I can also generate images!";
        } else if (lowerMessage.contains("code") || lowerMessage.contains("program")) {
            return "I'd be happy to help you with coding! What programming task do you need assistance with?";
        } else {
            return "That's an interesting point about \"" + message.substring(0, Math.min(30, message.length())) +
                    "...\". I'd love to discuss this further! How can I help you with this topic?";
        }
    }

    private void addToConversation(String conversationId, String role, String content) {
        if (conversationId == null) conversationId = "default";

        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(Map.of(
                        "role", role,
                        "content", content,
                        "timestamp", System.currentTimeMillis()
                ));


        List<Map<String, Object>> history = conversations.get(conversationId);
        if (history.size() > MAX_CONVERSATION_HISTORY) {
            history.subList(0, history.size() - MAX_CONVERSATION_HISTORY).clear();
        }
    }

    private boolean checkRateLimit(String identifier) {
        long now = System.currentTimeMillis();
        List<Long> requests = rateLimitMap.computeIfAbsent(identifier, k -> new ArrayList<>());

        requests.removeIf(time -> now - time > 60000); // Remove requests older than 1 minute

        if (requests.size() >= 30) { // 30 requests per minute limit
            return false;
        }

        requests.add(now);
        return true;
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>\"'&]", "").replaceAll("\\s+", " ").trim();
    }

    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", message);
        errorData.put("errorCode", errorCode);
        errorData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", errorData);

        return response;
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your-") && apiKey.length() > 10;
    }

    private boolean checkGeminiHealth() {
        if (!geminiEnabled || !isValidApiKey(geminiApiKey)) {
            return false;
        }

        try {
            String url = geminiBaseUrl + "/models/" + geminiModel + "?key=" + geminiApiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            log.error("Gemini health check failed: {}", e.getMessage());
            return false;
        }
    }


}