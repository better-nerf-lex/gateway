package com.bntech.lexai.gateway.service.ai.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProviderProperties {
    private AiModelProvider primaryProvider = AiModelProvider.ANTHROPIC;
    private AiModelProvider fallbackProvider = AiModelProvider.ANTHROPIC;
    private double openaiRolloutRatio = 0.0;
    private OpenAi openai = new OpenAi();

    @Getter
    @Setter
    public static class OpenAi {
        private String apiKey = "dummy-key";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-5.5";
        private String chatModel = "gpt-5.5";
        private String reasoningEffort = "medium";
    }
}
