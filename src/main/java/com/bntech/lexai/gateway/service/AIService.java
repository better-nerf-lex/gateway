package com.bntech.lexai.gateway.service;

//import com.anthropic.client.AnthropicClient;
//import com.anthropic.client.okhttp.AnthropicOkHttpClient;
//import com.anthropic.models.messages.Message;
//import com.anthropic.models.messages.MessageCreateParams;
//import com.anthropic.models.messages.TextBlock;
import com.bntech.lexai.gateway.data.object.Prompts;
import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.LegalBasisResponse;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model}")
    private String model;

    // private AnthropicClient client;
    private final ObjectMapper objectMapper;
    private final EliService eliService;

    @PostConstruct
    private void initializeClient() {
        log.debug("Initializing Anthropic client with API key: {}...",
                apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null");

        // this.client = AnthropicOkHttpClient.builder()
        // .apiKey(apiKey)
        // .build();
    }

    public Mono<SubmitPromptResponse> analyzePrompt(String prompt) {
        log.info("Analyzing prompt: {}", prompt);

        return extractLegalBasis(prompt)
                .flatMap(legalBasisResponse -> verifyLegalBasis(legalBasisResponse.getBase())
                        .flatMap(verifiedBasis -> generateQuestionsWithBasis(prompt, verifiedBasis)));
    }

    public Mono<LegalBasisResponse> extractLegalBasis(String prompt) {
        log.info("Extracting legal basis for prompt: {}", prompt);
        return Mono.empty();
        // return Mono.fromCallable(() -> {
        // MessageCreateParams params = MessageCreateParams.builder()
        // .maxTokens(2048L)
        // .system(Prompts.EN_EXTRACT_LEGAL_BASIS)
        // .addUserMessage(Prompts.EN_USER_MESSAGE + prompt)
        // .model(model)
        // .temperature(0.2)
        // .build();
        //
        // log.debug("Sending legal basis extraction request to Anthropic API");
        // Message message = client.messages().create(params);
        // String jsonResponse = message.content().getFirst()
        // .text()
        // .map(TextBlock::text)
        // .orElseThrow(() -> new IllegalStateException("No text content found in
        // message"));
        //
        // LegalBasisResponse response =
        // objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse),
        // LegalBasisResponse.class);
        // log.info("✅ Extracted {} legal bases: {}", response.getBase().size(),
        // response.getBase());
        // return response;
        // })
        // .subscribeOn(Schedulers.boundedElastic())
        // .timeout(Duration.ofMinutes(1))
        // .retryWhen(buildRetrySpec("legal basis extraction"))
        // .doOnError(error -> log.error("❌ Error extracting legal basis", error));
    }

    public Mono<List<String>> verifyLegalBasis(List<String> bases) {
        if (bases == null || bases.isEmpty()) {
            return Mono.just(List.of());
        }

        log.info("Verifying {} legal bases against ELI API", bases.size());

        return eliService.fetchLegalDocuments(bases)
                .collectList()
                .map(documents -> {
                    List<String> verifiedBases = new ArrayList<>();
                    List<String> failedBases = new ArrayList<>();

                    for (EliService.EliDocument doc : documents) {
                        if (doc.found()) {
                            verifiedBases.add(doc.uri());
                        } else {
                            failedBases.add(doc.uri());
                        }
                    }

                    if (!failedBases.isEmpty()) {
                        log.warn("Failed to verify {} legal bases: {}", failedBases.size(), failedBases);
                    }
                    if (!verifiedBases.isEmpty()) {
                        log.info("✅ Verified {} legal bases: {}", verifiedBases.size(), verifiedBases);
                    }

                    return verifiedBases.isEmpty() ? bases : verifiedBases;
                })
                .onErrorResume(e -> {
                    log.warn("ELI API verification failed, using unverified bases: {}", e.getMessage());
                    return Mono.just(bases);
                });
    }

    private Mono<SubmitPromptResponse> generateQuestionsWithBasis(String prompt, List<String> verifiedBasis) {
        log.info("Generating questions with verified legal basis: {}", verifiedBasis);
        return Mono.empty();

        // return Mono.fromCallable(() -> {
        // String basisJson;
        // try {
        // basisJson = objectMapper.writeValueAsString(verifiedBasis);
        // } catch (Exception e) {
        // basisJson = "[]";
        // }
        //
        // String systemPrompt = String.format(Prompts.EN_GENERATE_QUESTIONS, basisJson,
        // basisJson);
        //
        // MessageCreateParams params = MessageCreateParams.builder()
        // .maxTokens(4096L)
        // .system(systemPrompt)
        // .addUserMessage(Prompts.EN_USER_MESSAGE + prompt)
        // .model(model)
        // .temperature(0.3)
        // .build();
        //
        // log.debug("Sending prompt analysis request to Anthropic API");
        // Message message = client.messages().create(params);
        // String jsonResponse = message.content().getFirst()
        // .text()
        // .map(TextBlock::text)
        // .orElseThrow(() -> new IllegalStateException("No text content found in
        // message"));
        //
        // SubmitPromptResponse response =
        // objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse),
        // SubmitPromptResponse.class);
        // log.info("✅ Parsed response with {} questions",
        // response.getQuestions().size());
        // return response;
        // })
        // .subscribeOn(Schedulers.boundedElastic())
        // .timeout(Duration.ofMinutes(2))
        // .retryWhen(buildRetrySpec("prompt analysis"))
        // .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<String> generateDocumentWithAnswers(GenerateDocumentRequest request) {
        return generateDocumentWithAnswers(
                request.getTitle(),
                request.getPrompt(),
                request.getAnswers(),
                request.getRequirements(),
                request.getBase());
    }

    public Mono<String> generateDocumentWithAnswers(String title, String originalPrompt,
            Map<String, String> answers, List<String> requirements, List<String> base) {
        log.info("Generating document for: {}", title);
        return Mono.just("");
        // return fetchEliDocumentsContext(base)
        // .flatMap(legalContext -> {
        // String jsonData = buildJsonForGeneration(title, originalPrompt, answers,
        // requirements, base);
        // String userMessage = jsonData + legalContext;
        //
        // return Mono.fromCallable(() -> {
        // MessageCreateParams params = MessageCreateParams.builder()
        // .maxTokens(8192L)
        // .system(Prompts.EN_GENERATE_DOCUMENT)
        // .addUserMessage(userMessage)
        // .model(model)
        // .temperature(0.2)
        // .build();
        //
        // log.debug("Sending document generation request to Anthropic API for: {}",
        // title);
        // Message message = client.messages().create(params);
        // log.debug("Received document generation response from Anthropic API for: {}",
        // title);
        //
        // String rawContent = message.content().getFirst()
        // .text()
        // .map(TextBlock::text)
        // .orElseThrow(() -> new IllegalStateException("No text content found in
        // message"));
        // return stripMarkdownCodeBlocks(rawContent);
        // }).subscribeOn(Schedulers.boundedElastic());
        // })
        // .timeout(Duration.ofMinutes(5))
        // .retryWhen(buildRetrySpec(title))
        // .doOnSuccess(result -> log.info("✅ Document generated for: {}", title))
        // .doOnError(error -> log.error("❌ Error generating document for: {}", title,
        // error));
    }

    private Mono<String> fetchEliDocumentsContext(List<String> base) {
        if (base == null || base.isEmpty()) {
            return Mono.just("");
        }

        log.info("Fetching ELI documents for legal bases: {}", base);

        return eliService.fetchLegalDocuments(base)
                .collectList()
                .map(eliService::buildLegalDocumentsContext)
                .doOnSuccess(context -> {
                    if (!context.isBlank()) {
                        log.info("✅ Fetched {} legal documents from eli.gov.pl",
                                base.stream().filter(uri -> context.contains(uri)).count());
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Failed to fetch ELI documents, proceeding without legal context: {}", e.getMessage());
                    return Mono.just("");
                });
    }

    private String buildJsonForGeneration(String title, String originalPrompt,
            Map<String, String> answers, List<String> requirements, List<String> base) {
        try {
            Map<String, Object> jsonData = Map.of(
                    "name", title,
                    "questions", answers != null ? answers : Map.of(),
                    "requirements", requirements != null ? requirements : List.of(),
                    "base", base);

            return objectMapper.writeValueAsString(jsonData);
        } catch (Exception e) {
            log.error("Error building JSON for generation", e);
            return "{}";
        }
    }

    private Retry buildRetrySpec(String identifier) {
        return Retry.backoff(3, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(15))
                .jitter(0.2)
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    Throwable exception = retrySignal.failure();
                    log.warn("Retrying AI call for {} (attempt {}) due to: {}",
                            identifier, retrySignal.totalRetries() + 1, exception.getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Retry exhausted for {} after {} attempts", identifier, retrySignal.totalRetries());
                    return new ServiceException("AIService",
                            "AI service unavailable after " + retrySignal.totalRetries() + " attempts",
                            retrySignal.failure());
                });
    }

    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof ResourceAccessException
                || throwable instanceof TimeoutException
                || throwable instanceof java.io.IOException
                || (throwable.getCause() instanceof io.netty.handler.timeout.ReadTimeoutException)
                || (throwable.getCause() instanceof io.netty.handler.timeout.WriteTimeoutException)
                || (throwable.getCause() instanceof java.net.SocketTimeoutException)
                || (throwable.getCause() instanceof java.net.ConnectException)
                || (throwable.getMessage() != null && (throwable.getMessage().contains("timeout") ||
                        throwable.getMessage().contains("connection") ||
                        throwable.getMessage().contains("503") ||
                        throwable.getMessage().contains("502") ||
                        throwable.getMessage().contains("rate_limit")));
    }

    private String stripMarkdownCodeBlocks(String response) {
        if (response == null) {
            return null;
        }
        String stripped = response.strip();
        if (stripped.startsWith("```")) {
            int firstNewline = stripped.indexOf('\n');
            if (firstNewline > 0) {
                stripped = stripped.substring(firstNewline + 1);
            } else {
                stripped = stripped.substring(3);
            }
        }
        if (stripped.endsWith("```")) {
            stripped = stripped.substring(0, stripped.length() - 3);
        }
        return stripped.strip();
    }
}
