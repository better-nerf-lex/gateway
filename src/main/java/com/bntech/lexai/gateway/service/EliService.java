package com.bntech.lexai.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EliService {

    private static final String ELI_BASE_URL = "https://eli.gov.pl";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    public EliService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
                .build();

        this.webClient = webClientBuilder
                .exchangeStrategies(strategies)
                .build();
    }

    public record EliDocument(String uri, String content, boolean found) {}

    public Flux<EliDocument> fetchLegalDocuments(List<String> eliUris) {
        if (eliUris == null || eliUris.isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(eliUris)
                .filter(this::isValidEliUri)
                .flatMap(this::fetchDocument, 3)
                .doOnError(e -> log.error("Error fetching ELI documents", e));
    }

    public Mono<EliDocument> fetchDocument(String eliUri) {
        String url = buildEliUrl(eliUri);
        log.info("Fetching legal document from: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .map(content -> new EliDocument(eliUri, extractRelevantContent(content), true))
                .onErrorResume(e -> {
                    log.warn("Failed to fetch document for URI {}: {}", eliUri, e.getMessage());
                    return Mono.just(new EliDocument(eliUri, "", false));
                });
    }

    private String buildEliUrl(String eliUri) {
        if (eliUri.startsWith("http")) {
            return eliUri.endsWith("/html") ? eliUri : eliUri + "/html";
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

    private boolean isValidEliUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }

        Pattern eliPattern = Pattern.compile(
            "^(https?://eli\\.gov\\.pl)?/eli/[A-Z]+/\\d{4}/\\d+(/[a-z]+)?(/pol)?(/html|/pdf)?$",
            Pattern.CASE_INSENSITIVE
        );

        return eliPattern.matcher(uri).matches() || uri.startsWith("eli/") || uri.startsWith("/eli/");
    }

    private String extractRelevantContent(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return "";
        }

        String content = htmlContent
                .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                .replaceAll("<nav[^>]*>[\\s\\S]*?</nav>", "")
                .replaceAll("<header[^>]*>[\\s\\S]*?</header>", "")
                .replaceAll("<footer[^>]*>[\\s\\S]*?</footer>", "");

        Pattern articlePattern = Pattern.compile("<article[^>]*>([\\s\\S]*?)</article>", Pattern.CASE_INSENSITIVE);
        Matcher articleMatcher = articlePattern.matcher(content);
        if (articleMatcher.find()) {
            content = articleMatcher.group(1);
        }

        Pattern mainPattern = Pattern.compile("<main[^>]*>([\\s\\S]*?)</main>", Pattern.CASE_INSENSITIVE);
        Matcher mainMatcher = mainPattern.matcher(content);
        if (mainMatcher.find()) {
            content = mainMatcher.group(1);
        }

        content = content
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();

        if (content.length() > 15000) {
            content = content.substring(0, 15000) + "...";
        }

        return content;
    }

    public String buildLegalDocumentsContext(List<EliDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        List<EliDocument> foundDocs = documents.stream().filter(d -> d.found() && !d.content().isBlank()).toList();
        List<EliDocument> failedDocs = documents.stream().filter(d -> !d.found()).toList();

        if (!foundDocs.isEmpty()) {
            context.append("\n\n--- ZAŁĄCZONE DOKUMENTY PRAWNE (z eli.gov.pl) ---\n\n");
            for (EliDocument doc : foundDocs) {
                context.append("=== Dokument: ").append(doc.uri()).append(" ===\n");
                context.append(doc.content()).append("\n\n");
            }
        }

        if (!failedDocs.isEmpty()) {
            context.append("\n\n--- UWAGA: DOKUMENTY NIEDOSTĘPNE ---\n");
            context.append("Następujące podstawy prawne nie mogły zostać pobrane z eli.gov.pl.\n");
            context.append("Użyj tych linków jako odniesienia i spróbuj wariantów URL:\n\n");
            for (EliDocument doc : failedDocs) {
                String suggestedUrl = buildEliUrl(doc.uri());
                context.append("- ").append(doc.uri()).append("\n");
                context.append("  Sugerowany link: ").append(suggestedUrl).append("\n");
                context.append("  Alternatywne warianty do sprawdzenia:\n");
                context.append("    - ").append(ELI_BASE_URL).append("/").append(doc.uri()).append("/pol/html\n");
                context.append("    - ").append(ELI_BASE_URL).append("/").append(doc.uri()).append("/pol/pdf\n");
                context.append("    - ").append(ELI_BASE_URL).append("/").append(doc.uri().replace("/ogl", "/tj")).append("/pol/html\n");
            }
            context.append("\nPomimo braku dostępu do pełnych tekstów, uwzględnij te podstawy prawne w dokumencie.\n");
        }

        return context.toString();
    }
}
