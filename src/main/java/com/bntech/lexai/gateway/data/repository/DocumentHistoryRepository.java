package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.DocumentHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DocumentHistoryRepository extends ReactiveCrudRepository<DocumentHistory, UUID> {

    @Query("SELECT * FROM document_history WHERE document_id = :documentId ORDER BY version DESC")
    Flux<DocumentHistory> findByDocumentIdOrderByVersionDesc(UUID documentId);

    @Query("SELECT MAX(version) FROM document_history WHERE document_id = :documentId")
    Mono<Integer> findMaxVersionByDocumentId(UUID documentId);

    @Query("DELETE FROM document_history WHERE document_id = :documentId")
    Mono<Void> deleteByDocumentId(UUID documentId);
}
