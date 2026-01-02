package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.Event;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface EventRepository extends R2dbcRepository<Event, UUID> {
    Flux<Event> findByCaseIdOrderByEventDateDesc(UUID caseId);

    @Query("SELECT e.* FROM events e JOIN event_participants ep ON e.id = ep.event_id WHERE ep.legal_entity_id = :legalEntityId")
    Flux<Event> findByParticipantId(UUID legalEntityId);
}
