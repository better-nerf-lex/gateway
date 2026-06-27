package com.bntech.lexai.gateway.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class EliService {

    private static final String ELI_BASE_URL = "https://eli.gov.pl";
    private static final int MAX_DOCUMENTS = 5;
    private static final int MAX_SEARCH_TERMS = 16;
    private static final int MAX_SNIPPETS_PER_DOCUMENT = 4;
    private static final int SNIPPET_RADIUS = 650;

    private final WebClient webClient;

    public EliService() {
        this(WebClient.builder()
                .baseUrl(ELI_BASE_URL)
                .build());
    }

    EliService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<EliDocument> fetchLegalDocuments(List<String> eliUris) {
        return fetchLegalDocuments(eliUris, List.of());
    }

    public Flux<EliDocument> fetchLegalDocuments(List<String> eliUris, List<String> searchTerms) {
        if (eliUris == null || eliUris.isEmpty()) {
            return Flux.empty();
        }

        List<String> normalizedTerms = normalizeSearchTerms(searchTerms);
        log.info("Resolving {} ELI legal document(s) with {} research term(s)",
                eliUris.size(), normalizedTerms.size());

        return Flux.fromIterable(eliUris)
                .filter(this::isValidEliUri)
                .distinct()
                .take(MAX_DOCUMENTS)
                .flatMap(uri -> resolveLegalDocument(uri, normalizedTerms), 3)
                .doOnComplete(() -> log.info("Resolved ELI legal document context"));
    }

    private Mono<EliDocument> resolveLegalDocument(String uri, List<String> searchTerms) {
        EliActId actId = parseEliActId(uri);
        if (actId == null) {
            return Mono.just(new EliDocument(uri, buildFallbackUrl(uri)));
        }

        String originalUrl = buildOriginalTextUrl(actId);
        String metadataPath = buildMetadataPath(actId);

        return fetchMetadata(metadataPath)
                .flatMap(metadata -> fetchCurrentTextSnippets(actId, metadata, searchTerms)
                        .defaultIfEmpty(List.of())
                        .map(snippets -> buildDocument(uri, actId, metadata, originalUrl, snippets)))
                .timeout(Duration.ofSeconds(8))
                .onErrorResume(error -> {
                    log.warn("Failed to resolve ELI metadata for {}: {}", uri, error.getMessage());
                    return Mono.just(new EliDocument(uri, originalUrl));
                });
    }

    private Mono<JsonNode> fetchMetadata(String metadataPath) {
        return webClient.get()
                .uri(metadataPath)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    private Mono<List<String>> fetchCurrentTextSnippets(EliActId actId, JsonNode metadata, List<String> searchTerms) {
        if (searchTerms == null || searchTerms.isEmpty() || !metadata.path("textHTML").asBoolean(false)) {
            return Mono.just(List.of());
        }

        return webClient.get()
                .uri(buildCurrentHtmlPath(actId))
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(8))
                .map(html -> extractRelevantSnippets(html, searchTerms))
                .onErrorResume(error -> {
                    log.warn("Failed to fetch current ELI text snippets for {}/{}/{}: {}",
                            actId.publisher(), actId.year(), actId.position(), error.getMessage());
                    return Mono.just(List.of());
                });
    }

    private EliDocument buildDocument(String uri, EliActId actId, JsonNode metadata,
            String originalUrl, List<String> snippets) {
        String currentHtmlUrl = ELI_BASE_URL + buildCurrentHtmlPath(actId);
        boolean hasCurrentHtml = metadata.path("textHTML").asBoolean(false);
        boolean hasUnifiedText = hasUnifiedText(metadata);
        String currentPdfUrl = metadata.path("textPDF").asBoolean(false) && hasUnifiedText
                ? buildCurrentPdfUrl(actId)
                : null;
        String preferredUrl = hasCurrentHtml
                ? currentHtmlUrl
                : currentPdfUrl != null ? currentPdfUrl : originalUrl;

        return new EliDocument(
                uri,
                preferredUrl,
                originalUrl,
                currentPdfUrl,
                text(metadata, "title"),
                text(metadata, "status"),
                text(metadata, "inForce"),
                text(metadata, "changeDate"),
                latestConsolidatedTexts(metadata),
                recentAmendments(metadata),
                snippets);
    }

    private boolean hasUnifiedText(JsonNode metadata) {
        String status = text(metadata, "status");
        return (status != null && status.toLowerCase(Locale.ROOT).contains("tekst jednolity"))
                || metadata.path("references").path("Inf. o tekście jednolitym").isArray();
    }

    private List<String> extractRelevantSnippets(String html, List<String> searchTerms) {
        String text = Jsoup.parse(html).text();
        String normalizedText = normalizeForPosition(text);
        LinkedHashSet<String> snippets = new LinkedHashSet<>();

        for (String term : searchTerms) {
            if (snippets.size() >= MAX_SNIPPETS_PER_DOCUMENT) {
                break;
            }
            String normalizedTerm = normalize(term);
            if (normalizedTerm.length() < 4) {
                continue;
            }

            int index = normalizedText.indexOf(normalizedTerm);
            if (index < 0) {
                continue;
            }

            snippets.add(snippet(text, index));
        }

        return new ArrayList<>(snippets);
    }

    private String snippet(String text, int normalizedIndex) {
        int start = Math.max(0, normalizedIndex - SNIPPET_RADIUS);
        int end = Math.min(text.length(), normalizedIndex + SNIPPET_RADIUS);

        while (start > 0 && !Character.isWhitespace(text.charAt(start))) {
            start--;
        }
        while (end < text.length() && !Character.isWhitespace(text.charAt(end - 1))) {
            end++;
        }

        return text.substring(start, end)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> normalizeSearchTerms(List<String> searchTerms) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (searchTerms != null) {
            searchTerms.stream()
                    .filter(term -> term != null && !term.isBlank())
                    .map(String::trim)
                    .forEach(term -> addSearchTerm(terms, term));
        }

        return terms.stream()
                .filter(term -> normalize(term).length() >= 4)
                .limit(MAX_SEARCH_TERMS)
                .toList();
    }

    private void addSearchTerm(LinkedHashSet<String> terms, String term) {
        terms.add(term);

        String normalized = normalize(term);
        if (normalized.contains("sde")) {
            terms.add("system dozoru elektronicznego");
            terms.add("systemu dozoru elektronicznego");
            terms.add("dozór elektroniczny");
            terms.add("dozoru elektronicznego");
        }
        if (normalized.contains("odroczenie") && normalized.contains("kary")) {
            terms.add("odroczenie wykonania kary");
            terms.add("wykonania kary pozbawienia wolności");
        }
        if (normalized.contains("aliment")) {
            terms.add("obowiązek alimentacyjny");
            terms.add("świadczenia alimentacyjne");
        }
    }

    private List<String> latestConsolidatedTexts(JsonNode metadata) {
        JsonNode consolidated = metadata.path("references").path("Inf. o tekście jednolitym");
        if (!consolidated.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(consolidated.spliterator(), false)
                .map(node -> text(node, "id"))
                .filter(value -> value != null && !value.isBlank())
                .limit(5)
                .toList();
    }

    private List<String> recentAmendments(JsonNode metadata) {
        JsonNode amendments = metadata.path("references").path("Akty zmieniające");
        if (!amendments.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(amendments.spliterator(), false)
                .map(node -> new AmendmentRef(text(node, "id"), text(node, "date")))
                .filter(ref -> ref.id() != null && !ref.id().isBlank())
                .sorted(Comparator.comparing(
                        AmendmentRef::date,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(ref -> ref.date() == null || ref.date().isBlank()
                        ? ref.id()
                        : ref.id() + " (od " + ref.date() + ")")
                .toList();
    }

    private String buildOriginalTextUrl(EliActId actId) {
        String statusCode = "ogl".equals(actId.status()) ? "I" : "U";
        return ELI_BASE_URL + "/api/acts/%s/%s/%s/text/%s/%s".formatted(
                actId.publisher(), actId.year(), actId.position(), statusCode, buildPdfFilename(actId, false));
    }

    private String buildCurrentPdfUrl(EliActId actId) {
        return ELI_BASE_URL + "/api/acts/%s/%s/%s/text/U/%s".formatted(
                actId.publisher(), actId.year(), actId.position(), buildPdfFilename(actId, true));
    }

    private String buildFallbackUrl(String eliUri) {
        if (eliUri.startsWith("http")) {
            return eliUri;
        }

        EliActId actId = parseEliActId(eliUri);
        if (actId != null) {
            return buildOriginalTextUrl(actId);
        }

        String cleanUri = eliUri.startsWith("/") ? eliUri : "/" + eliUri;
        if (!cleanUri.contains("/pol/")) {
            cleanUri = cleanUri + "/pol";
        }
        if (!cleanUri.endsWith("/html") && !cleanUri.endsWith("/pdf")) {
            cleanUri = cleanUri + "/html";
        }

        return ELI_BASE_URL + cleanUri;
    }

    private String buildMetadataPath(EliActId actId) {
        return "/api/acts/%s/%s/%s".formatted(actId.publisher(), actId.year(), actId.position());
    }

    private String buildCurrentHtmlPath(EliActId actId) {
        return "/api/acts/%s/%s/%s/text.html".formatted(actId.publisher(), actId.year(), actId.position());
    }

    private String buildPdfFilename(EliActId actId, boolean currentUnifiedText) {
        String journalLetter = "MP".equals(actId.publisher()) ? "M" : "D";
        String paddedPosition = actId.position().matches("\\d+")
                ? String.format("%04d", Integer.parseInt(actId.position()))
                : actId.position();
        String suffix = currentUnifiedText ? "Lj" : "";
        return "%s%s%s%s.pdf".formatted(journalLetter, actId.year(), paddedPosition, suffix);
    }

    private EliActId parseEliActId(String uri) {
        if (uri == null || uri.isBlank() || uri.startsWith("http")) {
            return null;
        }

        try {
            String cleanUri = uri.startsWith("/") ? uri.substring(1) : uri;
            String[] parts = cleanUri.split("/");
            if (parts.length >= 4 && parts[0].equalsIgnoreCase("eli")) {
                return new EliActId(
                        parts[1].toUpperCase(Locale.ROOT),
                        parts[2],
                        parts[3],
                        parts.length > 4 ? parts[4].toLowerCase(Locale.ROOT) : "ogl");
            }
        } catch (Exception e) {
            log.debug("Could not parse ELI URI {}", uri, e);
        }
        return null;
    }

    private boolean isValidEliUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }

        Pattern eliPattern = Pattern.compile(
                "^(https?://eli\\.gov\\.pl)?/(eli|api/acts)/[A-Z]+/\\d{4}/\\d+.*$",
                Pattern.CASE_INSENSITIVE);

        return eliPattern.matcher(uri).matches() || uri.startsWith("eli/") || uri.startsWith("/eli/");
    }

    public String buildLegalDocumentsContext(List<EliDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n--- AKTUALNE ŹRÓDŁA PRAWA Z ELI (Current Legal Sources) ---\n\n");
        context.append("The following ELI acts were resolved through eli.gov.pl before drafting.\n");
        context.append("Treat the current text link, status, change date, consolidated-text references, recent amendments, ");
        context.append("and snippets as authoritative freshness context. Do not rely only on the original publication year.\n\n");

        for (EliDocument doc : documents) {
            context.append("- ELI identifier: ").append(doc.uri()).append("\n");
            appendLine(context, "  Title", doc.title());
            appendLine(context, "  Status", doc.status());
            appendLine(context, "  In force", doc.inForce());
            appendLine(context, "  Last ELI metadata update", doc.changeDate());
            context.append("  Current text HTML: ").append(doc.url()).append("\n");
            appendLine(context, "  Current consolidated PDF", doc.pdfUrl());
            appendLine(context, "  Original publication text", doc.originalUrl());
            appendList(context, "  Latest consolidated text notices", doc.latestConsolidatedTexts());
            appendList(context, "  Recent amendments by effective date", doc.recentAmendments());
            appendList(context, "  Current text snippets selected for this document", doc.textSnippets());
            context.append("\n");
        }

        context.append("Drafting instruction: use these sources to choose current, specific article references when useful. ");
        context.append("If a code was originally published years ago but ELI reports later amendments or unified text, ");
        context.append("prefer the current text and mention only article/paragraph references integrated into ordinary legal prose.\n");

        return context.toString();
    }

    private void appendLine(StringBuilder context, String label, String value) {
        if (value != null && !value.isBlank()) {
            context.append(label).append(": ").append(value).append("\n");
        }
    }

    private void appendList(StringBuilder context, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        context.append(label).append(":\n");
        for (String value : values) {
            context.append("    - ").append(value).append("\n");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ąćęłńóśźż\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String normalizeForPosition(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ąćęłńóśźż\\s]", " ");
    }

    private record EliActId(String publisher, String year, String position, String status) {
    }

    private record AmendmentRef(String id, String date) {
    }
}
