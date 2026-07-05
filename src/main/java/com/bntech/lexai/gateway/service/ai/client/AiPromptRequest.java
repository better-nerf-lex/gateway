package com.bntech.lexai.gateway.service.ai.client;

import com.bntech.lexai.gateway.service.ai.FileContent;

import java.util.List;

public record AiPromptRequest(
        AiCallKind kind,
        String title,
        String systemPrompt,
        String userMessage,
        String model,
        Double temperature,
        long maxTokens,
        String reasoningEffort,
        List<FileContent> templates,
        List<FileContent> contextFiles) {

    public AiPromptRequest {
        templates = templates != null ? templates : List.of();
        contextFiles = contextFiles != null ? contextFiles : List.of();
    }
}
