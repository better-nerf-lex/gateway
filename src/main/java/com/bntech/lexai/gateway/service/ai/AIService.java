package com.bntech.lexai.gateway.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.bntech.lexai.gateway.data.object.Prompts;
import com.bntech.lexai.gateway.data.object.StorageFileType;
import com.bntech.lexai.gateway.data.object.TemplateFile;
import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.exception.RateLimitException;
import com.bntech.lexai.gateway.rest.dto.document.AnalyzeFollowUpQuestionsRequest;
import com.bntech.lexai.gateway.rest.dto.document.DocumentChatRequest;
import com.bntech.lexai.gateway.rest.dto.document.DocumentChatResponse;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.LegalBasisResponse;
import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.bntech.lexai.gateway.service.document.StorageFileService;
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

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    private static final int MAX_FOLLOW_UP_ROUNDS = 2;

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

    @Value("${app.knowledge.enabled:true}")
    private boolean knowledgeEnabled;

    @Value("${app.knowledge.manifest-key:knowledge/manifest.json}")
    private String knowledgeManifestKey;

    @Value("${app.knowledge.max-conversations:2}")
    private int knowledgeMaxConversations;

    @Value("${app.knowledge.max-chars-per-conversation:8000}")
    private int knowledgeMaxCharsPerConversation;

    private AnthropicClient client;
    private final ObjectMapper objectMapper;
    private final EliService eliService;
    private final StorageFileService storageFileService;
    private final DocumentGenerationQualityService qualityService;
    private final QuestionQualityService questionQualityService;

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
        return analyzePrompt(prompt, fileIds, null);
    }

    public Mono<SubmitPromptResponse> analyzePrompt(String prompt, List<String> fileIds, LegalFrame providedFrame) {
        log.info("Analyzing prompt with {} files", fileIds != null ? fileIds.size() : 0);

        return fetchContextFiles(fileIds)
                .flatMap(files -> {
                    StringBuilder promptBuilder = new StringBuilder(prompt);
                    if (!files.isEmpty()) {
                        promptBuilder.append("\n\nAttached Files:\n");
                        files.forEach(f -> promptBuilder.append("- ").append(f.name()).append("\n"));
                    }
                    String finalPrompt = promptBuilder.toString();

                    if (providedFrame != null && !providedFrame.isEmptyFrame()) {
                        LegalFrame legalFrame = normalizeLegalFrame(providedFrame, providedFrame.getLegalBasis());
                        return generateQuestionsWithBasis(finalPrompt, legalFrame.getLegalBasis(), legalFrame);
                    }

                    return buildLegalFrame(finalPrompt)
                            .flatMap(legalFrame -> generateQuestionsWithBasis(finalPrompt,
                                    legalFrame.getLegalBasis(), legalFrame));
                });
    }

    public Mono<LegalBasisResponse> extractLegalBasis(String prompt) {
        log.info("Extracting legal basis for prompt length: {}", prompt != null ? prompt.length() : 0);
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

    public Mono<LegalFrame> buildLegalFrame(String prompt) {
        return buildLegalFrame(prompt, List.of())
                .onErrorResume(error -> {
                    log.warn("Failed to pre-build legal frame, continuing with empty frame: {}", error.getMessage());
                    return Mono.just(LegalFrame.empty());
                });
    }

    private Mono<SubmitPromptResponse> generateQuestionsWithBasis(
            String prompt,
            List<String> verifiedBasis,
            LegalFrame legalFrame) {
        log.info("Generating questions with verified legal basis: {}", verifiedBasis);
        return Mono.fromCallable(() -> {
            String basisJson = writeJson(verifiedBasis != null ? verifiedBasis : List.of());
            String legalFrameJson = writeJson(legalFrame != null ? legalFrame : LegalFrame.empty());

            String systemPrompt = String.format(Prompts.EN_GENERATE_QUESTIONS, basisJson, legalFrameJson, basisJson);

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
            SubmitPromptResponse cleanedResponse = questionQualityService.cleanFirstPassQuestions(response, legalFrame);
            applyLegalFrameToResponse(cleanedResponse, legalFrame);
            int questionCount = cleanedResponse.getQuestions() != null ? cleanedResponse.getQuestions().size() : 0;
            log.info("✅ Parsed response with {} questions", questionCount);
            return cleanedResponse;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(2))
                .retryWhen(buildRetrySpec("prompt analysis"))
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<SubmitPromptResponse> analyzeFollowUpQuestions(AnalyzeFollowUpQuestionsRequest request) {
        log.info("Analyzing whether follow-up questions are required for: {}", request.getName());
        return buildLegalFrame(buildFollowUpFramePrompt(request), request.getBase())
                .flatMap(legalFrame -> Mono.fromCallable(() -> {
            String inputJson = objectMapper.writeValueAsString(Map.of(
                    "prompt", request.getPrompt() != null ? request.getPrompt() : "",
                    "name", request.getName() != null ? request.getName() : "",
                    "questions", request.getQuestions() != null ? request.getQuestions() : List.of(),
                    "answers", request.getAnswers() != null ? request.getAnswers() : Map.of(),
                    "base", request.getBase() != null ? request.getBase() : List.of(),
                    "requirements", request.getRequirements() != null ? request.getRequirements() : List.of(),
                    "currentFollowUpRound", maxFollowUpRound(request.getQuestions()),
                    "maxFollowUpRounds", MAX_FOLLOW_UP_ROUNDS,
                    "legalFrame", legalFrame));

            MessageCreateParams params = MessageCreateParams.builder()
                    .maxTokens(4096L)
                    .system(Prompts.EN_ANALYZE_FOLLOW_UP_QUESTIONS)
                    .addUserMessage(inputJson)
                    .model(genModel)
                    .temperature(reasoningTemperature)
                    .build();

            log.debug("Sending follow-up question analysis request to Anthropic API");
            Message message = client.messages().create(params);
            String jsonResponse = message.content().getFirst()
                    .text()
                    .map(TextBlock::text)
                    .orElseThrow(() -> new IllegalStateException("No text content found in message"));

            SubmitPromptResponse response = objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse),
                    SubmitPromptResponse.class);
            response.setTemplateFiles(request.getTemplateFiles());
            response.setExampleId(request.getExampleId());
            if (response.getName() == null || response.getName().isBlank()) {
                response.setName(request.getName());
            }
            if (response.getQuestions() == null) {
                response.setQuestions(List.of());
            }
            if (response.getBase() == null) {
                response.setBase(request.getBase());
            }
            if (response.getRequirements() == null) {
                response.setRequirements(request.getRequirements());
            }
            response.setBase(mergeDistinct(legalFrame.getLegalBasis(), response.getBase()));
            response.setRequirements(mergeDistinct(response.getRequirements(), legalFrame.getAttachmentRequirements()));
            response = questionQualityService.cleanFirstPassQuestions(response, legalFrame);
            limitFollowUpDepth(response, request.getQuestions());
            log.info("✅ Follow-up analysis returned {} questions", response.getQuestions().size());
            return response;
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(2))
                .retryWhen(buildRetrySpec("follow-up question analysis"))
                .doOnError(error -> log.error("❌ Error analyzing follow-up questions", error));
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

        Mono<LegalFrame> legalFrameMono = buildLegalFrame(buildGenerationFramePrompt(request), request.getBase())
                .doOnNext(legalFrame -> {
                    request.setBase(mergeDistinct(legalFrame.getLegalBasis(), request.getBase()));
                    request.setRequirements(mergeDistinct(request.getRequirements(), legalFrame.getAttachmentRequirements()));
                })
                .cache();

        return legalFrameMono.flatMap(legalFrame -> {
            Mono<String> legalContextMono = fetchEliDocumentsContext(request.getBase());
            Mono<List<FileContent>> templateContentsMono = fetchTemplateFiles(templateFiles);
            Mono<List<FileContent>> contextFilesMono = fetchContextFiles(contextFileIds);
            Mono<List<KnowledgeConversation>> knowledgeConversationsMono = fetchKnowledgeConversations(request);

            return Mono.zip(legalContextMono, templateContentsMono, contextFilesMono, knowledgeConversationsMono)
                .flatMap(tuple -> {
                    String legalContext = tuple.getT1();
                    List<FileContent> templates = tuple.getT2();
                    List<FileContent> contextFiles = tuple.getT3();
                    List<KnowledgeConversation> knowledgeConversations = tuple.getT4();

                    return Mono.fromCallable(() -> {
                        String userMessage = buildUserMessage(request, legalContext, templates, contextFiles,
                                knowledgeConversations, legalFrame);
                        return generateQualityCheckedDocument(request, userMessage, templates, contextFiles);
                    }).subscribeOn(Schedulers.boundedElastic());
                });
        })
                .timeout(Duration.ofMinutes(5))
                .retryWhen(buildRetrySpec(request.getTitle()))
                .doOnSuccess(result -> log.info("✅ Document generated for: {}", request.getTitle()))
                .doOnError(error -> log.error("❌ Error generating document for: {}", request.getTitle(), error));
    }

    private String buildUserMessage(GenerateDocumentRequest request, String legalContext,
            List<FileContent> templates, List<FileContent> contextFiles,
            List<KnowledgeConversation> knowledgeConversations,
            LegalFrame legalFrame) {
        StringBuilder message = new StringBuilder();

        String jsonData = buildJsonForGeneration(request);
        message.append(jsonData);

        if (legalContext != null && !legalContext.isBlank()) {
            message.append("\n\n").append(legalContext);
        }

        if (request.getCaseContext() != null && !request.getCaseContext().isBlank()) {
            message.append("\n\n").append(request.getCaseContext());
        }

        appendContextFiles(message, contextFiles);
        appendTemplateFiles(message, templates);
        appendKnowledgeConversations(message, knowledgeConversations);
        appendLegalFrame(message, legalFrame);

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
            message.append(String.format("=== Context File %d: %s (%s) ===\n", i + 1, file.name(), file.fileType()));
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
        }
    }

    private void appendKnowledgeConversations(StringBuilder message, List<KnowledgeConversation> conversations) {
        if (conversations == null || conversations.isEmpty()) {
            return;
        }

        log.info("Using {} private knowledge conversation(s) for generation", conversations.size());

        message.append("\n\n--- PRIVATE KNOWLEDGE CONVERSATION EXAMPLES ---\n");
        message.append("The following private ChatGPT conversation transcripts were selected by filename/title/tags.\n");
        message.append("Use them only as confidential drafting knowledge: structure, legal reasoning, tone, section order, and quality bar.\n");
        message.append("Do NOT copy parties, addresses, PESEL numbers, case-specific facts, uploaded-file names, or transcript metadata.\n");
        message.append("Do NOT reproduce artifacts such as \"ChatGPT\", \"Thought for\", \"PDF\", branch notices, or user/assistant labels.\n");
        message.append("The generated document must still be based on the current user's facts and answers.\n\n");

        for (int i = 0; i < conversations.size(); i++) {
            KnowledgeConversation conversation = conversations.get(i);
            message.append(String.format("=== Knowledge Conversation %d: %s (%s) ===\n",
                    i + 1, conversation.title(), conversation.path()));
            message.append(conversation.content()).append("\n\n");
        }
    }

    private void appendLegalFrame(StringBuilder message, LegalFrame legalFrame) {
        if (legalFrame == null || legalFrame.isEmptyFrame()) {
            return;
        }

        message.append("\n\n--- LEGAL FRAME ---\n");
        message.append("Use this as the procedural and legal map for the document. ");
        message.append("It is not output content and must not be copied as a metadata section.\n");
        message.append(writeJson(legalFrame)).append("\n");
    }

    private String generateQualityCheckedDocument(
            GenerateDocumentRequest request,
            String userMessage,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        String generated = qualityService.removeLegalBasisSection(qualityService.cleanHtmlFragment(
                generateViaAnthropic(request.getTitle(), Prompts.EN_GENERATE_DOCUMENT, userMessage, templates,
                        contextFiles)), request.getBase());

        List<String> issues = qualityService.validate(generated, request);
        if (issues.isEmpty()) {
            return generated;
        }

        log.warn("Generated document for {} failed quality checks, requesting repair: {}",
                request.getTitle(), issues);

        String revisionMessage = qualityService.buildRevisionUserMessage(userMessage, generated, issues);
        String revised = qualityService.removeLegalBasisSection(qualityService.cleanHtmlFragment(
                generateViaAnthropic(request.getTitle(), Prompts.EN_REPAIR_GENERATED_DOCUMENT, revisionMessage,
                        templates, contextFiles)), request.getBase());

        return qualityService.validateOrThrow(revised, request);
    }

    private String generateViaAnthropic(
            String title,
            String systemPrompt,
            String userMessage,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(12000L)
                .system(systemPrompt)
                .addUserMessageOfBlockParams(buildGenerationContentBlocks(userMessage, templates, contextFiles))
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

    private List<ContentBlockParam> buildGenerationContentBlocks(
            String userMessage,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(userMessage).build()));
        appendFileBlocks(blocks, templates, "Template document");
        appendFileBlocks(blocks, contextFiles, "Context document");
        return blocks;
    }

    private void appendFileBlocks(List<ContentBlockParam> blocks, List<FileContent> files, String contextLabel) {
        if (files == null || files.isEmpty()) {
            return;
        }

        files.forEach(file -> buildFileBlock(file, contextLabel).forEach(blocks::add));
    }

    private List<ContentBlockParam> buildFileBlock(FileContent file, String contextLabel) {
        String base64 = Base64.getEncoder().encodeToString(file.content());
        String context = contextLabel + ": " + file.name();

        if (file.fileType() == StorageFileType.PDF) {
            return List.of(ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                    .base64Source(base64)
                    .title(file.name())
                    .context(context)
                    .build()));
        }

        if (file.fileType() == StorageFileType.TXT) {
            return List.of(ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                    .textSource(new String(file.content(), StandardCharsets.UTF_8))
                    .title(file.name())
                    .context(context)
                    .build()));
        }

        Base64ImageSource.MediaType mediaType = toImageMediaType(file.fileType());
        if (mediaType != null) {
            return List.of(
                    ContentBlockParam.ofText(TextBlockParam.builder().text(context).build()),
                    ContentBlockParam.ofImage(ImageBlockParam.builder()
                            .source(Base64ImageSource.builder()
                                    .data(base64)
                                    .mediaType(mediaType)
                                    .build())
                            .build()));
        }

        return List.of(ContentBlockParam.ofText(TextBlockParam.builder()
                .text(context + " was available in storage but cannot be attached directly because its type is "
                        + file.fileType() + ".")
                .build()));
    }

    private Base64ImageSource.MediaType toImageMediaType(StorageFileType fileType) {
        if (fileType == StorageFileType.JPG || fileType == StorageFileType.JPEG) {
            return Base64ImageSource.MediaType.IMAGE_JPEG;
        }
        if (fileType == StorageFileType.PNG) {
            return Base64ImageSource.MediaType.IMAGE_PNG;
        }
        if (fileType == StorageFileType.GIF) {
            return Base64ImageSource.MediaType.IMAGE_GIF;
        }
        return null;
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
                                    content,
                                    determineFileType(templateFile.getName())))
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
                                        content,
                                        dto.getFileType())))
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch context file {}: {}", id, e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .doOnSuccess(contents -> log.info("✅ Fetched {} context files successfully", contents.size()));
    }

    private Mono<List<KnowledgeConversation>> fetchKnowledgeConversations(GenerateDocumentRequest request) {
        if (!knowledgeEnabled || knowledgeMaxConversations <= 0) {
            return Mono.just(List.of());
        }

        return storageFileService.getFileContentByKey(knowledgeManifestKey)
                .map(this::readKnowledgeManifest)
                .map(manifest -> selectRelevantKnowledgeEntries(manifest, request))
                .flatMapMany(Flux::fromIterable)
                .flatMap(entry -> fetchKnowledgeConversation(entry)
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch knowledge conversation {}: {}", entry.path(), e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .doOnSuccess(conversations -> {
                    if (!conversations.isEmpty()) {
                        List<String> titles = conversations.stream()
                                .map(KnowledgeConversation::title)
                                .toList();
                        log.info("Selected private knowledge conversations: {}", titles);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Private knowledge manifest unavailable, proceeding without it: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private KnowledgeManifest readKnowledgeManifest(byte[] content) {
        try {
            return objectMapper.readValue(content, KnowledgeManifest.class);
        } catch (Exception e) {
            throw new ServiceException("AIService", "Invalid private knowledge manifest", e);
        }
    }

    private List<KnowledgeConversationEntry> selectRelevantKnowledgeEntries(
            KnowledgeManifest manifest,
            GenerateDocumentRequest request) {
        if (manifest == null || manifest.conversations() == null || manifest.conversations().isEmpty()) {
            return List.of();
        }

        String requestText = buildKnowledgeSearchText(request);
        String requestSlug = slugify(requestText);

        return manifest.conversations().stream()
                .filter(entry -> entry.path() != null && !entry.path().isBlank())
                .map(entry -> Map.entry(entry, scoreKnowledgeEntry(entry, requestText, requestSlug)))
                .filter(scored -> scored.getValue() > 0)
                .sorted(Map.Entry.<KnowledgeConversationEntry, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(knowledgeMaxConversations)
                .map(Map.Entry::getKey)
                .toList();
    }

    private int scoreKnowledgeEntry(KnowledgeConversationEntry entry, String requestText, String requestSlug) {
        if (hasConflictingProceduralTerm(requestText, knowledgeEntryTerms(entry))) {
            return 0;
        }

        int score = 0;

        String idSlug = slugify(entry.id());
        String titleSlug = slugify(entry.title());
        if (!idSlug.isBlank() && requestSlug.contains(idSlug)) {
            score += 30;
        }
        if (!titleSlug.isBlank() && requestSlug.contains(titleSlug)) {
            score += 25;
        }

        for (String value : knowledgeEntryTerms(entry)) {
            String normalized = normalizeForSearch(value);
            String slug = slugify(value);
            if (normalized.length() >= 3 && requestText.contains(normalized)) {
                score += 6;
            }
            if (slug.length() >= 3 && requestSlug.contains(slug)) {
                score += 6;
            }
            for (String token : normalized.split(" ")) {
                if (token.length() >= 4 && requestText.contains(token)) {
                    score += 1;
                }
            }
        }

        return score;
    }

    private boolean hasConflictingProceduralTerm(String requestText, Set<String> entryTerms) {
        String requestTerm = proceduralTerm(requestText);
        if (requestTerm.isBlank()) {
            return false;
        }

        String entryText = normalizeForSearch(String.join(" ", entryTerms));
        String entryTerm = proceduralTerm(entryText);
        return !entryTerm.isBlank() && !requestTerm.equals(entryTerm);
    }

    private String proceduralTerm(String text) {
        String normalized = normalizeForSearch(text);
        for (String term : List.of("odpowiedz", "pozew", "wniosek", "zazalenie", "apelacja", "skarga")) {
            if (normalized.matches(".*(^|\\s)" + term + "($|\\s).*")) {
                return term;
            }
        }
        return "";
    }

    private Set<String> knowledgeEntryTerms(KnowledgeConversationEntry entry) {
        Set<String> terms = new LinkedHashSet<>();
        addIfPresent(terms, entry.id());
        addIfPresent(terms, entry.title());
        addIfPresent(terms, entry.description());
        addAllIfPresent(terms, entry.documentTypes());
        addAllIfPresent(terms, entry.tags());
        return terms;
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private void addAllIfPresent(Set<String> values, List<String> source) {
        if (source == null) {
            return;
        }
        source.forEach(value -> addIfPresent(values, value));
    }

    private String buildKnowledgeSearchText(GenerateDocumentRequest request) {
        if (request == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        parts.add(request.getTitle());
        parts.add(request.getPrompt());
        if (request.getAnswers() != null) {
            request.getAnswers().forEach((key, value) -> {
                parts.add(key);
                parts.add(value);
            });
        }
        if (request.getRequirements() != null) {
            parts.addAll(request.getRequirements());
        }

        return normalizeForSearch(String.join(" ", parts.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList()));
    }

    private Mono<KnowledgeConversation> fetchKnowledgeConversation(KnowledgeConversationEntry entry) {
        String s3Key = normalizeKnowledgeConversationKey(entry.path());
        return storageFileService.getFileContentByKey(s3Key)
                .map(content -> new KnowledgeConversation(
                        entry.title() != null && !entry.title().isBlank() ? entry.title() : entry.id(),
                        entry.path(),
                        truncateKnowledgeConversation(new String(content, StandardCharsets.UTF_8))));
    }

    private String normalizeKnowledgeConversationKey(String path) {
        String normalized = path == null ? "" : path.strip().replace("\\", "/");
        if (normalized.isBlank() || normalized.equals("..") || normalized.startsWith("/")
                || normalized.startsWith("../") || normalized.contains("/../")
                || normalized.endsWith("/..") || normalized.contains("..\\")) {
            throw new ServiceException("AIService", "Invalid private knowledge conversation path");
        }
        if (normalized.startsWith("knowledge/")) {
            return normalized;
        }
        return "knowledge/" + normalized;
    }

    private String truncateKnowledgeConversation(String content) {
        if (content == null || content.length() <= knowledgeMaxCharsPerConversation) {
            return content == null ? "" : content;
        }
        return content.substring(0, knowledgeMaxCharsPerConversation)
                + "\n\n[conversation truncated for prompt size]";
    }

    private void limitFollowUpDepth(SubmitPromptResponse response, List<QuestionDto> existingQuestions) {
        if (response == null || response.getQuestions() == null) {
            return;
        }
        boolean canCreateOneMoreRound = maxFollowUpRound(existingQuestions) < MAX_FOLLOW_UP_ROUNDS - 1;
        if (!canCreateOneMoreRound) {
            response.getQuestions().forEach(question -> question.setFollowup(false));
        }
    }

    private int maxFollowUpRound(List<QuestionDto> questions) {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }
        return questions.stream()
                .map(QuestionDto::getFollowupRound)
                .filter(round -> round != null && round > 0)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private Mono<LegalFrame> buildLegalFrame(String prompt, List<String> verifiedBasis) {
        return Mono.fromCallable(() -> {
            String systemPrompt = String.format(Prompts.EN_BUILD_LEGAL_FRAME,
                    writeJson(verifiedBasis != null ? verifiedBasis : List.of()));

            MessageCreateParams params = MessageCreateParams.builder()
                    .maxTokens(2048L)
                    .system(systemPrompt)
                    .addUserMessage(Prompts.EN_USER_MESSAGE + (prompt != null ? prompt : ""))
                    .model(genModel)
                    .temperature(reasoningTemperature)
                    .build();

            log.debug("Sending legal frame request to Anthropic API");
            Message message = client.messages().create(params);
            String jsonResponse = message.content().getFirst()
                    .text()
                    .map(TextBlock::text)
                    .orElseThrow(() -> new IllegalStateException("No text content found in message"));

            LegalFrame legalFrame = objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse), LegalFrame.class);
            return normalizeLegalFrame(legalFrame, verifiedBasis);
        })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(1))
                .retryWhen(buildRetrySpec("legal frame"))
                .doOnSuccess(frame -> log.info("✅ Legal frame resolved: {} / {}",
                        frame.getLegalArea(), frame.getDocumentType()))
                .onErrorResume(error -> {
                    log.warn("Failed to build legal frame, falling back to detected basis only: {}", error.getMessage());
                    return Mono.just(normalizeLegalFrame(LegalFrame.empty(), verifiedBasis));
                });
    }

    private LegalFrame normalizeLegalFrame(LegalFrame legalFrame, List<String> fallbackBasis) {
        LegalFrame safe = legalFrame != null ? legalFrame : LegalFrame.empty();
        safe.setLegalBasis(mergeDistinct(safe.getLegalBasis(), fallbackBasis));
        safe.setSearchPhrases(nonNullList(safe.getSearchPhrases()));
        safe.setLegalInferences(nonNullList(safe.getLegalInferences()));
        safe.setUserFactTargets(nonNullList(safe.getUserFactTargets()));
        safe.setUserShouldNotBeAsked(nonNullList(safe.getUserShouldNotBeAsked()));
        safe.setDraftingGuidance(nonNullList(safe.getDraftingGuidance()));
        safe.setAttachmentRequirements(nonNullList(safe.getAttachmentRequirements()));
        return safe;
    }

    private void applyLegalFrameToResponse(SubmitPromptResponse response, LegalFrame legalFrame) {
        if (response == null || legalFrame == null) {
            return;
        }
        response.setBase(mergeDistinct(legalFrame.getLegalBasis(), response.getBase()));
        response.setRequirements(mergeDistinct(response.getRequirements(), legalFrame.getAttachmentRequirements()));
        if ((response.getName() == null || response.getName().isBlank())
                && legalFrame.getDocumentType() != null && !legalFrame.getDocumentType().isBlank()) {
            response.setName(legalFrame.getDocumentType());
        }
    }

    private List<String> mergeDistinct(List<String> first, List<String> second) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (first != null) {
            first.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(values::add);
        }
        if (second != null) {
            second.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(values::add);
        }
        return new ArrayList<>(values);
    }

    private List<String> nonNullList(List<String> values) {
        return values != null ? values : List.of();
    }

    private String buildFollowUpFramePrompt(AnalyzeFollowUpQuestionsRequest request) {
        return """
                Document request: %s
                Current document name: %s
                Existing questions: %s
                User answers: %s
                Existing requirements: %s
                """.formatted(
                request.getPrompt() != null ? request.getPrompt() : "",
                request.getName() != null ? request.getName() : "",
                writeJson(request.getQuestions() != null ? request.getQuestions() : List.of()),
                writeJson(request.getAnswers() != null ? request.getAnswers() : Map.of()),
                writeJson(request.getRequirements() != null ? request.getRequirements() : List.of()));
    }

    private String buildGenerationFramePrompt(GenerateDocumentRequest request) {
        return """
                Document title: %s
                Original prompt: %s
                Answers: %s
                Requirements: %s
                Case context available: %s
                """.formatted(
                request.getTitle() != null ? request.getTitle() : "",
                request.getPrompt() != null ? request.getPrompt() : "",
                writeJson(request.getAnswers() != null ? request.getAnswers() : Map.of()),
                writeJson(request.getRequirements() != null ? request.getRequirements() : List.of()),
                request.getCaseContext() != null && !request.getCaseContext().isBlank());
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
                    "requirements", request.getRequirements() != null ? request.getRequirements() : List.of());

            return objectMapper.writeValueAsString(jsonData);
        } catch (Exception e) {
            log.error("Error building JSON for generation", e);
            return "{}";
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private StorageFileType determineFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return StorageFileType.OTHER;
        }
        return StorageFileType.fromValue(filename.substring(filename.lastIndexOf('.') + 1));
    }

    private String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private String slugify(String value) {
        return normalizeForSearch(value).replace(' ', '-');
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
