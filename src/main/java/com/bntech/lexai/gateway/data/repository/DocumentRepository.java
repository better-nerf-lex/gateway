package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.Document;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DocumentRepository extends R2dbcRepository<Document, String> {

    Flux<Document> findAllByOrderByCreatedAtDesc();

    @Query("SELECT * FROM documents WHERE search_vector @@ to_tsquery('english', :query) ORDER BY ts_rank(search_vector, to_tsquery('english', :query)) DESC, created_at DESC")
    Flux<Document> searchDocuments(String query);
}
