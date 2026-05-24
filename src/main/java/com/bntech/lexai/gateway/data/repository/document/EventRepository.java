package com.bntech.lexai.gateway.data.repository.document;

import com.bntech.lexai.gateway.data.entity.document.Event;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface EventRepository extends R2dbcRepository<Event, UUID> {

    @Query("SELECT * FROM events WHERE case_id = :caseId AND deleted_at IS NULL ORDER BY event_date DESC")
    Flux<Event> findByCaseIdOrderByEventDateDesc(UUID caseId);

    @Query("SELECT e.* FROM events e JOIN event_participants ep ON e.id = ep.event_id WHERE ep.legal_entity_id = :legalEntityId AND e.deleted_at IS NULL")
    Flux<Event> findByParticipantId(UUID legalEntityId);

    @Query("SELECT * FROM events WHERE deleted_at IS NULL ORDER BY event_date DESC")
    Flux<Event> findAllActive();

    @Query("SELECT * FROM events WHERE id = :id AND deleted_at IS NULL")
    Mono<Event> findActiveById(UUID id);

    @Query("SELECT * FROM events WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<Event> findAllDeleted();

    @Query("SELECT * FROM events WHERE team_id = :teamId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<Event> findDeletedByTeamId(UUID teamId);

    @Query("SELECT * FROM events WHERE case_id = :caseId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    Flux<Event> findDeletedByCaseId(UUID caseId);

    @Modifying
    @Query("UPDATE events SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Void> softDelete(UUID id);

    @Modifying
    @Query("UPDATE events SET deleted_at = NULL WHERE id = :id")
    Mono<Void> restore(UUID id);
}
