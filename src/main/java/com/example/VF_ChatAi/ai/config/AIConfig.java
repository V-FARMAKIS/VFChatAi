package com.example.VF_ChatAi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ai")
@Component
public class AIConfig {
    private Gemini gemini = new Gemini();
    private Image image = new Image();
    private General general = new General();
    private Memory memory = new Memory();
    private Bots bots = new Bots();


    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }

    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }

    public General getGeneral() { return general; }
    public void setGeneral(General general) { this.general = general; }

    public Memory getMemory() { return memory; }
    public void setMemory(Memory memory) { this.memory = memory; }

    public Bots getBots() { return bots; }
    public void setBots(Bots bots) { this.bots = bots; }

    public static class Gemini {
        private boolean enabled = true;
        private String apiKey;
        private String model = "gemini-1.5-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private double temperature = 0.7;
        private int maxTokens = 2048;
        private int timeoutSeconds = 30;


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class Image {
        private boolean enabled = true;
        private String apiKey = "none-required";
        private String model = "pollinations";
        private String baseUrl = "https://image.pollinations.ai/prompt";
        private String saveDirectory = "static/generated-images";
        private String maxSize = "10MB";


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getSaveDirectory() { return saveDirectory; }
        public void setSaveDirectory(String saveDirectory) { this.saveDirectory = saveDirectory; }

        public String getMaxSize() { return maxSize; }
        public void setMaxSize(String maxSize) { this.maxSize = maxSize; }
    }

    public static class General {
        private String defaultProvider = "gemini";
        private int timeoutSeconds = 30;
        private int maxPromptLength = 32000;
        private int retryAttempts = 3;
        private boolean enableFallback = true;
        private RateLimit rateLimit = new RateLimit();


        public String getDefaultProvider() { return defaultProvider; }
        public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public int getMaxPromptLength() { return maxPromptLength; }
        public void setMaxPromptLength(int maxPromptLength) { this.maxPromptLength = maxPromptLength; }

        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

        public boolean isEnableFallback() { return enableFallback; }
        public void setEnableFallback(boolean enableFallback) { this.enableFallback = enableFallback; }

        public RateLimit getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

        public static class RateLimit {
            private boolean enabled = true;
            private int requestsPerMinute = 60;
            private int requestsPerHour = 1000;
            private int burstCapacity = 10;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public int getRequestsPerMinute() { return requestsPerMinute; }
            public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }

            public int getRequestsPerHour() { return requestsPerHour; }
            public void setRequestsPerHour(int requestsPerHour) { this.requestsPerHour = requestsPerHour; }

            public int getBurstCapacity() { return burstCapacity; }
            public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }
        }
    }

    public static class Memory {
        private boolean enabled = true;
        private int maxConversationHistory = 100;
        private int summaryThreshold = 20;
        private int maxMemoryEntries = 50;
        private int contextWindow = 8000;
        private int cleanupInterval = 1;


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMaxConversationHistory() { return maxConversationHistory; }
        public void setMaxConversationHistory(int maxConversationHistory) { this.maxConversationHistory = maxConversationHistory; }

        public int getSummaryThreshold() { return summaryThreshold; }
        public void setSummaryThreshold(int summaryThreshold) { this.summaryThreshold = summaryThreshold; }

        public int getMaxMemoryEntries() { return maxMemoryEntries; }
        public void setMaxMemoryEntries(int maxMemoryEntries) { this.maxMemoryEntries = maxMemoryEntries; }

        public int getContextWindow() { return contextWindow; }
        public void setContextWindow(int contextWindow) { this.contextWindow = contextWindow; }

        public int getCleanupInterval() { return cleanupInterval; }
        public void setCleanupInterval(int cleanupInterval) { this.cleanupInterval = cleanupInterval; }
    }

    public static class Bots {
        private boolean enabled = true;
        private CodeAssistant codeAssistant = new CodeAssistant();
        private WritingCoach writingCoach = new WritingCoach();
        private CreativeDirector creativeDirector = new CreativeDirector();
        private DataAnalyst dataAnalyst = new DataAnalyst();
        private Tutor tutor = new Tutor();
        private GeneralBot general = new GeneralBot();


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public CodeAssistant getCodeAssistant() { return codeAssistant; }
        public void setCodeAssistant(CodeAssistant codeAssistant) { this.codeAssistant = codeAssistant; }

        public WritingCoach getWritingCoach() { return writingCoach; }
        public void setWritingCoach(WritingCoach writingCoach) { this.writingCoach = writingCoach; }

        public CreativeDirector getCreativeDirector() { return creativeDirector; }
        public void setCreativeDirector(CreativeDirector creativeDirector) { this.creativeDirector = creativeDirector; }

        public DataAnalyst getDataAnalyst() { return dataAnalyst; }
        public void setDataAnalyst(DataAnalyst dataAnalyst) { this.dataAnalyst = dataAnalyst; }

        public Tutor getTutor() { return tutor; }
        public void setTutor(Tutor tutor) { this.tutor = tutor; }

        public GeneralBot getGeneral() { return general; }
        public void setGeneral(GeneralBot general) { this.general = general; }

        public static class CodeAssistant {
            private boolean enabled = true;
            private double temperature = 0.3;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public static class WritingCoach {
            private boolean enabled = true;
            private double temperature = 0.7;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public static class CreativeDirector {
            private boolean enabled = true;
            private double temperature = 0.8;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public static class DataAnalyst {
            private boolean enabled = true;
            private double temperature = 0.4;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public static class Tutor {
            private boolean enabled = true;
            private double temperature = 0.6;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }

        public static class GeneralBot {
            private boolean enabled = true;
            private double temperature = 0.7;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
        }
    }
}