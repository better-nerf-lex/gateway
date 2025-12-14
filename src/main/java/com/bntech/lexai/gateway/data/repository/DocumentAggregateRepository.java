package com.bntech.lexai.gateway.data.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Repository
public class DocumentAggregateRepository {

    private final DatabaseClient databaseClient;

    public Mono<Long> countAll() {
        return databaseClient.sql("SELECT COUNT(*) FROM documents")
                .map(row -> row.get(0, Long.class))
                .one();
    }

    public Mono<Long> countThisMonth(LocalDateTime startOfMonth) {
        return databaseClient.sql("SELECT COUNT(*) FROM documents WHERE created_at >= :startOfMonth")
                .bind("startOfMonth", startOfMonth)
                .map(row -> row.get(0, Long.class))
                .one();
    }

    public Mono<Long> countPending() {
        return databaseClient.sql("SELECT COUNT(*) FROM documents WHERE status = 'PENDING'")
                .map(row -> row.get(0, Long.class))
                .one();
    }

    public Mono<Long> countCompleted() {
        return databaseClient.sql("SELECT COUNT(*) FROM documents WHERE status = 'COMPLETED'")
                .map(row -> row.get(0, Long.class))
                .one();
    }
}
