package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Event;
import com.bntech.lexai.gateway.data.entity.document.EventParticipant;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.document.EventParticipantRepository;
import com.bntech.lexai.gateway.data.repository.document.EventRepository;
import com.bntech.lexai.gateway.data.repository.document.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.security.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.mapper.document.EventMapper;
import com.bntech.lexai.gateway.mapper.document.LegalEntityMapper;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.service.security.CognitoUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventParticipantRepository eventParticipantRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final LegalEntityMapper legalEntityMapper;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CognitoUserSyncService cognitoUserSyncService;

    private Mono<User> getCurrentUser() {
        return cognitoUserSyncService.getCurrentUser()
                .onErrorResume(e -> ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .flatMap(auth -> userRepository.findByUsername(auth.getName())));
    }

    public Flux<EventDto> getCaseEvents(String caseId) {
        return eventRepository.findByCaseIdOrderByEventDateDesc(UUID.fromString(caseId))
                .flatMapSequential(this::enrichEvent);
    }

    private Mono<EventDto> enrichEvent(Event event) {
        return eventParticipantRepository.findByEventId(event.getId())
                .flatMap(participant -> legalEntityRepository.findById(participant.getLegalEntityId())
                        .map(legalEntityMapper::toDto))
                .collectList()
                .map(participants -> {
                    EventDto dto = eventMapper.toDto(event);
                    dto.setParticipants(participants);
                    return dto;
                });
    }

    public Mono<EventDto> createEvent(EventDto eventDto) {
        return getCurrentUser()
                .switchIfEmpty(Mono.error(new AccessDeniedException("User not authenticated")))
                .flatMap(user -> {
                    log.info("Creating event for user: {}, caseId: {}", user.getEmail(), eventDto.getCaseId());
                    Event event = eventMapper.toEntity(eventDto);
                    if (event.getId() == null) {
                        event.setId(UUID.randomUUID());
                    }
                    event.setOwnerId(user.getId());
                    event.setCreatedAt(LocalDateTime.now());
                    event.setVersion(1);

                    Mono<Event> eventMono;
                    if (eventDto.getTeamId() != null) {
                        log.debug("Checking team access for user {} and team {}", user.getId(), eventDto.getTeamId());
                        eventMono = teamMemberRepository
                                .findByUserIdAndTeamId(user.getId(), eventDto.getTeamId())
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.warn("❌ Team access denied for user {} in team {}", user.getId(),
                                            eventDto.getTeamId());
                                    return Mono.error(new AccessDeniedException("Insufficient team access"));
                                }))
                                .flatMap(tm -> eventRepository.save(event));
                    } else {
                        eventMono = eventRepository.save(event);
                    }

                    return eventMono.flatMap(savedEvent -> {
                        if (eventDto.getParticipants() != null && !eventDto.getParticipants().isEmpty()) {
                            return Flux.fromIterable(eventDto.getParticipants())
                                    .flatMap(p -> eventParticipantRepository.save(
                                            EventParticipant.builder()
                                                    .eventId(savedEvent.getId())
                                                    .legalEntityId(UUID.fromString(p.getId()))
                                                    .build()))
                                    .then(Mono.just(savedEvent));
                        }
                        return Mono.just(savedEvent);
                    });
                })
                .map(savedEvent -> {
                    savedEvent.setNotNew();
                    return eventMapper.toDto(savedEvent);
                })
                .flatMap(dto -> enrichEvent(eventMapper.toEntity(dto)))
                .doOnSuccess(result -> log.info("✅ Event created: {}", result.getId()))
                .doOnError(error -> log.error("❌ Error creating event", error));
    }

    public Mono<Void> addParticipant(String eventId, String legalEntityId) {
        return eventParticipantRepository
                .save(new EventParticipant(UUID.fromString(eventId), UUID.fromString(legalEntityId)))
                .then()
                .doOnSuccess(v -> log.info("✅ Participant {} added to event {}", legalEntityId, eventId));
    }

    public Mono<Void> removeParticipant(String eventId, String legalEntityId) {
        return eventParticipantRepository
                .deleteByEventIdAndLegalEntityId(UUID.fromString(eventId), UUID.fromString(legalEntityId))
                .doOnSuccess(v -> log.info("✅ Participant {} removed from event {}", legalEntityId, eventId));
    }

    public Mono<Void> deleteEvent(String id) {
        return eventRepository.softDelete(UUID.fromString(id))
                .doOnSuccess(v -> log.info("✅ Event soft deleted: {}", id));
    }

    public Flux<EventDto> getDeletedEvents(String caseId) {
        if (caseId != null) {
            return eventRepository.findDeletedByCaseId(UUID.fromString(caseId))
                    .flatMapSequential(this::enrichEvent);
        }
        return eventRepository.findAllDeleted()
                .flatMapSequential(this::enrichEvent);
    }

    public Mono<EventDto> restoreEvent(String id) {
        UUID uuid = UUID.fromString(id);
        return eventRepository.restore(uuid)
                .then(eventRepository.findById(uuid))
                .flatMap(this::enrichEvent)
                .doOnSuccess(result -> log.info("✅ Event restored: {}", id));
    }

    public Mono<Void> permanentlyDeleteEvent(String id) {
        return eventRepository.deleteById(UUID.fromString(id))
                .doOnSuccess(v -> log.info("✅ Event permanently deleted: {}", id));
    }
}
