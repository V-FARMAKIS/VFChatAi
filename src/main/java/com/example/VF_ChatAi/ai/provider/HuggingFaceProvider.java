package com.example.VF_ChatAi.ai.provider;

import org.springframework.beans.factory.annotation.Value;
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

@Service
@ConditionalOnProperty(name = "ai.huggingface.enabled", havingValue = "true")
public class HuggingFaceProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceProvider.class);

    @Value("${ai.huggingface.api-key:}")
    private String apiKey;

    @Value("${ai.huggingface.model:microsoft/DialoGPT-large}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "huggingface";
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("${HUGGINGFACE_API_KEY:}");
    }

    @Override
    public AIResponse generateResponse(AIRequest request) throws AIException {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Making Hugging Face API call for user: {}", request.getUserId());


            Map<String, Object> requestBody = buildHuggingFaceRequest(request);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String apiUrl = "https://api-inference.huggingface.co/models/" + model;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            if (httpResponse.statusCode() != 200) {
                log.error("Hugging Face API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
                throw new AIException("Hugging Face API returned status: " + httpResponse.statusCode());
            }

            return parseHuggingFaceResponse(httpResponse.body(), responseTime, request.getConversationId());

        } catch (Exception e) {
            log.error("Hugging Face API call failed: {}", e.getMessage(), e);
            throw new AIException("Hugging Face request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildHuggingFaceRequest(AIRequest request) {
        Map<String, Object> requestBody = new HashMap<>();


        String input = request.getMessage();


        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            StringBuilder conversationBuilder = new StringBuilder();
            for (ChatMessage msg : request.getConversationHistory()) {
                if ("user".equals(msg.getRole())) {
                    conversationBuilder.append("User: ").append(msg.getContent()).append("\n");
                } else if ("assistant".equals(msg.getRole())) {
                    conversationBuilder.append("Bot: ").append(msg.getContent()).append("\n");
                }
            }
            conversationBuilder.append("User: ").append(request.getMessage()).append("\nBot: ");
            input = conversationBuilder.toString();
        }

        requestBody.put("inputs", input);


        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_length", 150);
        parameters.put("temperature", 0.7);
        parameters.put("do_sample", true);
        parameters.put("top_p", 0.9);
        requestBody.put("parameters", parameters);

        return requestBody;
    }

    private AIResponse parseHuggingFaceResponse(String responseBody, long responseTime, String conversationId)
            throws AIException {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("error")) {
                throw new AIException("Hugging Face API error: " + rootNode.get("error").asText());
            }

            String content;
            if (rootNode.isArray() && rootNode.size() > 0) {
                content = rootNode.get(0).get("generated_text").asText();


                if (content.contains("Bot: ")) {
                    int botIndex = content.lastIndexOf("Bot: ");
                    if (botIndex != -1) {
                        content = content.substring(botIndex + 5).trim();
                    }
                }


                content = content.replaceAll("User:.*", "").trim();

                if (content.isEmpty()) {
                    content = "I understand what you're saying. Could you tell me more about that?";
                }
            } else {
                content = "I'm here to help! Could you rephrase your question?";
            }

            log.info("Hugging Face response received in {}ms", responseTime);

            return AIResponse.builder()
                    .response(content)
                    .provider(getProviderName())
                    .model(model)
                    .responseTimeMs(responseTime)
                    .tokensUsed(content.length() / 4) // Rough estimate
                    .conversationId(conversationId)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Hugging Face response: {}", e.getMessage(), e);
            throw new AIException("Failed to parse Hugging Face response: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        if (!isEnabled()) return false;

        try {
            Map<String, Object> testRequest = new HashMap<>();
            testRequest.put("inputs", "Hello");
            String jsonBody = objectMapper.writeValueAsString(testRequest);

            String apiUrl = "https://api-inference.huggingface.co/models/" + model;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean healthy = response.statusCode() == 200;

            log.debug("Hugging Face health check: {}", healthy ? "OK" : "FAILED");
            return healthy;

        } catch (Exception e) {
            log.warn("Hugging Face health check failed: {}", e.getMessage());
            return false;
        }
    }
}