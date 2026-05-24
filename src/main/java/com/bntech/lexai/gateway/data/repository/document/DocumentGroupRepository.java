package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DocumentGroupRepository extends R2dbcRepository<DocumentGroup, UUID> {

    @Query("SELECT * FROM document_groups WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<DocumentGroup> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM document_groups WHERE team_id = :teamId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<DocumentGroup> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM document_groups WHERE owner_id = :ownerId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<DocumentGroup> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT * FROM document_groups WHERE case_id = :caseId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<DocumentGroup> findByCaseIdOrderByCreatedAtDesc(UUID caseId);

    @Query("SELECT * FROM document_groups WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<DocumentGroup> findAllActive();

    @Query("SELECT * FROM document_groups WHERE id = :id AND deleted_at IS NULL")
    Mono<DocumentGroup> findActiveById(UUID id);

    @Query("SELECT * FROM document_groups WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<DocumentGroup> findAllDeleted();

    @Query("SELECT * FROM document_groups WHERE team_id = :teamId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<DocumentGroup> findDeletedByTeamId(UUID teamId);

    @Modifying
    @Query("UPDATE document_groups SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE document_groups SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
