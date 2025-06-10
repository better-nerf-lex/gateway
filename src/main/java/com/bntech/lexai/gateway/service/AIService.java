
package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.object.DocumentType;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final AnthropicChatModel anthropicChatModel;

    public Mono<String> generateDocument(String title, DocumentType type, String userPrompt, List<LegalEntityDto> people) {
        log.info("Generating {} document with title: {}", type, title);

        return Mono.fromCallable(() -> {
                    String systemPrompt = buildSystemPrompt(type);
                    String formattedUserPrompt = buildUserPrompt(title, userPrompt, people);

                    List<Message> messages = List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(formattedUserPrompt)
                    );

                    Prompt prompt = new Prompt(messages);
                    ChatResponse response = anthropicChatModel.call(prompt);
                    log.debug("Generated response for {}: {}", title, response.getResult().getOutput().toString());
                    return response.getResult().getOutput().toString();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(5)) // Reduced timeout
                .retryWhen(buildRetrySpec(title))
                .doOnSuccess(result -> log.info("✅ Document generated successfully for: {}", title))
                .doOnError(error -> log.error("❌ Error generating document for: {}", title, error));
    }

    public Mono<String> generateDocumentWithTemplate(String title, DocumentType type, String userPrompt, List<LegalEntityDto> people) {
        log.info("Generating {} document with template for: {}", type, title);

        return Mono.fromCallable(() -> {
                    String templateText = getTemplateForDocumentType(type);
                    PromptTemplate promptTemplate = new PromptTemplate(templateText);
                    Map<String, Object> variables = Map.of(
                            "title", title,
                            "userPrompt", userPrompt,
                            "people", formatPeopleForTemplate(people),
                            "documentType", type.getValue()
                    );

                    // More conservative options
                    Prompt prompt = promptTemplate.create(variables, AnthropicChatOptions.builder()
                            .maxTokens(3000)  // Reduced from 4096
                            .model("claude-3-7-sonnet-20250219")
                            .temperature(0.2)
                            .build());

                    log.debug("Sending request to Anthropic API for: {}", title);
                    ChatResponse response = anthropicChatModel.call(prompt);
                    log.debug("Received response from Anthropic API for: {}", title);
                    return response.getResult().getOutput().toString();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(5)) // Consistent timeout
                .retryWhen(buildRetrySpec(title))
                .doOnSuccess(result -> log.info("✅ Document generated with template for: {}", title))
                .doOnError(error -> log.error("❌ Error generating document with template for: {}", title, error));
    }

    public Mono<String> streamGenerateDocument(String title, DocumentType type, String userPrompt, List<LegalEntityDto> people) {
        log.info("🚀 Streaming generation of {} document: {}", type, title);

        return Mono.fromCallable(() -> {
                    String systemPrompt = buildSystemPrompt(type);
                    String formattedUserPrompt = buildUserPrompt(title, userPrompt, people);

                    List<Message> messages = List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(formattedUserPrompt)
                    );

                    Prompt prompt = new Prompt(messages);
                    return prompt;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(prompt ->
                        // Convert Flux to Mono by collecting all chunks
                        anthropicChatModel.stream(prompt)
                                .map(chatResponse -> chatResponse.getResult().getOutput().toString())
                                .collect(Collectors.joining())
                                .timeout(Duration.ofMinutes(5))
                                .retryWhen(buildRetrySpec(title))
                )
                .doOnSuccess(result -> log.info("✅ Streaming document generation completed for: {}", title))
                .doOnError(error -> log.error("❌ Error in streaming document generation for: {}", title, error));
    }

    private Retry buildRetrySpec(String title) {
        return Retry.backoff(3, Duration.ofSeconds(2)) // Reduced retries and backoff
                .maxBackoff(Duration.ofSeconds(15))
                .jitter(0.2) // Increased jitter
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    Throwable exception = retrySignal.failure();
                    log.warn("Retrying AI call for document: {} (attempt {}) due to: {}",
                            title, retrySignal.totalRetries() + 1, exception.getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Retry exhausted for document: {} after {} attempts", title, retrySignal.totalRetries());
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
                        throwable.getMessage().contains("502")
        ));
    }

    // ... rest of your methods remain the same
    private String buildSystemPrompt(DocumentType type) {
        return switch (type) {
            case CONTRACT -> """
                    You are an expert legal contract writer. Your task is to create professional, 
                    legally sound contracts that are clear, comprehensive, and enforceable. 
                    Include all necessary clauses, terms, and conditions. Use formal legal language 
                    and ensure all parties' rights and obligations are clearly defined.
                    """;
            case REPORT -> """
                    You are a professional report writer specializing in comprehensive business and 
                    legal analysis. Create detailed, well-structured reports with clear sections, 
                    executive summaries, findings, and recommendations. Use professional formatting 
                    and maintain objectivity throughout.
                    """;
            case PROPOSAL -> """
                    You are an expert proposal writer who creates compelling, professional business 
                    proposals. Structure your proposals with clear value propositions, detailed 
                    solutions, timelines, and cost breakdowns. Make the proposal persuasive yet 
                    professional and factual.
                    """;
        };
    }

    private String buildUserPrompt(String title, String userPrompt, List<LegalEntityDto> people) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Document Title: ").append(title).append("\n\n");
        prompt.append("User Requirements: ").append(userPrompt).append("\n\n");

        if (people != null && !people.isEmpty()) {
            prompt.append("Involved Parties:\n");
            people.forEach(person ->
                    prompt.append("- ")
                            .append(person.getName())
                            .append(" (")
                            .append(person.getRelevance())
                            .append(")\n")
            );
            prompt.append("\n");
        }

        prompt.append("Please generate a comprehensive document based on the above requirements.");
        return prompt.toString();
    }

    private String getTemplateForDocumentType(DocumentType type) {
        return switch (type) {
            case CONTRACT -> """
                    Create a {documentType} titled "{title}" with the following requirements:
                    
                    {userPrompt}
                    
                    Involved Parties:
                    {people}
                    
                    Please structure the contract with:
                    1. Preamble and parties identification
                    2. Definitions and interpretations
                    3. Main terms and conditions
                    4. Rights and obligations of each party
                    5. Payment terms (if applicable)
                    6. Termination clauses
                    7. Dispute resolution
                    8. Signatures section
                    
                    Ensure the contract is legally sound and comprehensive.
                    """;
            case REPORT -> """
                    Generate a comprehensive {documentType} titled "{title}":
                    
                    Requirements: {userPrompt}
                    
                    Stakeholders: {people}
                    
                    Structure the report with:
                    1. Executive Summary
                    2. Introduction and Scope
                    3. Methodology
                    4. Findings and Analysis
                    5. Recommendations
                    6. Conclusion
                    7. Appendices (if needed)
                    
                    Make it professional and data-driven.
                    """;
            case PROPOSAL -> """
                    Create a business {documentType} titled "{title}":
                    
                    Proposal Requirements: {userPrompt}
                    
                    Key Stakeholders: {people}
                    
                    Include these sections:
                    1. Executive Summary
                    2. Problem Statement
                    3. Proposed Solution
                    4. Implementation Timeline
                    5. Cost Breakdown
                    6. Benefits and ROI
                    7. Risk Assessment
                    8. Next Steps
                    
                    Make it compelling and professional.
                    """;
        };
    }

    private String formatPeopleForTemplate(List<LegalEntityDto> people) {
        if (people == null || people.isEmpty()) {
            return "No specific parties mentioned";
        }

        return people.stream()
                .map(person -> String.format("- %s (%s)", person.getName(), person.getRelevance()))
                .collect(Collectors.joining("\n"));
    }
}
