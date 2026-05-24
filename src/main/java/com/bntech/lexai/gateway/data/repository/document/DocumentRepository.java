package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.Document;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DocumentRepository extends R2dbcRepository<Document, UUID> {

    @Query("SELECT * FROM documents WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Document> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM documents WHERE deleted_at IS NULL AND search_vector @@ to_tsquery('english', :query) ORDER BY ts_rank(search_vector, to_tsquery('english', :query)) DESC, created_at DESC")
    Flux<Document> searchDocuments(String query);

    @Query("SELECT * FROM documents WHERE team_id = :teamId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Document> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM documents WHERE owner_id = :ownerId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Document> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT d.* FROM documents d JOIN case_documents cd ON d.id = cd.document_id WHERE cd.case_id = :caseId AND d.deleted_at IS NULL ORDER BY d.created_at DESC")
    Flux<Document> findByCaseId(UUID caseId);

    @Query("SELECT * FROM documents WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Document> findAllActive();

    @Query("SELECT * FROM documents WHERE id = :id AND deleted_at IS NULL")
    Mono<Document> findActiveById(UUID id);

    @Query("SELECT * FROM documents WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<Document> findAllDeleted();

    @Query("SELECT * FROM documents WHERE team_id = :teamId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<Document> findDeletedByTeamId(UUID teamId);

    @Modifying
    @Query("UPDATE documents SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE documents SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
