package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CaseRepository extends R2dbcRepository<LegalCase, UUID> {

    @Query("SELECT * FROM cases WHERE team_id = :teamId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<LegalCase> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM cases WHERE owner_id = :ownerId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<LegalCase> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT c.* FROM cases c JOIN case_documents cd ON c.id = cd.case_id WHERE cd.document_id = :documentId AND c.deleted_at IS NULL")
    Flux<LegalCase> findByDocumentId(UUID documentId);

    @Query("SELECT * FROM cases WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<LegalCase> findAllActive();

    @Query("SELECT * FROM cases WHERE id = :id AND deleted_at IS NULL")
    Mono<LegalCase> findActiveById(UUID id);

    @Query("SELECT * FROM cases WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<LegalCase> findAllDeleted();

    @Query("SELECT * FROM cases WHERE team_id = :teamId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<LegalCase> findDeletedByTeamId(UUID teamId);

    @Modifying
    @Query("UPDATE cases SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE cases SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
