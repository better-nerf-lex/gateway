package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.DocumentHistory;
import com.bntech.lexai.gateway.data.object.DocumentStatus;
import com.bntech.lexai.gateway.data.repository.DocumentAggregateRepository;
import com.bntech.lexai.gateway.data.repository.DocumentHistoryRepository;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.mapper.DocumentMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentHistoryDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    private final DocumentRepository documents;
    private final DocumentAggregateRepository aggregates;
    private final DocumentHistoryRepository documentsHistory;
    private final DocumentMapper mapper;
    private final AIService aiService;

    public Flux<DocumentDto> getDocuments() {
        return documents.findAllByOrderByCreatedAtDesc()
                .map(mapper::toDto)
                .doOnError(error -> log.error("Error retrieving documents", error));
    }

    public Mono<DocumentDto> getDocument(String id) {
        return documents.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Document not found: " + id)))
                .map(mapper::toDto)
                .doOnError(error -> log.error("Error retrieving document: {}", id, error));
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

        return aiService.analyzePrompt(request.getPrompt())
                .doOnSuccess(response -> {
                    log.info("✅ Prompt analysis completed {}", response);
                    // send response to front
                })
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<DocumentDto> generateDocument(GenerateDocumentRequest request) {
        log.info("Generating document with request: {}", request);

        Document document = mapper.toEntity(DocumentDto.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle())
                .type(null) // No predefined type, it's prompt-based
                .status(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .text("")
                .build());

        return documents.save(document)
                .doOnNext(Document::setNotNew)
                .flatMap(savedDocument -> processAIGeneration(savedDocument, request));
    }

    private Mono<DocumentDto> processAIGeneration(Document document, GenerateDocumentRequest request) {
        return aiService.generateDocumentWithAnswers(
                request.getTitle(),
                request.getPrompt(),
                request.getAnswers(),
                request.getRequirements(),
                request.getBase())
                .map(content -> {
                    log.info("Generated document content: {}", content);
                    return content;
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

        return documents.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Document not found: " + id)))
                .flatMap(document -> {
                    // Create history entry before updating
                    return createHistoryEntry(document)
                            .then(Mono.just(document));
                })
                .flatMap(document -> {
                    // Update document with new content
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

        return documentsHistory.findByDocumentIdOrderByVersionDesc(documentId)
                .map(this::toHistoryDto)
                .doOnError(error -> log.error("Error fetching document history", error));
    }

    public Mono<DocumentDto> restoreDocumentVersion(String documentId, String versionId) {
        log.info("Restoring document {} to version {}", documentId, versionId);

        return documentsHistory.findById(versionId)
                .switchIfEmpty(Mono.error(new RuntimeException("Version not found: " + versionId)))
                .flatMap(history -> updateDocument(documentId, history.getContent()))
                .doOnSuccess(result -> log.info("✅ Document restored to version: {}", versionId))
                .doOnError(error -> log.error("❌ Error restoring document version", error));
    }

    private Mono<DocumentHistory> createHistoryEntry(Document document) {
        return documentsHistory.findMaxVersionByDocumentId(document.getId())
                .defaultIfEmpty(0)
                .map(maxVersion -> DocumentHistory.builder()
                        .id(UUID.randomUUID().toString())
                        .documentId(document.getId())
                        .content(document.getText())
                        .version(maxVersion + 1)
                        .createdAt(LocalDateTime.now())
                        .createdBy("system") // TODO: Get from security context
                        .build())
                .flatMap(documentsHistory::save)
                .doOnNext(DocumentHistory::setNotNew);
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

        return documents.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Document not found: " + id)))
                .flatMap(document -> documentsHistory.deleteByDocumentId(id)
                        .then(documents.deleteById(id)))
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
