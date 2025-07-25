package com.example.VF_ChatAi.ai.provider;

import com.example.VF_ChatAi.ai.config.AIConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Gemini AI Provider - FIXED VERSION
 * Compatible with VFChat's ChatMessage model
 */
@Service
@ConditionalOnProperty(name = "ai.gemini.enabled", havingValue = "true", matchIfMissing = true)
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final AIConfig aiConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiProvider(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AIResponse generateResponse(AIRequest request) throws AIException {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🧠 Making Gemini API call for user: {}", request.getUserId());

            Map<String, Object> requestBody = buildGeminiRequest(request);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String apiUrl = aiConfig.getGemini().getBaseUrl() + "/models/" +
                    aiConfig.getGemini().getModel() + ":generateContent?key=" +
                    aiConfig.getGemini().getApiKey();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(aiConfig.getGemini().getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            if (httpResponse.statusCode() == 429) {
                log.warn("🚦 Gemini API rate limited");
                throw new AIException("Gemini API rate limited. Please try again in a few moments.");
            }

            if (httpResponse.statusCode() != 200) {
                log.error("❌ Gemini API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
                throw new AIException("Gemini API returned status: " + httpResponse.statusCode());
            }

            return parseGeminiResponse(httpResponse.body(), responseTime, request.getConversationId());

        } catch (AIException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Gemini API call failed: {}", e.getMessage(), e);
            throw new AIException("Gemini request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildGeminiRequest(AIRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();


        Map<String, Object> systemContext = new HashMap<>();
        systemContext.put("role", "user");
        systemContext.put("parts", List.of(Map.of("text",
                "You are VF Assistant, a helpful AI assistant powered by Google Gemini. " +
                        "You are knowledgeable, friendly, and can help with various tasks including " +
                        "answering questions, writing, coding, analysis, and creative tasks. " +
                        "Always provide helpful, accurate, and engaging responses.")));
        contents.add(systemContext);


        Map<String, Object> systemResponse = new HashMap<>();
        systemResponse.put("role", "model");
        systemResponse.put("parts", List.of(Map.of("text",
                "Hello! I'm VF Assistant, powered by Google Gemini. I'm here to help you with any questions or tasks you have. How can I assist you today?")));
        contents.add(systemResponse);


        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            List<ChatMessage> recentHistory = request.getConversationHistory();
            int startIndex = Math.max(0, recentHistory.size() - 10);

            for (int i = startIndex; i < recentHistory.size(); i++) {
                ChatMessage msg = recentHistory.get(i);
                if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                    Map<String, Object> contentObj = new HashMap<>();
                    contentObj.put("role", msg.isUserMessage() ? "user" : "model");
                    contentObj.put("parts", List.of(Map.of("text", msg.getContent())));
                    contents.add(contentObj);
                }
            }
        }


        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("parts", List.of(Map.of("text", request.getMessage())));
        contents.add(userMessage);

        requestBody.put("contents", contents);


        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", aiConfig.getGemini().getTemperature());
        generationConfig.put("maxOutputTokens", aiConfig.getGemini().getMaxTokens());
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private AIResponse parseGeminiResponse(String responseBody, long responseTime, String conversationId)
            throws AIException {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").get("message").asText();
                throw new AIException("Gemini API error: " + errorMsg);
            }

            String content = "";
            String finishReason = "";

            if (rootNode.has("candidates") && rootNode.get("candidates").isArray()) {
                JsonNode candidates = rootNode.get("candidates");
                if (candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);

                    if (firstCandidate.has("finishReason")) {
                        finishReason = firstCandidate.get("finishReason").asText();
                    }

                    if ("SAFETY".equals(finishReason)) {
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

            log.info("✅ Gemini response generated successfully in {}ms", responseTime);

            return AIResponse.builder()
                    .response(content)
                    .provider(getProviderName())
                    .model(aiConfig.getGemini().getModel())
                    .responseTimeMs(responseTime)
                    .tokensUsed(estimateTokens(content))
                    .conversationId(conversationId)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to parse Gemini response: {}", e.getMessage(), e);
            throw new AIException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "";
    }

    @Override
    public boolean isEnabled() {
        return aiConfig.getGemini().isEnabled() &&
                aiConfig.getGemini().getApiKey() != null &&
                !aiConfig.getGemini().getApiKey().trim().isEmpty() &&
                !aiConfig.getGemini().getApiKey().contains("your-") &&
                aiConfig.getGemini().getApiKey().length() > 20;
    }

    @Override
    public boolean isHealthy() {
        if (!isEnabled()) return false;

        try {
            String url = aiConfig.getGemini().getBaseUrl() + "/models/" +
                    aiConfig.getGemini().getModel() + "?key=" +
                    aiConfig.getGemini().getApiKey();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            log.warn("❌ Gemini health check failed: {}", e.getMessage());
            return false;
        }
    }

    private int estimateTokens(String text) {
        return text.length() / 4;
    }
}