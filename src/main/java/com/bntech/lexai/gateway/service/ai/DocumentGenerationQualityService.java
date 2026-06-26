package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentGenerationQualityService {

    private static final int MIN_VISIBLE_TEXT_LENGTH = 350;
    private static final List<String> LEGAL_BASIS_HEADINGS = List.of(
            "podstawa prawna",
            "podstawy prawne",
            "legal basis",
            "legal bases");
    private static final List<String> TRANSCRIPT_ARTIFACTS = List.of(
            "chatgpt",
            "thought for",
            "this is a conversation between",
            "responding will create a branch");
    private static final Set<String> LOW_SIGNAL_ANSWER_VALUES = Set.of(
            "tak",
            "nie",
            "yes",
            "no",
            "brak",
            "none",
            "n/a",
            "nie dotyczy",
            "unknown",
            "nie wiem");
    private static final Set<String> LOW_SIGNAL_WORDS = Set.of(
            "oraz",
            "albo",
            "lub",
            "dla",
            "przez",
            "przy",
            "pod",
            "nad",
            "bez",
            "jest",
            "ma",
            "czy",
            "jak",
            "jaki",
            "jaka",
            "jakie",
            "ktory",
            "ktora",
            "ktore",
            "wniosek",
            "pozew",
            "pismo",
            "dokument");

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
        if (hasBlockingIssues(issues)) {
            throw new ServiceException("AIService", "Generated document failed quality checks: "
                    + String.join("; ", issues));
        }
        return content;
    }

    public boolean hasBlockingIssues(List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return false;
        }
        return issues.stream().anyMatch(issue -> !isNonBlockingIssue(issue));
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
        if (containsLegalBasisLeak(cleaned, request)) {
            issues.add("document includes legal basis metadata that must be stored separately");
        }
        if (containsTranscriptArtifact(cleaned)) {
            issues.add("document includes private conversation transcript artifacts");
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

    public String removeLegalBasisSection(String content, List<String> legalBasis) {
        if (content == null || content.isBlank()) {
            return content == null ? "" : content.strip();
        }

        Document parsed = Jsoup.parseBodyFragment(content);
        Element body = parsed.body();
        if (body == null) {
            return content.strip();
        }

        boolean removedAny = removeTrailingLegalBasisChildren(body, legalBasis);

        return removedAny ? body.html().strip() : content.strip();
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

    private boolean removeTrailingLegalBasisChildren(Element container, List<String> legalBasis) {
        boolean removedAny = false;
        for (Element child : new ArrayList<>(container.children())) {
            removedAny = removeTrailingLegalBasisChildren(child, legalBasis) || removedAny;
        }

        List<Element> children = new ArrayList<>(container.children());
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (!isEmptyListContainer(child) && !isLegalBasisElement(child, legalBasis)) {
                break;
            }
            child.remove();
            removedAny = true;
        }
        return removedAny;
    }

    private boolean isEmptyListContainer(Element element) {
        return element != null
                && ("ul".equals(element.tagName()) || "ol".equals(element.tagName()))
                && element.text().isBlank();
    }

    private boolean containsLegalBasisLeak(String content, GenerateDocumentRequest request) {
        Document parsed = Jsoup.parseBodyFragment(content);
        if (isLegalBasisElement(parsed.body(), request != null ? request.getBase() : List.of())) {
            return true;
        }

        return parsed.select("h1, h2, h3, h4, strong, b, p, div, section, article, li").stream()
                .anyMatch(element -> isLegalBasisElement(element, request != null ? request.getBase() : List.of()));
    }

    private boolean containsTranscriptArtifact(String content) {
        String text = normalize(Jsoup.parseBodyFragment(content).text());
        return TRANSCRIPT_ARTIFACTS.stream().anyMatch(text::contains);
    }

    private boolean isLegalBasisElement(Element element, List<String> legalBasis) {
        if (element == null) {
            return false;
        }

        String text = normalize(element.text());
        if (text.isBlank()) {
            return false;
        }

        boolean hasLegalHeading = LEGAL_BASIS_HEADINGS.stream().anyMatch(text::contains);
        boolean hasEliReference = text.contains("eli/")
                || text.contains("eli.gov.pl")
                || (legalBasis != null && legalBasis.stream()
                        .filter(basis -> basis != null && !basis.isBlank())
                        .map(this::normalize)
                        .anyMatch(text::contains));

        return hasEliReference || startsWithLegalBasisHeading(text, hasLegalHeading);
    }

    private boolean startsWithLegalBasisHeading(String text, boolean hasLegalHeading) {
        if (!hasLegalHeading) {
            return false;
        }
        return LEGAL_BASIS_HEADINGS.stream().anyMatch(heading -> text.startsWith(heading));
    }

    private boolean hasLegalDocumentStructure(Document document) {
        return !document.select("p, div, section, article").isEmpty()
                && !document.select("h1, h2, h3, strong, b").isEmpty();
    }

    private boolean containsTitle(String visibleText, GenerateDocumentRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return true;
        }

        String normalizedText = normalize(visibleText);
        String normalizedTitle = normalize(request.getTitle());
        if (normalizedText.contains(normalizedTitle)) {
            return true;
        }

        String documentOpening = normalizedText.substring(0, Math.min(normalizedText.length(), 700));
        List<String> titleTerms = meaningfulTerms(normalizedTitle);
        if (titleTerms.isEmpty()) {
            return true;
        }

        long present = titleTerms.stream().filter(documentOpening::contains).count();
        int required = Math.max(1, (int) Math.ceil(titleTerms.size() * 0.70));
        return present >= required;
    }

    private boolean containsMostAnswers(String visibleText, GenerateDocumentRequest request) {
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return true;
        }

        List<String> meaningfulAnswers = request.getAnswers().values().stream()
                .filter(value -> value != null && value.strip().length() >= 3)
                .map(String::strip)
                .filter(this::isMeaningfulAnswerForValidation)
                .toList();

        if (meaningfulAnswers.isEmpty()) {
            return true;
        }

        String normalizedText = normalize(visibleText);
        long present = meaningfulAnswers.stream()
                .filter(answer -> answerAppearsInText(answer, normalizedText))
                .count();

        int required = Math.max(1, (int) Math.ceil(meaningfulAnswers.size() * 0.50));
        return present >= required;
    }

    private boolean isMeaningfulAnswerForValidation(String answer) {
        String normalized = normalize(answer);
        if (LOW_SIGNAL_ANSWER_VALUES.contains(normalized)) {
            return false;
        }
        if (normalized.length() > 120) {
            return false;
        }
        return !meaningfulTerms(normalized).isEmpty() || normalized.matches(".*\\d.*");
    }

    private boolean answerAppearsInText(String answer, String normalizedText) {
        String normalizedAnswer = normalize(answer);
        if (normalizedText.contains(normalizedAnswer)) {
            return true;
        }

        String answerDigits = normalizedAnswer.replaceAll("\\D+", "");
        if (answerDigits.length() >= 4 && normalizedText.replaceAll("\\D+", "").contains(answerDigits)) {
            return true;
        }

        List<String> terms = meaningfulTerms(normalizedAnswer);
        if (terms.isEmpty()) {
            return false;
        }

        long present = terms.stream().filter(normalizedText::contains).count();
        int required = Math.max(1, (int) Math.ceil(terms.size() * 0.60));
        return present >= required;
    }

    private List<String> meaningfulTerms(String value) {
        return Arrays.stream(stripDiacritics(value).split("[^a-z0-9]+"))
                .map(String::strip)
                .filter(token -> token.length() >= 3)
                .filter(token -> !LOW_SIGNAL_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return stripDiacritics(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private String stripDiacritics(String value) {
        return java.text.Normalizer.normalize(value != null ? value : "", java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("ł", "l")
                .replace("Ł", "L");
    }

    private boolean isNonBlockingIssue(String issue) {
        return "document does not contain the requested title".equals(issue)
                || "document omits too many provided answers".equals(issue);
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
