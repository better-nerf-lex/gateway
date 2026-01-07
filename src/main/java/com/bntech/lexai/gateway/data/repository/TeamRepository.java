package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.security.Team;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface TeamRepository extends R2dbcRepository<Team, UUID> {

    @Query("SELECT * FROM teams WHERE deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Team> findAllActive();

    @Query("SELECT * FROM teams WHERE id = :id AND deleted_at IS NULL")
    Mono<Team> findActiveById(UUID id);

    @Query("SELECT * FROM teams WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<Team> findAllDeleted();

    @Modifying
    @Query("UPDATE teams SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE teams SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
