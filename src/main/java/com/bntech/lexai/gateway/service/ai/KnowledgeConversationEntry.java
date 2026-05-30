package com.bntech.lexai.gateway.service.ai;

import java.util.List;

public record KnowledgeConversationEntry(
        String id,
        String title,
        String description,
        String language,
        List<String> documentTypes,
        List<String> tags,
        String path) {
}
