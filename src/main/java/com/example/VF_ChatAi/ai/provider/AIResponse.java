package com.example.VF_ChatAi.ai.provider;

public class AIResponse {
        private String response;
        private String provider;
        private String model;
        private long responseTimeMs;
        private int tokensUsed;
        private String conversationId;
        private boolean success;
        private String errorMessage;

        private AIResponse(Builder builder) {
            this.response = builder.response;
            this.provider = builder.provider;
            this.model = builder.model;
            this.responseTimeMs = builder.responseTimeMs;
            this.tokensUsed = builder.tokensUsed;
            this.conversationId = builder.conversationId;
            this.success = builder.success;
            this.errorMessage = builder.errorMessage;
        }

        public static Builder builder() {
            return new Builder();
        }


        public String getResponse() { return response; }
        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public int getTokensUsed() { return tokensUsed; }
        public String getConversationId() { return conversationId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }

        public static class Builder {
            private String response;
            private String provider;
            private String model;
            private long responseTimeMs;
            private int tokensUsed;
            private String conversationId;
            private boolean success;
            private String errorMessage;

            public Builder response(String response) { this.response = response; return this; }
            public Builder provider(String provider) { this.provider = provider; return this; }
            public Builder model(String model) { this.model = model; return this; }
            public Builder responseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; return this; }
            public Builder tokensUsed(int tokensUsed) { this.tokensUsed = tokensUsed; return this; }
            public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
            public Builder success(boolean success) { this.success = success; return this; }
            public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

            public AIResponse build() {
                return new AIResponse(this);
            }
        }
    }
