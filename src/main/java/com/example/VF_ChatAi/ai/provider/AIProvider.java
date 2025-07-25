package com.example.VF_ChatAi.ai.provider;

public interface AIProvider {
        String getProviderName();
        boolean isEnabled();
        AIResponse generateResponse(AIRequest request) throws AIException;
        boolean isHealthy();
    }
