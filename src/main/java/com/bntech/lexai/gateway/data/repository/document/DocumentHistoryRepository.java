package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentHistory;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DocumentHistoryRepository extends ReactiveCrudRepository<DocumentHistory, UUID> {

    @Query("SELECT * FROM document_history WHERE document_id = :documentId ORDER BY history_version DESC")
    Flux<DocumentHistory> findByDocumentIdOrderByVersionDesc(UUID documentId);

    @Query("DELETE FROM document_history WHERE document_id = :documentId")
    Mono<Void> deleteByDocumentId(UUID documentId);

    @Query("SELECT COALESCE(MAX(history_version), 0) as value FROM document_history WHERE document_id = :documentId")
    Mono<ScalarResult> findMaxHistoryVersionByDocumentId(UUID documentId);

    @Modifying
    @Query("DELETE FROM document_history WHERE id IN (SELECT id FROM document_history WHERE document_id = :documentId ORDER BY history_version DESC OFFSET 10)")
    Mono<Void> deleteOldVersions(UUID documentId);
}
