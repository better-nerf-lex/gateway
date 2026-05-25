package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DocumentGenerationQualityService {

    private static final int MIN_VISIBLE_TEXT_LENGTH = 350;

    public String cleanHtmlFragment(String content) {
        if (content == null) {
            return "";
        }

        Document parsed = Jsoup.parseBodyFragment(content.strip());
        parsed.select("script, iframe, object, embed, link, meta, title").remove();

        Element body = parsed.body();
        return body != null ? body.html().strip() : content.strip();
    }

    public String validateOrThrow(String content, GenerateDocumentRequest request) {
        List<String> issues = validate(content, request);
        if (!issues.isEmpty()) {
            throw new ServiceException("AIService", "Generated document failed quality checks: "
                    + String.join("; ", issues));
        }
        return content;
    }

    public List<String> validate(String content, GenerateDocumentRequest request) {
        List<String> issues = new ArrayList<>();
        String cleaned = content == null ? "" : content.strip();

        if (cleaned.isBlank()) {
            issues.add("document is empty");
            return issues;
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.contains("<html") || lower.contains("<head") || lower.contains("<body")
                || lower.contains("<!doctype")) {
            issues.add("document contains full-page wrapper tags instead of an HTML fragment");
        }
        if (lower.contains("<style")) {
            issues.add("document contains a style block instead of inline styles");
        }
        if (lower.contains("```")) {
            issues.add("document contains markdown code fences");
        }
        if (containsPlaceholder(cleaned)) {
            issues.add("document contains unresolved placeholders or draft markers");
        }

        Document parsed = Jsoup.parseBodyFragment(cleaned);
        String visibleText = parsed.text().strip();
        if (visibleText.length() < MIN_VISIBLE_TEXT_LENGTH) {
            issues.add("document is too short for a usable legal document");
        }
        if (!hasLegalDocumentStructure(parsed)) {
            issues.add("document lacks basic legal document structure");
        }
        if (!containsTitle(visibleText, request)) {
            issues.add("document does not contain the requested title");
        }
        if (!containsMostAnswers(visibleText, request)) {
            issues.add("document omits too many provided answers");
        }

        return issues;
    }

    public String buildRevisionUserMessage(String originalUserMessage, String failedOutput, List<String> issues) {
        return """
                The previous draft failed internal quality checks.

                Fix the document and return only a clean HTML fragment.
                Keep all factual data from the original request. Do not invent missing facts.
                Use inline styles only. Do not include markdown fences or full-page wrapper tags.

                Quality issues to fix:
                %s

                Original generation input:
                %s

                Failed draft:
                %s
                """.formatted(formatIssues(issues), originalUserMessage, failedOutput);
    }

    private boolean containsPlaceholder(String content) {
        String lower = content.toLowerCase(Locale.ROOT);
        return lower.contains("lorem ipsum")
                || lower.contains("todo")
                || lower.contains("undefined")
                || lower.contains("null")
                || lower.contains("[uzupe")
                || lower.contains("[wpisz")
                || lower.contains("{{")
                || lower.contains("}}")
                || lower.contains("___");
    }

    private boolean hasLegalDocumentStructure(Document document) {
        return !document.select("p, div, section, article").isEmpty()
                && !document.select("h1, h2, h3, strong, b").isEmpty();
    }

    private boolean containsTitle(String visibleText, GenerateDocumentRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return true;
        }
        return normalize(visibleText).contains(normalize(request.getTitle()));
    }

    private boolean containsMostAnswers(String visibleText, GenerateDocumentRequest request) {
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return true;
        }

        List<String> meaningfulAnswers = request.getAnswers().values().stream()
                .filter(value -> value != null && value.strip().length() >= 3)
                .map(String::strip)
                .toList();

        if (meaningfulAnswers.isEmpty()) {
            return true;
        }

        String normalizedText = normalize(visibleText);
        long present = meaningfulAnswers.stream()
                .filter(answer -> normalizedText.contains(normalize(answer)))
                .count();

        int required = Math.max(1, (int) Math.ceil(meaningfulAnswers.size() * 0.75));
        return present >= required;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private String formatIssues(List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "- unknown quality issue";
        }
        return issues.stream()
                .map(issue -> "- " + issue)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- unknown quality issue");
    }
}
