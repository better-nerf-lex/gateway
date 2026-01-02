package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LegalEntityRepository extends R2dbcRepository<LegalEntity, UUID> {

    @Query("SELECT * FROM legal_entities WHERE document_id = :documentId")
    Flux<LegalEntity> findByDocumentId(UUID documentId);

    @Query("SELECT * FROM legal_entities WHERE relevance = :relevance")
    Flux<LegalEntity> findByRelevance(String relevance);

    @Query("SELECT * FROM legal_entities WHERE document_id = :documentId AND relevance = :relevance")
    Flux<LegalEntity> findByDocumentIdAndRelevance(UUID documentId, String relevance);

    @Query("SELECT COUNT(*) FROM legal_entities WHERE document_id = :documentId")
    Mono<Long> countByDocumentId(UUID documentId);

    @Query("DELETE FROM legal_entities WHERE document_id = :documentId")
    Mono<Void> deleteByDocumentId(UUID documentId);

    @Query("SELECT * FROM legal_entities ORDER BY relevance")
    Flux<LegalEntity> findAllByOrderByRelevance();

    @Query("SELECT COUNT(*) FROM legal_entities")
    Mono<Long> countAll();

    @Query("SELECT * FROM legal_entities WHERE owner_id = :ownerId ORDER BY relevance")
    Flux<LegalEntity> findByOwnerId(Long ownerId);

    @Query("SELECT * FROM legal_entities WHERE team_id = :teamId ORDER BY relevance")
    Flux<LegalEntity> findByTeamId(UUID teamId);
}
