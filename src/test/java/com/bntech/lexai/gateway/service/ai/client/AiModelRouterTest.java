package com.bntech.lexai.gateway.service.ai.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelRouterTest {
    @Test
    void routesAllTrafficToOpenAiWhenRolloutIsOne() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setPrimaryProvider(AiModelProvider.ANTHROPIC);
        properties.setOpenaiRolloutRatio(1.0);

        CountingClient anthropic = new CountingClient(AiModelProvider.ANTHROPIC, "anthropic");
        CountingClient openai = new CountingClient(AiModelProvider.OPENAI, "openai");
        AiModelRouter router = new AiModelRouter(properties, List.of(anthropic, openai));

        String result = router.complete(request());

        assertThat(result).isEqualTo("openai");
        assertThat(openai.calls).isEqualTo(1);
        assertThat(anthropic.calls).isZero();
    }

    @Test
    void fallsBackToAnthropicWhenOpenAiFails() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setFallbackProvider(AiModelProvider.ANTHROPIC);
        properties.setOpenaiRolloutRatio(1.0);

        CountingClient anthropic = new CountingClient(AiModelProvider.ANTHROPIC, "anthropic");
        AiModelClient openai = new AiModelClient() {
            @Override
            public AiModelProvider provider() {
                return AiModelProvider.OPENAI;
            }

            @Override
            public String complete(AiPromptRequest request) {
                throw new IllegalStateException("boom");
            }
        };

        AiModelRouter router = new AiModelRouter(properties, List.of(openai, anthropic));

        assertThat(router.complete(request())).isEqualTo("anthropic");
        assertThat(anthropic.calls).isEqualTo(1);
    }

    private AiPromptRequest request() {
        return new AiPromptRequest(
                AiCallKind.QUESTIONS,
                "title",
                "system",
                "user",
                null,
                0.2,
                1000L,
                "medium",
                List.of(),
                List.of());
    }

    private static class CountingClient implements AiModelClient {
        private final AiModelProvider provider;
        private final String response;
        private int calls;

        private CountingClient(AiModelProvider provider, String response) {
            this.provider = provider;
            this.response = response;
        }

        @Override
        public AiModelProvider provider() {
            return provider;
        }

        @Override
        public String complete(AiPromptRequest request) {
            calls++;
            return response;
        }
    }
}
