package com.example.VF_ChatAi.ai.controller;

import com.example.VF_ChatAi.ai.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Test Controller for AI Integration
 * Use this to verify that all configurations are working properly
 */
@RestController
@RequestMapping("/api/ai/test")
@CrossOrigin(origins = "*")
public class AITestController {

    private static final Logger log = LoggerFactory.getLogger(AITestController.class);

    @Autowired
    private AIConfig aiConfig;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        try {
            Map<String, Object> config = new HashMap<>();
            

            Map<String, Object> gemini = new HashMap<>();
            gemini.put("enabled", aiConfig.getGemini().isEnabled());
            gemini.put("model", aiConfig.getGemini().getModel());
            gemini.put("baseUrl", aiConfig.getGemini().getBaseUrl());
            gemini.put("temperature", aiConfig.getGemini().getTemperature());
            gemini.put("maxTokens", aiConfig.getGemini().getMaxTokens());
            gemini.put("timeoutSeconds", aiConfig.getGemini().getTimeoutSeconds());
            gemini.put("apiKeyConfigured", aiConfig.getGemini().getApiKey() != null && !aiConfig.getGemini().getApiKey().trim().isEmpty());
            config.put("gemini", gemini);


            Map<String, Object> image = new HashMap<>();
            image.put("enabled", aiConfig.getImage().isEnabled());
            image.put("model", aiConfig.getImage().getModel());
            image.put("baseUrl", aiConfig.getImage().getBaseUrl());
            image.put("saveDirectory", aiConfig.getImage().getSaveDirectory());
            image.put("maxSize", aiConfig.getImage().getMaxSize());
            config.put("image", image);


            Map<String, Object> general = new HashMap<>();
            general.put("defaultProvider", aiConfig.getGeneral().getDefaultProvider());
            general.put("timeoutSeconds", aiConfig.getGeneral().getTimeoutSeconds());
            general.put("retryAttempts", aiConfig.getGeneral().getRetryAttempts());
            general.put("maxPromptLength", aiConfig.getGeneral().getMaxPromptLength());
            general.put("enableFallback", aiConfig.getGeneral().isEnableFallback());
            

            Map<String, Object> rateLimit = new HashMap<>();
            rateLimit.put("enabled", aiConfig.getGeneral().getRateLimit().isEnabled());
            rateLimit.put("requestsPerMinute", aiConfig.getGeneral().getRateLimit().getRequestsPerMinute());
            rateLimit.put("requestsPerHour", aiConfig.getGeneral().getRateLimit().getRequestsPerHour());
            rateLimit.put("burstCapacity", aiConfig.getGeneral().getRateLimit().getBurstCapacity());
            general.put("rateLimit", rateLimit);
            config.put("general", general);


            Map<String, Object> memory = new HashMap<>();
            memory.put("enabled", aiConfig.getMemory().isEnabled());
            memory.put("maxConversationHistory", aiConfig.getMemory().getMaxConversationHistory());
            memory.put("summaryThreshold", aiConfig.getMemory().getSummaryThreshold());
            memory.put("maxMemoryEntries", aiConfig.getMemory().getMaxMemoryEntries());
            memory.put("contextWindow", aiConfig.getMemory().getContextWindow());
            memory.put("cleanupInterval", aiConfig.getMemory().getCleanupInterval());
            config.put("memory", memory);


            Map<String, Object> bots = new HashMap<>();
            bots.put("enabled", aiConfig.getBots().isEnabled());
            
            Map<String, Object> botConfigs = new HashMap<>();
            botConfigs.put("codeAssistant", Map.of(
                "enabled", aiConfig.getBots().getCodeAssistant().isEnabled(),
                "temperature", aiConfig.getBots().getCodeAssistant().getTemperature()
            ));
            botConfigs.put("writingCoach", Map.of(
                "enabled", aiConfig.getBots().getWritingCoach().isEnabled(),
                "temperature", aiConfig.getBots().getWritingCoach().getTemperature()
            ));
            botConfigs.put("creativeDirector", Map.of(
                "enabled", aiConfig.getBots().getCreativeDirector().isEnabled(),
                "temperature", aiConfig.getBots().getCreativeDirector().getTemperature()
            ));
            botConfigs.put("dataAnalyst", Map.of(
                "enabled", aiConfig.getBots().getDataAnalyst().isEnabled(),
                "temperature", aiConfig.getBots().getDataAnalyst().getTemperature()
            ));
            botConfigs.put("tutor", Map.of(
                "enabled", aiConfig.getBots().getTutor().isEnabled(),
                "temperature", aiConfig.getBots().getTutor().getTemperature()
            ));
            botConfigs.put("general", Map.of(
                "enabled", aiConfig.getBots().getGeneral().isEnabled(),
                "temperature", aiConfig.getBots().getGeneral().getTemperature()
            ));
            
            bots.put("configurations", botConfigs);
            config.put("bots", bots);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            response.put("configuration", config);

            log.info("✅ Configuration retrieved successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error retrieving configuration: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve configuration: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateConfiguration() {
        Map<String, Object> validation = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();


        if (!aiConfig.getGemini().isEnabled()) {
            issues.add("Gemini AI is disabled");
        } else {
            if (aiConfig.getGemini().getApiKey() == null || aiConfig.getGemini().getApiKey().trim().isEmpty() || aiConfig.getGemini().getApiKey().contains("your-")) {
                issues.add("Gemini API key is not properly configured");
            }
            if (aiConfig.getGemini().getTemperature() < 0 || aiConfig.getGemini().getTemperature() > 2) {
                recommendations.add("Gemini temperature should be between 0 and 2");
            }
        }


        if (!aiConfig.getImage().isEnabled()) {
            recommendations.add("Image generation is disabled - consider enabling for full functionality");
        }


        if (aiConfig.getMemory().isEnabled()) {
            if (aiConfig.getMemory().getMaxConversationHistory() < 10) {
                recommendations.add("Consider increasing max conversation history for better context");
            }
        } else {
            recommendations.add("Memory system is disabled - consider enabling for better conversations");
        }


        if (!aiConfig.getGeneral().getRateLimit().isEnabled()) {
            recommendations.add("Rate limiting is disabled - consider enabling for production use");
        }

        validation.put("valid", issues.isEmpty());
        validation.put("issues", issues);
        validation.put("recommendations", recommendations);
        validation.put("timestamp", System.currentTimeMillis());

        if (issues.isEmpty()) {
            log.info("✅ Configuration validation passed");
            validation.put("status", "All configurations are valid!");
        } else {
            log.warn("⚠️ Configuration validation found issues: {}", issues);
            validation.put("status", "Configuration has issues that need attention");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("validation", validation);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/simple-chat")
    public ResponseEntity<Map<String, Object>> simpleChat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Message is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }


            boolean geminiEnabled = aiConfig.getGemini().isEnabled();
            boolean apiKeyConfigured = aiConfig.getGemini().getApiKey() != null && !aiConfig.getGemini().getApiKey().trim().isEmpty();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuration test successful!");
            response.put("receivedMessage", message);
            response.put("geminiEnabled", geminiEnabled);
            response.put("apiKeyConfigured", apiKeyConfigured);
            response.put("readyForAI", geminiEnabled && apiKeyConfigured);
            response.put("timestamp", System.currentTimeMillis());

            if (geminiEnabled && apiKeyConfigured) {
                response.put("status", "✅ Ready for AI conversations!");
                response.put("nextStep", "Try the full /api/ai/chat endpoint");
            } else {
                response.put("status", "⚠️ Configuration needs attention");
                response.put("nextStep", "Check your Gemini API key configuration");
            }

            log.info("🧪 Simple chat test completed for message: {}", message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Simple chat test failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
