package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.Case;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface CaseRepository extends R2dbcRepository<Case, UUID> {

    @Query("SELECT * FROM cases WHERE team_id = :teamId ORDER BY created_at DESC")
    Flux<Case> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM cases WHERE owner_id = :ownerId ORDER BY created_at DESC")
    Flux<Case> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT c.* FROM cases c JOIN case_documents cd ON c.id = cd.case_id WHERE cd.document_id = :documentId")
    Flux<Case> findByDocumentId(UUID documentId);
}
