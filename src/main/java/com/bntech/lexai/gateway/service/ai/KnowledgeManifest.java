package com.bntech.lexai.gateway.service.ai;

import java.util.List;

public record KnowledgeManifest(int version, String updatedAt, List<KnowledgeConversationEntry> conversations) {
}
