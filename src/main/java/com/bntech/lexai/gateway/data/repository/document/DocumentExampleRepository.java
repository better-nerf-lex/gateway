package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface DocumentExampleRepository extends R2dbcRepository<DocumentExample, UUID> {

    @Query("SELECT * FROM document_examples WHERE search_vector @@ to_tsquery('english', :query) " +
            "OR title ILIKE :plainQuery OR description ILIKE :plainQuery " +
            "ORDER BY ts_rank(search_vector, to_tsquery('english', :query)) DESC")
    Flux<DocumentExample> searchTemplates(String query, String plainQuery);
}
