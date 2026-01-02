package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.CaseDocument;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CaseDocumentRepository extends R2dbcRepository<CaseDocument, UUID> {
    Flux<CaseDocument> findAllByCaseId(UUID caseId);

    Flux<CaseDocument> findAllByDocumentId(UUID documentId);

    Mono<Void> deleteByCaseIdAndDocumentId(UUID caseId, UUID documentId);

    Mono<CaseDocument> findByCaseIdAndDocumentId(UUID caseId, UUID documentId);
}
