package com.bntech.lexai.gateway.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import com.bntech.lexai.gateway.data.object.Prompts;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    private AnthropicClient client;
    private final ObjectMapper objectMapper;

    @PostConstruct
    private void initializeClient() {
        log.debug("Initializing Anthropic client with API key: {}...",
                apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null");

        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public Mono<SubmitPromptResponse> analyzePrompt(String prompt) {
        log.info("Analyzing prompt: {}", prompt);

        return Mono.fromCallable(() -> {
                    String systemPrompt = Prompts.PL_GENERATE_QUESTIONS;

                    MessageCreateParams params = MessageCreateParams.builder()
                            .maxTokens(4096L)
                            .system(systemPrompt)
                            .addUserMessage(Prompts.PL_USER_MESSAGE + prompt)
                            .model(Model.CLAUDE_3_7_SONNET_LATEST)
                            .temperature(0.3)
                            .build();

                    log.debug("Sending prompt analysis request to Anthropic API");
                    Message message = client.messages().create(params);
                    String jsonResponse = message.content().getFirst()
                            .text()
                            .map(TextBlock::text)
                            .orElseThrow(() -> new IllegalStateException("No text content found in message"));

                    return objectMapper.readValue(jsonResponse, SubmitPromptResponse.class);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(2))
                .retryWhen(buildRetrySpec("prompt analysis"))
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<String> generateDocumentWithAnswers(String title, String originalPrompt,
                                                    Map<String, String> answers, List<String> requirements, List<String> base) {
        log.info("Generating document for: {}", title);

        return Mono.fromCallable(() -> {
                    String jsonData = buildJsonForGeneration(title, originalPrompt, answers, requirements, base);

                    MessageCreateParams params = MessageCreateParams.builder()
                            .maxTokens(8192L)
                            .system(Prompts.PL_GENERATE_DOCUMENT)
                            .addUserMessage(jsonData)
                            .model(Model.CLAUDE_3_7_SONNET_LATEST)
                            .temperature(0.2)
                            .build();

                    log.debug("Sending document generation request to Anthropic API for: {}", title);
                    Message message = client.messages().create(params);
                    log.debug("Received document generation response from Anthropic API for: {}", title);

                    return message.content().getFirst()
                            .text()
                            .map(TextBlock::text)
                            .orElseThrow(() -> new IllegalStateException("No text content found in message"));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(5))
                .retryWhen(buildRetrySpec(title))
                .doOnSuccess(result -> log.info("✅ Document generated for: {}", title))
                .doOnError(error -> log.error("❌ Error generating document for: {}", title, error));
    }

    private String buildJsonForGeneration(String title, String originalPrompt,
                                          Map<String, String> answers, List<String> requirements, List<String> base) {
        try {
            Map<String, Object> jsonData = Map.of(
                    "name", title,
                    "questions", answers != null ? answers : Map.of(),
                    "requirements", requirements != null ? requirements : List.of(),
                    "base", base
            );

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
                    return new RuntimeException("AI service unavailable after " + retrySignal.totalRetries() + " attempts", retrySignal.failure());
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
                || (throwable.getMessage() != null && (
                throwable.getMessage().contains("timeout") ||
                        throwable.getMessage().contains("connection") ||
                        throwable.getMessage().contains("503") ||
                        throwable.getMessage().contains("502") ||
                        throwable.getMessage().contains("rate_limit")
        ));
    }
}
