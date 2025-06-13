package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.object.DocumentStatus;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.mapper.DocumentMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final AIService aiService;

    public Flux<DocumentDto> getDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .map(documentMapper::toDto)
                .doOnError(error -> log.error("Error retrieving documents", error));
    }

    public Mono<DocumentStatsDto> getDocumentStats() {
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();

        return Mono.zip(
                        documentRepository.countAll(),
                        documentRepository.countThisMonth(startOfMonth),
                        documentRepository.countPending(),
                        documentRepository.countCompleted()
                ).map(tuple -> DocumentStatsDto.builder()
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
                    //send response to front
                })
                .doOnError(error -> log.error("❌ Error analyzing prompt", error));
    }

    public Mono<Void> generateDocument(GenerateDocumentRequest request) {
        log.info("Generating document with request: {}", request);

        Document document = documentMapper.toEntity(DocumentDto.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle())
                .type(null) // No predefined type, it's prompt-based
                .status(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .prompt(request.getPrompt())
                .build());

        return documentRepository.save(document)
                .doOnNext(Document::setNotNew)
                .flatMap(savedDocument -> processAIGeneration(savedDocument, request));
    }

    private Mono<Void> processAIGeneration(Document document, GenerateDocumentRequest request) {
        return aiService.generateDocumentWithAnswers(
                        request.getTitle(),
                        request.getPrompt(),
                        request.getAnswers(),
                        request.getRequirements(),
                        request.getBase()
                )
                .map(content -> {
                    log.info("Generated document content: {}", content);
                    return content;
                })
                .flatMap(content -> updateDocumentWithContent(document, content))
                .doOnSuccess(result -> log.info("✅ AI generation completed for: {}", document.getTitle()))
                .doOnError(error -> log.error("❌ AI generation failed for: {}", document.getTitle(), error))
                .onErrorResume(error -> failDocument(document))
                .then();
    }

    private Mono<Document> updateDocumentWithContent(Document document, String content) {
        return documentRepository.findById(document.getId())
                .map(existingDoc -> {
                    existingDoc.setStatus(DocumentStatus.COMPLETED);
                    existingDoc.setUpdatedAt(LocalDateTime.now());
                    existingDoc.setNotNew();
                    return existingDoc;
                })
                .switchIfEmpty(Mono.just(document)
                        .map(doc -> {
                            doc.setStatus(DocumentStatus.COMPLETED);
                            doc.setUpdatedAt(LocalDateTime.now());
                            return doc;
                        }))
                .flatMap(documentRepository::save);
    }

    private Mono<Document> failDocument(Document document) {
        log.error("Failing document: {}", document.getTitle());
        return documentRepository.findById(document.getId())
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
                .flatMap(documentRepository::save);
    }
}
