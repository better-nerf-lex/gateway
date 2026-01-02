package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.Document;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface DocumentRepository extends R2dbcRepository<Document, UUID> {

    Flux<Document> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM documents WHERE search_vector @@ to_tsquery('english', :query) ORDER BY ts_rank(search_vector, to_tsquery('english', :query)) DESC, created_at DESC")
    Flux<Document> searchDocuments(String query);

    @Query("SELECT * FROM documents WHERE team_id = :teamId ORDER BY created_at DESC")
    Flux<Document> findByTeamIdOrderByCreatedAtDesc(UUID teamId);

    @Query("SELECT * FROM documents WHERE owner_id = :ownerId ORDER BY created_at DESC")
    Flux<Document> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT d.* FROM documents d JOIN case_documents cd ON d.id = cd.document_id WHERE cd.case_id = :caseId ORDER BY d.created_at DESC")
    Flux<Document> findByCaseId(UUID caseId);
}
