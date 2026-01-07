package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LegalEntityRepository extends R2dbcRepository<LegalEntity<?>, UUID> {

    @Query("SELECT * FROM legal_entities WHERE document_id = :documentId AND deleted_at IS NULL")
    Flux<LegalEntity<?>> findByDocumentId(UUID documentId);

    @Query("SELECT * FROM legal_entities WHERE relevance = :relevance AND deleted_at IS NULL")
    Flux<LegalEntity<?>> findByRelevance(String relevance);

    @Query("SELECT * FROM legal_entities WHERE document_id = :documentId AND relevance = :relevance AND deleted_at IS NULL")
    Flux<LegalEntity<?>> findByDocumentIdAndRelevance(UUID documentId, String relevance);

    @Query("SELECT COUNT(*) FROM legal_entities WHERE document_id = :documentId AND deleted_at IS NULL")
    Mono<Long> countByDocumentId(UUID documentId);

    @Query("DELETE FROM legal_entities WHERE document_id = :documentId")
    Mono<Void> deleteByDocumentId(UUID documentId);

    @Query("SELECT * FROM legal_entities WHERE deleted_at IS NULL ORDER BY relevance")
    Flux<LegalEntity<?>> findAllByOrderByRelevance();

    @Query("SELECT COUNT(*) FROM legal_entities WHERE deleted_at IS NULL")
    Mono<Long> countAll();

    @Query("SELECT * FROM legal_entities WHERE owner_id = :ownerId AND deleted_at IS NULL ORDER BY relevance")
    Flux<LegalEntity<?>> findByOwnerId(Long ownerId);

    @Query("SELECT * FROM legal_entities WHERE team_id = :teamId AND deleted_at IS NULL ORDER BY relevance")
    Flux<LegalEntity<?>> findByTeamId(UUID teamId);

    @Query("SELECT * FROM legal_entities WHERE (owner_id = :ownerId OR team_id IN (SELECT team_id FROM team_members WHERE user_id = :ownerId)) "
            +
            "AND deleted_at IS NULL AND (details->>'firstName' ILIKE :query OR details->>'lastName' ILIKE :query OR details->>'fullName' ILIKE :query) "
            +
            "ORDER BY relevance")
    Flux<LegalEntity<?>> search(Long ownerId, String query);

    @Query("SELECT * FROM legal_entities WHERE deleted_at IS NULL ORDER BY relevance")
    Flux<LegalEntity<?>> findAllActive();

    @Query("SELECT * FROM legal_entities WHERE id = :id AND deleted_at IS NULL")
    Mono<LegalEntity<?>> findActiveById(UUID id);

    @Query("SELECT * FROM legal_entities WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<LegalEntity<?>> findAllDeleted();

    @Query("SELECT * FROM legal_entities WHERE team_id = :teamId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<LegalEntity<?>> findDeletedByTeamId(UUID teamId);

    @Modifying
    @Query("UPDATE legal_entities SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE legal_entities SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
