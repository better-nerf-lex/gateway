package com.bntech.lexai.gateway.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.bntech.lexai.gateway.data.object.Prompts;
import com.bntech.lexai.gateway.data.object.TemplateFile;
import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.exception.RateLimitException;
import com.bntech.lexai.gateway.rest.dto.document.DocumentChatRequest;
import com.bntech.lexai.gateway.rest.dto.document.DocumentChatResponse;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.LegalBasisResponse;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;

import com.bntech.lexai.gateway.util.RetryUtils;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model}")
    private String genModel;

    @Value("${spring.ai.anthropic.chat.options.chat-model}")
    private String chatModel;

    @Value("${spring.ai.anthropic.chat.options.creativity-temperature}")
    private double creativityTemperature;

    @Value("${spring.ai.anthropic.chat.options.reasoning-temperature}")
    private double reasoningTemperature;

    private AnthropicClient client;
    private final ObjectMapper objectMapper;
    private final EliService eliService;
    private final StorageFileService storageFileService;

    @PostConstruct
    private void initializeClient() {
        log.debug("Initializing Anthropic client with API key: {}...",
                apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null");

        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public Mono<SubmitPromptResponse> analyzePrompt(String prompt) {
        return analyzePrompt(prompt, null);
    }

    public Mono<SubmitPromptResponse> analyzePrompt(String prompt, List<String> fileIds) {
        log.info("Analyzing prompt: {} with {} files", prompt, fileIds != null ? fileIds.size() : 0);

        return fetchContextFiles(fileIds)
                .flatMap(files -> {
                    StringBuilder promptBuilder = new StringBuilder(prompt);
                    if (!files.isEmpty()) {
                        promptBuilder.append("\n\nAttached Files:\n");
                        files.forEach(f -> promptBuilder.append("- ").append(f.name()).append("\n"));
                    }
                    String finalPrompt = promptBuilder.toString();

                    return extractLegalBasis(finalPrompt)
                            .flatMap(legalBasisResponse -> verifyLegalBasis(legalBasisResponse.getBase())
                                    .flatMap(verifiedBasis -> generateQuestionsWithBasis(finalPrompt, verifiedBasis)));
                });
    }

    public Mono<LegalBasisResponse> extractLegalBasis(String prompt) {
        log.info("Extracting legal basis for prompt: {}", prompt);
        return Mono.fromCallable(() -> {
            MessageCreateParams params = MessageCreateParams.builder()
                    .maxTokens(2048L)
                    .system(Prompts.EN_EXTRACT_LEGAL_BASIS)
                    .addUserMessage(Prompts.EN_USER_MESSAGE + prompt)
                    .model(genModel)
                    .temperature(reasoningTemperature)
                    .build();

            log.debug("Sending legal basis extraction request to Anthropic API");
            Message message = client.messages().create(params);
            String jsonResponse = message.content().getFirst()
                    .text()
                    .map(TextBlock::text)
                    .orElseThrow(() -> new IllegalStateException("No text content found in message"));

            LegalBasisResponse response = objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse),
                    LegalBasisResponse.class);
            log.info("✅ Extracted {} legal bases: {}", response.getBase().size(), response.getBase());
            return response;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(1))
                .retryWhen(buildRetrySpec("legal basis extraction"))
                .doOnError(error -> log.error("❌ Error extracting legal basis", error));
    }

    public Mono<List<String>> verifyLegalBasis(List<String> bases) {
        if (bases == null || bases.isEmpty()) {
            return Mono.just(List.of());
        }

        log.info("Using {} legal bases for document generation", bases.size());
        return Mono.just(bases);
    }

    private Mono<SubmitPromptResponse> generateQuestionsWithBasis(String prompt, List<String> verifiedBasis) {
        log.info("Generating questions with verified legal basis: {}", verifiedBasis);
        return Mono.fromCallable(() -> {
            String basisJson;
            try {
                basisJson = objectMapper.writeValueAsString(verifiedBasis);
            } catch (Exception e) {
                basisJson = "[]";
            }

            String systemPrompt = String.format(Prompts.EN_GENERATE_QUESTIONS, basisJson, basisJson);

            MessageCreateParams params = MessageCreateParams.builder()
                    .maxTokens(4096L)
                    .system(systemPrompt)
                    .addUserMessage(Prompts.EN_USER_MESSAGE + prompt)
                    .model(genModel)
                    .temperature(creativityTemperature)
                    .build();

            log.debug("Sending prompt analysis request to Anthropic API");
            Message message = client.messages().create(params);
            String jsonResponse = message.content().getFirst()
                    .text()
                    .map(TextBlock::text)
                    .orElseThrow(() -> new IllegalStateException("No text content found in message"));

            SubmitPromptResponse response = objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse),
                    SubmitPromptResponse.class);
            log.info("✅ Parsed response with {} questions", response.getQuestions().size());
            return response;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(2))
                .retryWhen(buildRetrySpec("prompt analysis"))
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<DocumentChatResponse> chatWithDocument(DocumentChatRequest request) {
        log.info("Chatting with document: {}", request.getDocumentId());
        return Mono.fromCallable(() -> {
            String inputJson;
            try {
                inputJson = objectMapper.writeValueAsString(Map.of(
                        "currentContent", request.getCurrentContent(),
                        "userRequest", request.getUserMessage()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing chat request", e);
            }

            MessageCreateParams params = MessageCreateParams.builder()
                    .maxTokens(8192L)
                    .system(Prompts.EN_CHAT_WITH_DOCUMENT)
                    .addUserMessage(inputJson)
                    .model(chatModel)
                    .temperature(creativityTemperature)
                    .build();

            log.debug("Sending chat request to Anthropic API for document: {}", request.getDocumentId());
            Message message = client.messages().create(params);

            String rawContent = message.content().getFirst()
                    .text()
                    .map(TextBlock::text)
                    .orElseThrow(() -> new IllegalStateException("No text content found in message"));

            String jsonContent = stripMarkdownCodeBlocks(rawContent);
            return objectMapper.readValue(jsonContent, DocumentChatResponse.class);
        })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(2))
                .retryWhen(buildRetrySpec("document chat"))
                .doOnSuccess(response -> log.info("✅ Chat response received for document: {}", request.getDocumentId()))
                .doOnError(error -> log.error("❌ Error during document chat for: {}", request.getDocumentId(), error));
    }

    public Mono<String> generateDocumentWithAnswers(GenerateDocumentRequest request) {
        List<TemplateFile> templateFiles = request.getTemplateFiles();
        List<String> contextFileIds = request.getContextFileIds();

        int templateCount = templateFiles != null ? templateFiles.size() : 0;
        int contextCount = contextFileIds != null ? contextFileIds.size() : 0;

        log.info("Generating document: {} with {} template files, {} context files",
                request.getTitle(), templateCount, contextCount);

        Mono<String> legalContextMono = fetchEliDocumentsContext(request.getBase());
        Mono<List<FileContent>> templateContentsMono = fetchTemplateFiles(templateFiles);
        Mono<List<FileContent>> contextFilesMono = fetchContextFiles(contextFileIds);

        return Mono.zip(legalContextMono, templateContentsMono, contextFilesMono)
                .flatMap(tuple -> {
                    String legalContext = tuple.getT1();
                    List<FileContent> templates = tuple.getT2();
                    List<FileContent> contextFiles = tuple.getT3();

                    return Mono.fromCallable(() -> {
                        String userMessage = buildUserMessage(request, legalContext, templates, contextFiles);
                        return generateViaAnthropic(request.getTitle(), userMessage);
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .timeout(Duration.ofMinutes(5))
                .retryWhen(buildRetrySpec(request.getTitle()))
                .doOnSuccess(result -> log.info("✅ Document generated for: {}", request.getTitle()))
                .doOnError(error -> log.error("❌ Error generating document for: {}", request.getTitle(), error));
    }

    private String buildUserMessage(GenerateDocumentRequest request, String legalContext,
            List<FileContent> templates, List<FileContent> contextFiles) {
        StringBuilder message = new StringBuilder();

        String jsonData = buildJsonForGeneration(request);
        message.append(jsonData);

        if (legalContext != null && !legalContext.isBlank()) {
            message.append("\n\n").append(legalContext);
        }

        appendContextFiles(message, contextFiles);
        appendTemplateFiles(message, templates);

        return message.toString();
    }

    private void appendContextFiles(StringBuilder message, List<FileContent> contextFiles) {
        if (contextFiles == null || contextFiles.isEmpty()) {
            return;
        }

        log.info("Attaching {} context files to prompt", contextFiles.size());
        message.append("\n\n--- ATTACHED CONTEXT FILES ---\n");
        message.append("The following files are attached by the user as additional context for document generation.\n");
        message.append("Use the information from these files to inform the content of the generated document.\n\n");

        for (int i = 0; i < contextFiles.size(); i++) {
            FileContent file = contextFiles.get(i);
            message.append(String.format("=== Context File %d: %s ===\n", i + 1, file.name()));
            message.append(String.format("Content (Base64 encoded): %s\n\n", file.base64Content()));
        }
    }

    private void appendTemplateFiles(StringBuilder message, List<FileContent> templates) {
        if (templates == null || templates.isEmpty()) {
            return;
        }

        List<String> fileNames = templates.stream().map(FileContent::name).toList();
        log.info("Attaching {} template files to prompt: {}", templates.size(), fileNames);

        message.append("\n\n--- TEMPLATE DOCUMENTS ---\n");
        message.append("The following template documents are provided as visual and structural references.\n");
        message.append("Use them to understand the expected document layout, formatting style, and section organization.\n");
        message.append("Match the visual structure while filling in the actual data from the questions above.\n\n");

        for (int i = 0; i < templates.size(); i++) {
            FileContent template = templates.get(i);
            message.append(String.format("=== Template %d: %s ===\n", i + 1, template.name()));
            message.append(String.format("Content (Base64 encoded): %s\n\n", template.base64Content()));
        }
    }

    private String generateViaAnthropic(String title, String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(8192L)
                .system(Prompts.EN_GENERATE_DOCUMENT)
                .addUserMessage(userMessage)
                .model(genModel)
                .temperature(reasoningTemperature)
                .build();

        log.debug("Sending document generation request to Anthropic API for: {}", title);
        Message response = client.messages().create(params);
        log.debug("Received document generation response from Anthropic API for: {}", title);

        String rawContent = response.content().getFirst()
                .text()
                .map(TextBlock::text)
                .orElseThrow(() -> new IllegalStateException("No text content found in message"));

        return stripMarkdownCodeBlocks(rawContent);
    }

    private record FileContent(String name, String base64Content) {
    }

    private Mono<List<FileContent>> fetchTemplateFiles(List<TemplateFile> templateFiles) {
        if (templateFiles == null || templateFiles.isEmpty()) {
            return Mono.just(List.of());
        }

        log.info("Fetching {} template files from S3", templateFiles.size());

        return Flux.fromIterable(templateFiles)
                .flatMap(templateFile -> {
                    log.debug("Fetching template file: {} ({})", templateFile.getName(), templateFile.getS3Key());
                    return storageFileService.getFileContentByKey(templateFile.getS3Key())
                            .map(content -> new FileContent(
                                    templateFile.getName(),
                                    Base64.getEncoder().encodeToString(content)))
                            .onErrorResume(e -> {
                                log.warn("Failed to fetch template file {}: {}", templateFile.getS3Key(),
                                        e.getMessage());
                                return Mono.empty();
                            });
                })
                .collectList()
                .doOnSuccess(contents -> log.info("✅ Fetched {} template files successfully", contents.size()))
                .onErrorResume(e -> {
                    log.warn("Failed to fetch template files, proceeding without templates: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private Mono<List<FileContent>> fetchContextFiles(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Mono.just(List.of());
        }

        log.info("Fetching {} context files from storage", fileIds.size());

        return Flux.fromIterable(fileIds)
                .flatMap(id -> storageFileService.getDocumentFileById(id)
                        .flatMap(dto -> storageFileService.getFileContent(id)
                                .map(content -> new FileContent(
                                        dto.getName(),
                                        Base64.getEncoder().encodeToString(content))))
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch context file {}: {}", id, e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .doOnSuccess(contents -> log.info("✅ Fetched {} context files successfully", contents.size()));
    }

    private Mono<String> fetchEliDocumentsContext(List<String> base) {
        if (base == null || base.isEmpty()) {
            return Mono.just("");
        }

        log.info("Generating URLs for {} legal bases", base.size());

        return eliService.fetchLegalDocuments(base)
                .collectList()
                .map(documents -> {
                    log.info("✅ Generated {} legal document URLs", documents.size());
                    return eliService.buildLegalDocumentsContext(documents);
                })
                .onErrorResume(e -> {
                    log.warn("Failed to generate ELI document URLs, proceeding without legal context: {}",
                            e.getMessage());
                    return Mono.just("");
                });
    }

    private String buildJsonForGeneration(GenerateDocumentRequest request) {
        try {
            Map<String, Object> jsonData = Map.of(
                    "name", request.getTitle() != null ? request.getTitle() : "",
                    "questions", request.getAnswers() != null ? request.getAnswers() : Map.of(),
                    "requirements", request.getRequirements() != null ? request.getRequirements() : List.of(),
                    "base", request.getBase() != null ? request.getBase() : List.of());

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
                .filter(RetryUtils::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    Throwable exception = retrySignal.failure();
                    log.warn("Retrying AI call for {} (attempt {}) due to: {}",
                            identifier, retrySignal.totalRetries() + 1, exception.getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Retry exhausted for {} after {} attempts", identifier, retrySignal.totalRetries());
                    Throwable failure = retrySignal.failure();
                    
                    if (isRateLimitError(failure)) {
                        Integer retryAfter = extractRetryAfter(failure);
                        String message = "Rate limit exceeded. Please try again later.";
                        log.warn("Rate limit error detected with retry-after: {} seconds", retryAfter);
                        return new RateLimitException(message, retryAfter, failure);
                    }
                    
                    return new ServiceException("AIService",
                            "AI service unavailable after " + retrySignal.totalRetries() + " attempts",
                            failure);
                });
    }

    private boolean isRateLimitError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        return message != null && (message.contains("429") || message.contains("rate_limit"));
    }

    private Integer extractRetryAfter(Throwable throwable) {
        if (throwable == null) {
            return 60;
        }
        
        String message = throwable.getMessage();
        if (message == null) {
            return 60;
        }
        
        try {
            if (message.contains("retry-after")) {
                int start = message.indexOf("retry-after");
                String afterPart = message.substring(start);
                String[] parts = afterPart.split("[^0-9]+");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        return Integer.parseInt(part);
                    }
                }
            }
            
            if (message.contains("30,000 input tokens per minute")) {
                return 60;
            }
            
            return 60;
        } catch (Exception e) {
            log.warn("Failed to extract retry-after from error message, defaulting to 60 seconds", e);
            return 60;
        }
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
