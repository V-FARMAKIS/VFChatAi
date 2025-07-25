package com.example.VF_ChatAi.ai.provider;

public class ChatMessage {
        private String role;
        private String content;
        private long timestamp;

        public ChatMessage() {}

        public ChatMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }


        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isUserMessage() {
        return false;
    }
}
