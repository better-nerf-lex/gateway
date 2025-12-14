package com.bntech.lexai.gateway.data.repository;
import com.bntech.lexai.gateway.data.entity.document.Document;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DocumentRepository extends R2dbcRepository<Document, String> {

    Flux<Document> findAllByOrderByCreatedAtDesc();
}
