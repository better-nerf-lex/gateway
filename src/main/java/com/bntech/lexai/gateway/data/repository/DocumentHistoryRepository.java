package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.DocumentHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DocumentHistoryRepository extends ReactiveCrudRepository<DocumentHistory, String> {

    @Query("SELECT * FROM document_history WHERE document_id = :documentId ORDER BY version DESC")
    Flux<DocumentHistory> findByDocumentIdOrderByVersionDesc(String documentId);

    @Query("SELECT MAX(version) FROM document_history WHERE document_id = :documentId")
    Mono<Integer> findMaxVersionByDocumentId(String documentId);
}
