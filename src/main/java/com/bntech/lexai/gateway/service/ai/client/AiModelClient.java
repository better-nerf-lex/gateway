package com.bntech.lexai.gateway.service.ai.client;

public interface AiModelClient {
    AiModelProvider provider();

    String complete(AiPromptRequest request);
}
