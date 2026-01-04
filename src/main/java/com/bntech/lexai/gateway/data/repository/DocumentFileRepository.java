package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.DocumentFile;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface DocumentFileRepository extends R2dbcRepository<DocumentFile, UUID> {

    Flux<DocumentFile> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM document_files WHERE document_group_id = :documentGroupId ORDER BY created_at DESC")
    Flux<DocumentFile> findByDocumentGroupIdOrderByCreatedAtDesc(UUID documentGroupId);

    @Query("SELECT * FROM document_files WHERE team_id = :teamId ORDER BY created_at DESC")
    Flux<DocumentFile> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM document_files WHERE owner_id = :ownerId ORDER BY created_at DESC")
    Flux<DocumentFile> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
