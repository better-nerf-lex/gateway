package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface DocumentGroupRepository extends R2dbcRepository<DocumentGroup, UUID> {

    Flux<DocumentGroup> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM document_groups WHERE team_id = :teamId ORDER BY created_at DESC")
    Flux<DocumentGroup> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM document_groups WHERE owner_id = :ownerId ORDER BY created_at DESC")
    Flux<DocumentGroup> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
