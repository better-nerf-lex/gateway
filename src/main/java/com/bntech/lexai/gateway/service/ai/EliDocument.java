package com.bntech.lexai.gateway.service.ai;

import java.util.List;

public record EliDocument(
        String uri,
        String url,
        String originalUrl,
        String pdfUrl,
        String title,
        String status,
        String inForce,
        String changeDate,
        List<String> latestConsolidatedTexts,
        List<String> recentAmendments,
        List<String> textSnippets) {

    public EliDocument(String uri, String url) {
        this(uri, url, url, null, null, null, null, null, List.of(), List.of(), List.of());
    }
}
