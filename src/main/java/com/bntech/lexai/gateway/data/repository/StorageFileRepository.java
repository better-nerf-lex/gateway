package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface StorageFileRepository extends R2dbcRepository<StorageFile, UUID> {

    Flux<StorageFile> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM storage_files WHERE document_group_id = :documentGroupId ORDER BY created_at DESC")
    Flux<StorageFile> findByDocumentGroupIdOrderByCreatedAtDesc(UUID documentGroupId);

    @Query("SELECT * FROM storage_files WHERE team_id = :teamId ORDER BY created_at DESC")
    Flux<StorageFile> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM storage_files WHERE owner_id = :ownerId ORDER BY created_at DESC")
    Flux<StorageFile> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
