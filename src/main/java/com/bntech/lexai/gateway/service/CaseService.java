package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.entity.document.CaseDocument;
import com.bntech.lexai.gateway.data.entity.document.CaseParty;
import com.bntech.lexai.gateway.data.object.CasePartyRole;
import com.bntech.lexai.gateway.data.repository.*;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.CaseMapper;
import com.bntech.lexai.gateway.mapper.CasePartyMapper;
import com.bntech.lexai.gateway.mapper.EventMapper;
import com.bntech.lexai.gateway.mapper.LegalEntityMapper;
import com.bntech.lexai.gateway.rest.dto.document.AddPartiesRequest;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import com.bntech.lexai.gateway.rest.dto.document.CasePartyDto;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {
    private final CaseRepository caseRepository;
    private final CaseMapper caseMapper;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final DocumentRepository documentRepository;
    private final com.bntech.lexai.gateway.mapper.DocumentMapper documentMapper;
    private final CasePartyRepository casePartyRepository;
    private final CasePartyMapper casePartyMapper;
    private final LegalEntityRepository legalEntityRepository;
    private final LegalEntityMapper legalEntityMapper;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventParticipantRepository eventParticipantRepository;

    public Flux<CaseDto> getCases(String teamId) {
        if (teamId != null && !teamId.isBlank() && !teamId.equals("null") && !teamId.equals("undefined")) {
            return caseRepository.findByTeamIdOrderByCreatedAtDesc(UUID.fromString(teamId))
                    .map(caseMapper::toDto)
                    .flatMap(this::enrichCase)
                    .doOnError(error -> log.error("Error retrieving team cases for {}", teamId, error));
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> userRepository.findByUsername(auth.getName())
                        .flatMapMany(user -> {
                            Flux<LegalCase> byUser = caseRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
                            Flux<UUID> teamIds = teamMemberRepository.findByUserId(user.getId())
                                    .map(com.bntech.lexai.gateway.data.entity.security.TeamMember::getTeamId);
                            Flux<LegalCase> byTeams = teamIds.flatMap(caseRepository::findByTeamIdOrderByCreatedAtDesc);
                            return Flux.merge(byUser, byTeams);
                        }))
                .distinct(LegalCase::getId)
                .map(caseMapper::toDto)
                .flatMap(this::enrichCase)
                .doOnError(error -> log.error("Error retrieving cases", error));
    }

    public Mono<CaseDto> getCase(String id) {
        UUID uuid = UUID.fromString(id);
        return caseRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Case", id)))
                .map(caseMapper::toDto)
                .flatMap(this::enrichCase)
                .doOnError(error -> log.error("Error retrieving case: {}", id, error));
    }

    private Mono<CaseDto> enrichCase(CaseDto dto) {
        if (dto.getId() == null)
            return Mono.just(dto);
        UUID caseId = UUID.fromString(dto.getId());

        Mono<List<CasePartyDto>> partiesMono = casePartyRepository.findByCaseId(caseId)
                .flatMap(party -> legalEntityRepository.findById(party.getLegalEntityId())
                        .map(le -> {
                            CasePartyDto partyDto = casePartyMapper.toDto(party);
                            partyDto.setLegalEntity(legalEntityMapper.toDto(le));
                            return partyDto;
                        }))
                .collectList();

        Mono<List<EventDto>> eventsMono = eventRepository.findByCaseIdOrderByEventDateDesc(caseId)
                .flatMapSequential(event -> eventParticipantRepository.findByEventId(event.getId())
                        .flatMap(participant -> legalEntityRepository.findById(participant.getLegalEntityId())
                                .map(legalEntityMapper::toDto))
                        .collectList()
                        .map(participants -> {
                            EventDto eventDto = eventMapper.toDto(event);
                            eventDto.setParticipants(participants);
                            return eventDto;
                        }))
                .collectList();

        return Mono.zip(partiesMono, eventsMono)
                .map(tuple -> {
                    dto.setParties(tuple.getT1());
                    dto.setEvents(tuple.getT2());
                    return dto;
                });
    }

    public Mono<CaseDto> createCase(CaseDto caseDto) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> userRepository.findByUsername(auth.getName())
                        .flatMap(user -> {
                            LegalCase caseEntity = caseMapper.toEntity(caseDto);
                            if (caseEntity.getId() == null) {
                                caseEntity.setId(UUID.randomUUID());
                            }
                            caseEntity.setOwnerId(user.getId());
                            caseEntity.setCreatedAt(LocalDateTime.now());
                            caseEntity.setUpdatedAt(LocalDateTime.now());

                            if (caseDto.getLeadingCourtId() != null) {
                                caseEntity.setLeadingCourtId(UUID.fromString(caseDto.getLeadingCourtId()));
                            }

                            if (caseDto.getTeamId() != null) {
                                return teamMemberRepository.findByUserIdAndTeamId(user.getId(), caseDto.getTeamId())
                                        .switchIfEmpty(
                                                Mono.error(new AccessDeniedException("Insufficient team access")))
                                        .flatMap(tm -> caseRepository.save(caseEntity));
                            }

                            return caseRepository.save(caseEntity);
                        }))
                .map(savedCase -> {
                    savedCase.setNotNew();
                    return caseMapper.toDto(savedCase);
                })
                .flatMap(this::enrichCase)
                .doOnSuccess(result -> log.info("✅ LegalCase created: {}", result.getId()))
                .doOnError(error -> log.error("❌ Error creating case", error));
    }

    public Mono<CaseDto> updateCase(String id, CaseDto caseDto) {
        UUID uuid = UUID.fromString(id);
        log.debug("updateCase called - id: {}, dto: {}", id, caseDto);
        return caseRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Case", id)))
                .flatMap(existingCase -> {
                    existingCase.setTitle(caseDto.getTitle());
                    existingCase.setDescription(caseDto.getDescription());
                    existingCase.setStatus(caseDto.getStatus());
                    if (caseDto.getLeadingCourtId() != null) {
                        if (caseDto.getLeadingCourtId().isBlank()) {
                            existingCase.setLeadingCourtId(null);
                        } else {
                            existingCase.setLeadingCourtId(UUID.fromString(caseDto.getLeadingCourtId()));
                        }
                    }
                    existingCase.setUpdatedAt(LocalDateTime.now());
                    existingCase.setNotNew();
                    return caseRepository.save(existingCase);
                })
                .map(caseMapper::toDto)
                .flatMap(this::enrichCase)
                .doOnSuccess(result -> log.info("✅ LegalCase updated: {}", id))
                .doOnError(error -> log.error("❌ Error updating case: {}", id, error));
    }

    public Mono<Void> addParty(String caseId, String legalEntityId, CasePartyRole role) {
        UUID cId = UUID.fromString(caseId);
        UUID leId = UUID.fromString(legalEntityId);
        return casePartyRepository.findByCaseIdAndLegalEntityIdAndRole(cId, leId, role)
                .switchIfEmpty(casePartyRepository.save(new CaseParty(cId, leId, role)))
                .then()
                .doOnSuccess(v -> log.info("✅ Party {} added to case {} as {}", legalEntityId, caseId, role));
    }

    public Mono<Void> addParties(String caseId, List<AddPartiesRequest.PartyEntry> parties) {
        return Flux.fromIterable(parties)
                .flatMap(party -> addParty(caseId, party.getLegalEntityId(), party.getRole()))
                .then()
                .doOnSuccess(v -> log.info("✅ {} parties added to case {}", parties.size(), caseId));
    }

    public Mono<Void> removeParty(String caseId, String legalEntityId, CasePartyRole role) {
        UUID cId = UUID.fromString(caseId);
        UUID leId = UUID.fromString(legalEntityId);
        return casePartyRepository.deleteByCaseIdAndLegalEntityIdAndRole(cId, leId, role)
                .doOnSuccess(v -> log.info("✅ Party {} removed from case {} as {}", legalEntityId, caseId, role));
    }

    public Mono<Void> deleteCase(String id) {
        UUID uuid = UUID.fromString(id);
        return caseRepository.deleteById(uuid)
                .doOnSuccess(result -> log.info("✅ LegalCase deleted: {}", id))
                .doOnError(error -> log.error("❌ Error deleting case: {}", id, error));
    }

    public Mono<Void> assignDocument(String caseId, String documentId) {
        UUID caseUuid = UUID.fromString(caseId);
        UUID documentUuid = UUID.fromString(documentId);
        return caseDocumentRepository.findByCaseIdAndDocumentId(caseUuid, documentUuid)
                .switchIfEmpty(caseDocumentRepository.save(new CaseDocument(caseUuid, documentUuid)))
                .then()
                .doOnSuccess(v -> log.info("✅ Document {} assigned to case {}", documentId, caseId));
    }

    public Mono<Void> removeDocument(String caseId, String documentId) {
        UUID caseUuid = UUID.fromString(caseId);
        UUID documentUuid = UUID.fromString(documentId);
        return caseDocumentRepository.deleteByCaseIdAndDocumentId(caseUuid, documentUuid)
                .doOnSuccess(v -> log.info("✅ Document {} removed from case {}", documentId, caseId));
    }

    public Flux<com.bntech.lexai.gateway.rest.dto.document.DocumentDto> getCaseDocuments(String caseId) {
        UUID uuid = UUID.fromString(caseId);
        return documentRepository.findByCaseId(uuid)
                .map(documentMapper::toDto);
    }
}
