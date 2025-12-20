package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LegalEntityRepository extends R2dbcRepository<LegalEntity, String> {

    @Query("SELECT * FROM legal_entities WHERE document_id = :documentId")
    Flux<LegalEntity> findByDocumentId(String documentId);

    @Query("SELECT * FROM legal_entities WHERE relevance = :relevance")
    Flux<LegalEntity> findByRelevance(String relevance);

    @Query("SELECT * FROM legal_entities WHERE document_id = :documentId AND relevance = :relevance")
    Flux<LegalEntity> findByDocumentIdAndRelevance(String documentId, String relevance);

    @Query("SELECT COUNT(*) FROM legal_entities WHERE document_id = :documentId")
    Mono<Long> countByDocumentId(String documentId);

    @Query("DELETE FROM legal_entities WHERE document_id = :documentId")
    Mono<Void> deleteByDocumentId(String documentId);

    @Query("SELECT * FROM legal_entities ORDER BY relevance")
    Flux<LegalEntity> findAllByOrderByRelevance();

    @Query("SELECT COUNT(*) FROM legal_entities")
    Mono<Long> countAll();

    @Query("SELECT * FROM legal_entities WHERE user_id = :userId ORDER BY relevance")
    Flux<LegalEntity> findByUserId(String userId);

    @Query("SELECT * FROM legal_entities WHERE team_id = :teamId ORDER BY relevance")
    Flux<LegalEntity> findByTeamId(String teamId);
}
