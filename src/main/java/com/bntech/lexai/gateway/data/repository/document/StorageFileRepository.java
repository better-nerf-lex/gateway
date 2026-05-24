package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface StorageFileRepository extends R2dbcRepository<StorageFile, UUID> {

    @Query("SELECT * FROM storage_files WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<StorageFile> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM storage_files WHERE document_group_id = :documentGroupId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<StorageFile> findByDocumentGroupIdOrderByCreatedAtDesc(UUID documentGroupId);

    @Query("SELECT * FROM storage_files WHERE team_id = :teamId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<StorageFile> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM storage_files WHERE owner_id = :ownerId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<StorageFile> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT * FROM storage_files WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<StorageFile> findAllActive();

    @Query("SELECT * FROM storage_files WHERE id = :id AND deleted_at IS NULL")
    Mono<StorageFile> findActiveById(UUID id);

    @Query("SELECT * FROM storage_files WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<StorageFile> findAllDeleted();

    @Query("SELECT * FROM storage_files WHERE team_id = :teamId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<StorageFile> findDeletedByTeamId(UUID teamId);

    @Modifying
    @Query("UPDATE storage_files SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE storage_files SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
