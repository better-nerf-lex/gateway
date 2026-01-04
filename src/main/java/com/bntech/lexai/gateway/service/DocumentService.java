package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.DocumentHistory;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.object.DocumentStatus;
import com.bntech.lexai.gateway.data.repository.*;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.CaseMapper;
import com.bntech.lexai.gateway.mapper.DocumentMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentHistoryDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptRequest;
import org.jsoup.Jsoup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {
    private final DocumentRepository documents;
    private final DocumentAggregateRepository aggregates;
    private final DocumentHistoryRepository documentsHistory;
    private final DocumentMapper mapper;
    private final AIService aiService;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CaseRepository caseRepository;
    private final com.bntech.lexai.gateway.mapper.CaseMapper caseMapper;
    private final DatabaseClient databaseClient;
    private final DocumentExampleService documentExampleService;

    public DocumentService(
            DocumentRepository documents,
            DocumentAggregateRepository aggregates,
            DocumentHistoryRepository documentsHistory,
            DocumentMapper mapper,
            AIService aiService,
            UserRepository userRepository,
            TeamMemberRepository teamMemberRepository,
            CaseRepository caseRepository,
            CaseMapper caseMapper,
            DatabaseClient databaseClient,
            @Autowired(required = false) DocumentExampleService documentExampleService) {
        this.documents = documents;
        this.aggregates = aggregates;
        this.documentsHistory = documentsHistory;
        this.mapper = mapper;
        this.aiService = aiService;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.caseRepository = caseRepository;
        this.caseMapper = caseMapper;
        this.databaseClient = databaseClient;
        this.documentExampleService = documentExampleService;
    }

    public Flux<DocumentDto> getDocuments() {
        return getDocuments(null);
    }

    public Flux<DocumentDto> getDocuments(String teamId) {
        if (teamId != null && !teamId.isBlank() && !teamId.equals("null") && !teamId.equals("undefined")) {
            return documents.findByTeamIdOrderByCreatedAtDesc(UUID.fromString(teamId))
                    .map(mapper::toDto)
                    .doOnError(error -> log.error("Error retrieving team documents for {}", teamId, error));
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> userRepository.findByUsername(auth.getName())
                        .flatMapMany(user -> {
                            reactor.core.publisher.Flux<Document> byUser = documents
                                    .findByOwnerIdOrderByCreatedAtDesc(user.getId());

                            reactor.core.publisher.Flux<UUID> teamIds = teamMemberRepository.findByUserId(user.getId())
                                    .map(TeamMember::getTeamId);

                            reactor.core.publisher.Flux<Document> byTeams = teamIds
                                    .flatMap(documents::findByTeamIdOrderByCreatedAtDesc);

                            return reactor.core.publisher.Flux.merge(byUser, byTeams);
                        }))
                .distinct(Document::getId)
                .map(mapper::toDto)
                .flatMap(this::enrichWithCases)
                .doOnError(error -> log.error("Error retrieving documents", error));
    }

    public Mono<DocumentDto> getDocument(String id) {
        log.info("Fetching document: {}", id);
        return documents.findById(UUID.fromString(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Document", id)))
                .map(mapper::toDto)
                .flatMap(this::enrichWithCases)
                .doOnSuccess(doc -> log.info("Document enriched with cases: {}", doc.getId()))
                .doOnError(error -> log.error("Error retrieving document: {}", id, error));
    }

    private Mono<DocumentDto> enrichWithCases(DocumentDto dto) {
        if (dto.getId() == null)
            return Mono.just(dto);
        return caseRepository.findByDocumentId(UUID.fromString(dto.getId()))
                .map(caseMapper::toDto)
                .collectList()
                .map(cases -> {
                    dto.setCases(cases);
                    return dto;
                });
    }

    public Mono<DocumentStatsDto> getDocumentStats() {
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();

        return Mono.zip(
                aggregates.countAll(),
                aggregates.countThisMonth(startOfMonth),
                aggregates.countPending(),
                aggregates.countCompleted()).map(
                        tuple -> DocumentStatsDto.builder()
                                .totalDocuments(tuple.getT1().intValue())
                                .thisMonth(tuple.getT2().intValue())
                                .pending(tuple.getT3().intValue())
                                .completed(tuple.getT4().intValue())
                                .build())
                .doOnError(error -> log.error("Error retrieving document stats", error));
    }

    public Mono<SubmitPromptResponse> submitPrompt(SubmitPromptRequest request) {
        log.info("Processing prompt submission: {}", request.getPrompt());

        Mono<SubmitPromptResponse> templateSearch = documentExampleService != null
                ? documentExampleService.searchTemplates(request.getPrompt())
                        .next()
                        .map(template -> SubmitPromptResponse.builder()
                                .name(template.getTitle())
                                .questions(template.getQuestions())
                                .base(template.getBase())
                                .requirements(template.getRequirements())
                                .build())
                : Mono.empty();

        return templateSearch
                .switchIfEmpty(aiService.analyzePrompt(request.getPrompt()))
                .doOnSuccess(response -> {
                    log.info("✅ Prompt analysis completed {}", response);
                })
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<DocumentDto> generateDocument(GenerateDocumentRequest request) {
        log.info("Generating document with request: {}", request);

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String username = auth.getName();
                    return userRepository.findByUsername(username)
                            .flatMap(user -> {
                                String requestedTeamId = request.getTeamId();
                                Mono<UUID> teamIdMono;
                                if (requestedTeamId != null && !requestedTeamId.isBlank()
                                        && !requestedTeamId.equals("null") && !requestedTeamId.equals("undefined")) {
                                    teamIdMono = teamMemberRepository
                                            .findByUserIdAndTeamId(user.getId(), UUID.fromString(requestedTeamId))
                                            .switchIfEmpty(
                                                    Mono.error(new AccessDeniedException("Insufficient team access")))
                                            .map(TeamMember::getTeamId);
                                } else {
                                    teamIdMono = teamMemberRepository.findByUserId(user.getId())
                                            .next()
                                            .map(TeamMember::getTeamId);
                                }

                                return teamIdMono.flatMap(teamId -> {
                                    Document document = mapper.toEntity(DocumentDto.builder()
                                            .id(UUID.randomUUID().toString())
                                            .title(request.getTitle())
                                            .type(null) // No predefined type, it's prompt-based
                                            .status(DocumentStatus.PENDING)
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .text("")
                                            .build());
                                    document.setOwnerId(user.getId());
                                    if (teamId != null) {
                                        document.setTeamId(teamId);
                                    }

                                    return documents.save(document)
                                            .map(doc -> {
                                                doc.setNotNew();
                                                return doc;
                                            })
                                            .flatMap(savedDocument -> processAIGeneration(savedDocument, request));
                                });
                            });
                });
    }

    private Mono<DocumentDto> processAIGeneration(Document document, GenerateDocumentRequest request) {
        return aiService.generateDocumentWithAnswers(request)
                .map(content -> {
                    log.info("Generated document content raw: {}", content);
                    // Clean up HTML using Jsoup to ensure we only have the body content
                    // and no html/head/body tags that confuse editors
                    String cleanedContent = content;
                    try {
                        org.jsoup.nodes.Document doc = Jsoup.parse(content);
                        // If there is a body, use its html, otherwise use the whole thing (it might be
                        // a fragment)
                        if (doc.body() != null && !doc.body().html().isBlank()) {
                            cleanedContent = doc.body().html();
                        } else {
                            // Fallback if body is empty but doc has content
                            cleanedContent = doc.html();
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse/clean HTML content, using raw", e);
                    }
                    log.info("Cleaned document content: {}", cleanedContent);
                    return cleanedContent;
                })
                .flatMap(content -> updateDocumentWithContent(document, content))
                .doOnSuccess(result -> log.info("✅ AI generation completed for: {}", document.getTitle()))
                .doOnError(error -> log.error("❌ AI generation failed for: {}", document.getTitle(), error))
                .onErrorResume(error -> failDocument(document)); // Remove documentMapper.toDto() here
    }

    private Mono<DocumentDto> updateDocumentWithContent(Document document, String content) {
        return documents.findById(document.getId())
                .map(existingDoc -> {
                    existingDoc.setStatus(DocumentStatus.COMPLETED);
                    existingDoc.setUpdatedAt(LocalDateTime.now());
                    existingDoc.setNotNew();
                    existingDoc.setText(content);
                    return existingDoc;
                })
                .switchIfEmpty(Mono.just(document)
                        .map(doc -> {
                            doc.setStatus(DocumentStatus.COMPLETED);
                            doc.setUpdatedAt(LocalDateTime.now());
                            doc.setText(content);
                            return doc;
                        }))
                .flatMap(documents::save)
                .map(mapper::toDto);
    }

    private Mono<DocumentDto> failDocument(Document document) {
        log.error("Failing document: {}", document.getTitle());
        return documents.findById(document.getId())
                .map(existingDoc -> {
                    existingDoc.setStatus(DocumentStatus.ERROR);
                    existingDoc.setUpdatedAt(LocalDateTime.now());
                    existingDoc.setNotNew();
                    return existingDoc;
                })
                .switchIfEmpty(Mono.just(document)
                        .map(doc -> {
                            doc.setStatus(DocumentStatus.ERROR);
                            doc.setUpdatedAt(LocalDateTime.now());
                            return doc;
                        }))
                .flatMap(documents::save)
                .map(mapper::toDto);
    }

    public Mono<DocumentDto> updateDocument(String id, String content) {
        log.info("Updating document: {}", id);

        return documents.findById(UUID.fromString(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Document", id)))
                .flatMap(document -> {
                    return createHistoryEntry(document)
                            .then(Mono.just(document));
                })
                .flatMap(document -> {
                    document.setText(content);
                    document.setUpdatedAt(LocalDateTime.now());
                    document.setNotNew();
                    return documents.save(document);
                })
                .map(mapper::toDto)
                .doOnSuccess(result -> log.info("✅ Document updated: {}", id))
                .doOnError(error -> log.error("❌ Error updating document: {}", id, error));
    }

    public Flux<DocumentHistoryDto> getDocumentHistory(String documentId) {
        log.info("Fetching history for document: {}", documentId);

        return documentsHistory.findByDocumentIdOrderByVersionDesc(UUID.fromString(documentId))
                .map(this::toHistoryDto)
                .doOnError(error -> log.error("Error fetching document history", error));
    }

    public Mono<DocumentDto> restoreDocumentVersion(String documentId, String versionId) {
        log.info("Restoring document {} to version {}", documentId, versionId);

        return documentsHistory.findById(UUID.fromString(versionId))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentVersion", versionId)))
                .flatMap(history -> updateDocument(documentId, history.getContent()))
                .doOnSuccess(result -> log.info("✅ Document restored to version: {}", versionId))
                .doOnError(error -> log.error("❌ Error restoring document version", error));
    }

    private Mono<DocumentHistory> createHistoryEntry(Document document) {
        return databaseClient.sql("SELECT MAX(version) FROM document_history WHERE document_id = :documentId")
                .bind("documentId", document.getId())
                .map(row -> row.get(0, Integer.class))
                .one()
                .defaultIfEmpty(0)
                .map(maxVersion -> DocumentHistory.builder()
                        .id(UUID.randomUUID())
                        .documentId(document.getId())
                        .content(document.getText())
                        .version(maxVersion + 1)
                        .createdAt(LocalDateTime.now())
                        .isNew(true)
                        .build())
                .flatMap(documentsHistory::save)
                .map(history -> {
                    history.setNotNew();
                    return history;
                });
    }

    private DocumentHistoryDto toHistoryDto(DocumentHistory entity) {
        return DocumentHistoryDto.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .content(entity.getContent())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public Mono<Void> deleteDocument(String id) {
        log.info("Deleting document: {}", id);
        UUID uuid = UUID.fromString(id);

        return documents.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Document", id)))
                .flatMap(document -> documentsHistory.deleteByDocumentId(uuid)
                        .then(documents.deleteById(uuid)))
                .doOnSuccess(result -> log.info("✅ Document deleted: {}", id))
                .doOnError(error -> log.error("❌ Error deleting document: {}", id, error));
    }

    public Flux<DocumentDto> searchDocuments(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getDocuments();
        }

        // Preprocess query for prefix matching:
        // 1. Split by spaces
        // 2. Add :* to each word for prefix matching (e.g., "poz" becomes "poz:*")
        // 3. Join with & for AND search
        String processedQuery = Arrays.stream(query.trim().split("\\s+"))
                .map(word -> word + ":*")
                .collect(Collectors.joining(" & "));

        log.info("Searching documents with query: {}", processedQuery);

        return documents.searchDocuments(processedQuery)
                .map(mapper::toDto)
                .doOnError(error -> log.error("❌ Error searching documents", error));
    }
}
