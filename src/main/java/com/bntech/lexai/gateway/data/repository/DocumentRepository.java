package com.bntech.lexai.gateway.data.repository;
import com.bntech.lexai.gateway.data.entity.document.Document;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface DocumentRepository extends R2dbcRepository<Document, String> {

    @Query("SELECT COUNT(d) FROM documents d")
    Mono<Long> countAll();

    @Query("SELECT COUNT(d) FROM documents d WHERE d.created_at >= :startOfMonth")
    Mono<Long> countThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(d) FROM documents d WHERE d.status = 'PENDING'")
    Mono<Long> countPending();

    @Query("SELECT COUNT(d) FROM documents d WHERE d.status = 'COMPLETED'")
    Mono<Long> countCompleted();

    Flux<Document> findAllByOrderByCreatedAtDesc();
}
