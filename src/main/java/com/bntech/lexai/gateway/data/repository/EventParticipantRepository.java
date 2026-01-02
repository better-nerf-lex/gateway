package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.EventParticipant;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface EventParticipantRepository extends R2dbcRepository<EventParticipant, UUID> {
    Flux<EventParticipant> findByEventId(UUID eventId);

    @Modifying
    @Query("DELETE FROM event_participants WHERE event_id = :eventId AND legal_entity_id = :legalEntityId")
    Mono<Void> deleteByEventIdAndLegalEntityId(UUID eventId, UUID legalEntityId);
}
