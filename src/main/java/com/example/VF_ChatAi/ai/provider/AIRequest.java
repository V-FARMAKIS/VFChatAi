package com.example.VF_ChatAi.ai.provider;

import java.util.List;

public class AIRequest {
        private String message;
        private String userId;
        private String conversationId;
        private List<ChatMessage> conversationHistory;
        private AIParameters parameters;


        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }

        public List<ChatMessage> getConversationHistory() { return conversationHistory; }
        public void setConversationHistory(List<ChatMessage> conversationHistory) { this.conversationHistory = conversationHistory; }

        public AIParameters getParameters() { return parameters; }
        public void setParameters(AIParameters parameters) { this.parameters = parameters; }

        public static class AIParameters {
            private double temperature;
            private int maxTokens;
            private String systemPrompt;
            private boolean includeHistory;


            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }

            public int getMaxTokens() { return maxTokens; }
            public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

            public String getSystemPrompt() { return systemPrompt; }
            public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

            public boolean isIncludeHistory() { return includeHistory; }
            public void setIncludeHistory(boolean includeHistory) { this.includeHistory = includeHistory; }
        }
    }
