package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.data.object.Prompts;
import com.bntech.lexai.gateway.data.object.LegalFrame;
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
import com.bntech.lexai.gateway.service.ai.client.AiCallKind;
import com.bntech.lexai.gateway.service.ai.client.AiModelRouter;
import com.bntech.lexai.gateway.service.ai.client.AiPromptRequest;
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

import com.bntech.lexai.gateway.util.RetryUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    @Value("${app.ai.question-timeout-seconds:50}")
    private long questionTimeoutSeconds;

    @Value("${app.ai.openai.question-reasoning-effort:low}")
    private String questionReasoningEffort;

    private final ObjectMapper objectMapper;
    private final AiModelRouter aiModelRouter;
    private final EliService eliService;
    private final StorageFileService storageFileService;
    private final DocumentGenerationQualityService qualityService;
    private final QuestionQualityService questionQualityService;

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

                    LegalFrame legalFrame = normalizeLegalFrame(
                            providedFrame != null ? providedFrame : LegalFrame.empty(),
                            providedFrame != null ? providedFrame.getLegalBasis() : List.of());
                    return generateQuestionsWithBasis(
                            finalPrompt,
                            legalFrame.getLegalBasis(),
                            legalFrame,
                            List.of(),
                            files);
                })
                .timeout(questionTimeout())
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<SubmitPromptResponse> prepareTemplateQuestionnaire(
            String prompt,
            List<String> fileIds,
            SubmitPromptResponse template) {
        LegalFrame initialSeedFrame = legalFrameFromTemplate(template);
        questionQualityService.cleanFirstPassQuestions(template, initialSeedFrame);
        LegalFrame seedFrame = legalFrameFromTemplate(template);
        template.setLegalFrame(seedFrame);
        String intakePrompt = buildTemplateIntakePrompt(prompt, template);

        return Mono.zip(
                fetchTemplateFiles(template.getTemplateFiles()),
                fetchContextFiles(fileIds))
                .flatMap(files -> generateQuestionsWithBasis(
                        intakePrompt,
                        seedFrame.getLegalBasis(),
                        seedFrame,
                        files.getT1(),
                        files.getT2()))
                .map(response -> {
                    response.setTemplateFiles(template.getTemplateFiles());
                    response.setExampleId(template.getExampleId());
                    return response;
                })
                .timeout(questionTimeout())
                .doOnError(error -> log.error("❌ Error preparing template questionnaire", error));
    }

    public Mono<LegalBasisResponse> extractLegalBasis(String prompt) {
        log.info("Extracting legal basis for prompt length: {}", prompt != null ? prompt.length() : 0);
        return Mono.fromCallable(() -> {
            String jsonResponse = completeAi(
                    AiCallKind.LEGAL_BASIS,
                    "legal basis extraction",
                    Prompts.EN_EXTRACT_LEGAL_BASIS,
                    Prompts.EN_USER_MESSAGE + prompt,
                    reasoningTemperature,
                    2048L);

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

    private Mono<SubmitPromptResponse> generateQuestionsWithBasis(
            String prompt,
            List<String> verifiedBasis,
            LegalFrame legalFrame,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        log.info("Generating questions with verified legal basis: {}", verifiedBasis);
        return Mono.fromCallable(() -> {
            String basisJson = writeJson(verifiedBasis != null ? verifiedBasis : List.of());
            String legalFrameJson = writeJson(legalFrame != null ? legalFrame : LegalFrame.empty());

            String systemPrompt = String.format(Prompts.EN_GENERATE_QUESTIONS, basisJson, legalFrameJson, basisJson);

            String jsonResponse = completeAi(
                    AiCallKind.QUESTIONS,
                    "question generation",
                    systemPrompt,
                    Prompts.EN_USER_MESSAGE + prompt,
                    creativityTemperature,
                    2500L,
                    questionReasoningEffort,
                    templates,
                    contextFiles);

            SubmitPromptResponse response = objectMapper.readValue(stripMarkdownCodeBlocks(jsonResponse),
                    SubmitPromptResponse.class);
            LegalFrame resolvedFrame = mergeLegalFrames(response.getLegalFrame(), legalFrame);
            response.setLegalFrame(resolvedFrame);
            SubmitPromptResponse cleanedResponse = questionQualityService.cleanFirstPassQuestions(response, resolvedFrame);
            questionQualityService.retainVerifiedSuggestions(cleanedResponse, contextFileNames(contextFiles));
            applyLegalFrameToResponse(cleanedResponse, resolvedFrame);
            int questionCount = cleanedResponse.getQuestions() != null ? cleanedResponse.getQuestions().size() : 0;
            log.info("✅ Parsed response with {} questions", questionCount);
            return cleanedResponse;
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<SubmitPromptResponse> analyzeFollowUpQuestions(AnalyzeFollowUpQuestionsRequest request) {
        log.info("Analyzing whether follow-up questions are required for: {}", request.getName());
        LegalFrame legalFrame = normalizeLegalFrame(request.getLegalFrame(), request.getBase());
        legalFrame.setAttachmentRequirements(mergeDistinct(
                request.getRequirements(),
                legalFrame.getAttachmentRequirements()));

        return fetchContextFiles(request.getContextFileIds())
                .flatMap(contextFiles -> Mono.fromCallable(
                                () -> generateFollowUpQuestions(request, legalFrame, contextFiles))
                        .subscribeOn(Schedulers.boundedElastic()))
                .timeout(questionTimeout())
                .doOnError(error -> log.error("❌ Error analyzing follow-up questions", error));
    }

    private SubmitPromptResponse generateFollowUpQuestions(
            AnalyzeFollowUpQuestionsRequest request,
            LegalFrame legalFrame,
            List<FileContent> contextFiles) throws JsonProcessingException {
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

        String jsonResponse = completeAi(
                AiCallKind.FOLLOW_UP_QUESTIONS,
                "follow-up question analysis",
                Prompts.EN_ANALYZE_FOLLOW_UP_QUESTIONS,
                inputJson,
                reasoningTemperature,
                2200L,
                questionReasoningEffort,
                List.of(),
                contextFiles);

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
        LegalFrame resolvedFrame = mergeLegalFrames(response.getLegalFrame(), legalFrame);
        response.setLegalFrame(resolvedFrame);
        response.setBase(mergeDistinct(resolvedFrame.getLegalBasis(), response.getBase()));
        response.setRequirements(mergeDistinct(
                response.getRequirements(),
                resolvedFrame.getAttachmentRequirements()));
        response = questionQualityService.cleanFirstPassQuestions(response, resolvedFrame);
        questionQualityService.retainVerifiedSuggestions(response, contextFileNames(contextFiles));
        limitFollowUpDepth(response, request.getQuestions());
        log.info("✅ Follow-up analysis returned {} questions", response.getQuestions().size());
        return response;
    }

    private List<String> contextFileNames(List<FileContent> contextFiles) {
        if (contextFiles == null) {
            return List.of();
        }
        return contextFiles.stream().map(FileContent::name).toList();
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

            String rawContent = completeAi(
                    AiCallKind.DOCUMENT_CHAT,
                    request.getDocumentId(),
                    Prompts.EN_CHAT_WITH_DOCUMENT,
                    inputJson,
                    creativityTemperature,
                    8192L);

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

        LegalFrame legalFrame = normalizeLegalFrame(request.getLegalFrame(), request.getBase());
        legalFrame.setAttachmentRequirements(mergeDistinct(
                request.getRequirements(),
                legalFrame.getAttachmentRequirements()));
        request.setBase(legalFrame.getLegalBasis());
        request.setRequirements(legalFrame.getAttachmentRequirements());

        Mono<String> legalContextMono = fetchEliDocumentsContext(request, legalFrame);
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
                })
                .timeout(Duration.ofMinutes(5))
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
                generateWithAi(request.getTitle(), Prompts.EN_GENERATE_DOCUMENT, userMessage, templates,
                        contextFiles)), request.getBase());

        List<String> issues = qualityService.validate(generated, request);
        if (issues.isEmpty() || !qualityService.hasBlockingIssues(issues)) {
            if (!issues.isEmpty()) {
                log.warn("Generated document for {} has non-blocking quality warnings: {}",
                        request.getTitle(), issues);
            }
            return generated;
        }

        log.warn("Generated document for {} failed quality checks, requesting repair: {}",
                request.getTitle(), issues);

        String revisionMessage = qualityService.buildRevisionUserMessage(userMessage, generated, issues);
        String revised = qualityService.removeLegalBasisSection(qualityService.cleanHtmlFragment(
                repairWithAi(request.getTitle(), Prompts.EN_REPAIR_GENERATED_DOCUMENT, revisionMessage,
                        templates, contextFiles)), request.getBase());

        return qualityService.validateOrThrow(revised, request);
    }

    private String generateWithAi(
            String title,
            String systemPrompt,
            String userMessage,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        return completeAi(
                AiCallKind.DOCUMENT_GENERATION,
                title,
                systemPrompt,
                userMessage,
                reasoningTemperature,
                12000L,
                templates,
                contextFiles);
    }

    private String repairWithAi(
            String title,
            String systemPrompt,
            String userMessage,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        return completeAi(
                AiCallKind.DOCUMENT_REPAIR,
                title,
                systemPrompt,
                userMessage,
                reasoningTemperature,
                12000L,
                templates,
                contextFiles);
    }

    private String completeAi(
            AiCallKind kind,
            String title,
            String systemPrompt,
            String userMessage,
            double temperature,
            long maxTokens) {
        return completeAi(kind, title, systemPrompt, userMessage, temperature, maxTokens, List.of(), List.of());
    }

    private String completeAi(
            AiCallKind kind,
            String title,
            String systemPrompt,
            String userMessage,
            double temperature,
            long maxTokens,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        return completeAi(
                kind,
                title,
                systemPrompt,
                userMessage,
                temperature,
                maxTokens,
                null,
                templates,
                contextFiles);
    }

    private String completeAi(
            AiCallKind kind,
            String title,
            String systemPrompt,
            String userMessage,
            double temperature,
            long maxTokens,
            String reasoningEffort,
            List<FileContent> templates,
            List<FileContent> contextFiles) {
        String response = aiModelRouter.complete(new AiPromptRequest(
                kind,
                title,
                systemPrompt,
                userMessage,
                null,
                temperature,
                maxTokens,
                reasoningEffort,
                templates,
                contextFiles));
        return stripMarkdownCodeBlocks(response);
    }

    private Duration questionTimeout() {
        return Duration.ofSeconds(Math.max(1, questionTimeoutSeconds));
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

    private LegalFrame mergeLegalFrames(LegalFrame generated, LegalFrame seed) {
        LegalFrame generatedFrame = normalizeLegalFrame(generated, List.of());
        LegalFrame seedFrame = normalizeLegalFrame(seed, List.of());

        return LegalFrame.builder()
                .documentType(firstNonBlank(generatedFrame.getDocumentType(), seedFrame.getDocumentType()))
                .legalArea(firstNonBlank(generatedFrame.getLegalArea(), seedFrame.getLegalArea()))
                .procedureType(firstNonBlank(generatedFrame.getProcedureType(), seedFrame.getProcedureType()))
                .applicantRole(firstNonBlank(generatedFrame.getApplicantRole(), seedFrame.getApplicantRole()))
                .requestedOutcome(firstNonBlank(generatedFrame.getRequestedOutcome(), seedFrame.getRequestedOutcome()))
                .authorityRule(firstNonBlank(generatedFrame.getAuthorityRule(), seedFrame.getAuthorityRule()))
                .legalBasis(mergeDistinct(generatedFrame.getLegalBasis(), seedFrame.getLegalBasis()))
                .searchPhrases(mergeDistinct(generatedFrame.getSearchPhrases(), seedFrame.getSearchPhrases()))
                .legalInferences(mergeDistinct(generatedFrame.getLegalInferences(), seedFrame.getLegalInferences()))
                .userFactTargets(mergeDistinct(generatedFrame.getUserFactTargets(), seedFrame.getUserFactTargets()))
                .userShouldNotBeAsked(mergeDistinct(
                        generatedFrame.getUserShouldNotBeAsked(),
                        seedFrame.getUserShouldNotBeAsked()))
                .draftingGuidance(mergeDistinct(
                        generatedFrame.getDraftingGuidance(),
                        seedFrame.getDraftingGuidance()))
                .attachmentRequirements(mergeDistinct(
                        generatedFrame.getAttachmentRequirements(),
                        seedFrame.getAttachmentRequirements()))
                .build();
    }

    private LegalFrame legalFrameFromTemplate(SubmitPromptResponse template) {
        List<String> factTargets = template.getQuestions() == null
                ? List.of()
                : template.getQuestions().stream()
                        .map(QuestionDto::getText)
                        .filter(text -> text != null && !text.isBlank())
                        .toList();

        return normalizeLegalFrame(LegalFrame.builder()
                .documentType(template.getName())
                .legalBasis(template.getBase())
                .userFactTargets(factTargets)
                .attachmentRequirements(template.getRequirements())
                .draftingGuidance(List.of(
                        "Use the selected document template as the authoritative structural reference."))
                .build(), template.getBase());
    }

    private String buildTemplateIntakePrompt(String prompt, SubmitPromptResponse template) {
        return """
                User request: %s

                Selected document template: %s
                Curated coverage question bank: %s
                Known legal bases: %s
                Known attachment requirements: %s

                The curated question bank defines information that may be relevant to this document type. It is not a list that must be displayed one-for-one. Produce a minimum-sufficient first pass: reuse facts already supplied, group related required details, and defer conditional detail to follow-up rounds without losing required coverage.
                """.formatted(
                prompt != null ? prompt : "",
                template.getName() != null ? template.getName() : "",
                writeJson(template.getQuestions() != null ? template.getQuestions() : List.of()),
                writeJson(template.getBase() != null ? template.getBase() : List.of()),
                writeJson(template.getRequirements() != null ? template.getRequirements() : List.of()));
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
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

    private Mono<String> fetchEliDocumentsContext(GenerateDocumentRequest request, LegalFrame legalFrame) {
        List<String> base = request.getBase();
        if (base == null || base.isEmpty()) {
            return Mono.just("");
        }

        log.info("Generating URLs for {} legal bases", base.size());

        return eliService.fetchLegalDocuments(base, buildEliResearchTerms(request, legalFrame))
                .collectList()
                .map(documents -> {
                    log.info("✅ Resolved {} legal document source(s)", documents.size());
                    return eliService.buildLegalDocumentsContext(documents);
                })
                .onErrorResume(e -> {
                    log.warn("Failed to resolve ELI legal context, proceeding without legal context: {}",
                            e.getMessage());
                    return Mono.just("");
                });
    }

    private List<String> buildEliResearchTerms(GenerateDocumentRequest request, LegalFrame legalFrame) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        addTerm(terms, request.getTitle());
        addTerm(terms, request.getPrompt());
        addTerms(terms, request.getRequirements());

        if (request.getAnswers() != null) {
            request.getAnswers().values().forEach(value -> addTerm(terms, value));
        }

        if (legalFrame != null) {
            addTerm(terms, legalFrame.getDocumentType());
            addTerm(terms, legalFrame.getLegalArea());
            addTerm(terms, legalFrame.getProcedureType());
            addTerm(terms, legalFrame.getRequestedOutcome());
            addTerm(terms, legalFrame.getAuthorityRule());
            addTerms(terms, legalFrame.getSearchPhrases());
            addTerms(terms, legalFrame.getLegalInferences());
            addTerms(terms, legalFrame.getDraftingGuidance());
            addTerms(terms, legalFrame.getUserFactTargets());
        }

        return terms.stream()
                .filter(term -> term != null && !term.isBlank())
                .limit(24)
                .toList();
    }

    private void addTerms(Set<String> terms, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> addTerm(terms, value));
    }

    private void addTerm(Set<String> terms, String value) {
        if (value != null && !value.isBlank()) {
            terms.add(value.trim());
        }
    }

    String buildJsonForGeneration(GenerateDocumentRequest request) {
        try {
            Map<String, Object> jsonData = new LinkedHashMap<>();
            jsonData.put("name", request.getTitle() != null ? request.getTitle() : "");
            jsonData.put("originalRequest", request.getPrompt() != null ? request.getPrompt() : "");
            jsonData.put("intakeAnswers", buildIntakeAnswers(request));
            jsonData.put("requirements", request.getRequirements() != null ? request.getRequirements() : List.of());

            return objectMapper.writeValueAsString(jsonData);
        } catch (Exception e) {
            log.error("Error building JSON for generation", e);
            return "{}";
        }
    }

    private List<Map<String, Object>> buildIntakeAnswers(GenerateDocumentRequest request) {
        Map<String, String> answers = request.getAnswers() != null ? request.getAnswers() : Map.of();
        List<QuestionDto> questions = request.getQuestions() != null ? request.getQuestions() : List.of();
        List<Map<String, Object>> intakeAnswers = new ArrayList<>();
        Set<String> consumedKeys = new LinkedHashSet<>();

        for (int index = 0; index < questions.size(); index++) {
            QuestionDto question = questions.get(index);
            String indexKey = Integer.toString(index);
            String answer = answers.get(indexKey);
            String questionId = question.getId();

            if (answer == null && questionId != null) {
                answer = answers.get(questionId);
            }
            if (answer == null || answer.isBlank()) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", questionId != null ? questionId : indexKey);
            item.put("question", question.getText() != null ? question.getText() : "");
            item.put("answer", answer);
            item.put("followUpRound", question.getFollowupRound() != null ? question.getFollowupRound() : 0);
            intakeAnswers.add(item);
            consumedKeys.add(indexKey);
            if (questionId != null) {
                consumedKeys.add(questionId);
            }
        }

        answers.forEach((key, answer) -> {
            if (!consumedKeys.contains(key) && answer != null && !answer.isBlank()) {
                intakeAnswers.add(Map.of(
                        "questionId", key,
                        "question", "Legacy answer without question metadata",
                        "answer", answer,
                        "followUpRound", 0));
            }
        });
        return intakeAnswers;
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
