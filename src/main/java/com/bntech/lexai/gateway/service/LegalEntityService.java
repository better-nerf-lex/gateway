package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.mapper.LegalEntityMapper;
import com.bntech.lexai.gateway.data.object.ActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;
    private final LegalEntityMapper legalEntityMapper;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ActivityService activityService;
    private final CognitoUserSyncService cognitoUserSyncService;

    private Mono<User> getCurrentUser() {
        return cognitoUserSyncService.getCurrentUser()
                .onErrorResume(e -> ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .flatMap(auth -> userRepository.findByUsername(auth.getName())));
    }

    public Flux<LegalEntityDto> getAllLegalEntities() {
        return getAllLegalEntities(null);
    }

    public Flux<LegalEntityDto> getAllLegalEntities(String teamId) {
        if (teamId != null && !teamId.isBlank() && !teamId.equals("null") && !teamId.equals("undefined")) {
            return legalEntityRepository.findByTeamId(UUID.fromString(teamId))
                    .map(legalEntityMapper::toDto);
        }
        return getCurrentUser()
                .flatMapMany(user -> {
                    Flux<LegalEntity<?>> byUser = legalEntityRepository.findByOwnerId(user.getId());
                    Flux<UUID> teamIds = teamMemberRepository.findByUserId(user.getId())
                            .map(TeamMember::getTeamId);
                    Flux<LegalEntity<?>> byTeams = teamIds.flatMap(legalEntityRepository::findByTeamId);
                    return Flux.merge(byUser, byTeams);
                })
                .distinct(LegalEntity::getId)
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> getLegalEntity(String id) {
        return legalEntityRepository.findById(UUID.fromString(id))
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> createLegalEntity(LegalEntityDto legalEntityDto) {
        return getCurrentUser()
                .flatMap(user -> teamMemberRepository.findByUserId(user.getId())
                        .collectList()
                        .flatMap(members -> {
                            UUID targetTeamId = null;

                            if (legalEntityDto.getTeamId() != null
                                    && !legalEntityDto.getTeamId().isBlank()
                                    && !legalEntityDto.getTeamId().equals("null")
                                    && !legalEntityDto.getTeamId().equals("undefined")) {
                                UUID requestedTeamId = UUID.fromString(legalEntityDto.getTeamId());
                                boolean isMember = members.stream()
                                        .anyMatch(m -> m.getTeamId().equals(requestedTeamId));
                                if (isMember) {
                                    targetTeamId = requestedTeamId;
                                }
                            }

                            if (targetTeamId == null && !members.isEmpty()) {
                                targetTeamId = members.get(0).getTeamId();
                            }

                            LegalEntity<EntityDetails> legalEntity = legalEntityMapper.toEntity(legalEntityDto);
                            if (legalEntity.getId() == null) {
                                legalEntity.setId(UUID.randomUUID());
                            }
                            legalEntity.setNew(true);
                            legalEntity.setOwnerId(user.getId());
                            if (targetTeamId != null) {
                                legalEntity.setTeamId(targetTeamId);
                            }

                            return legalEntityRepository.save(legalEntity)
                                    .doOnSuccess(saved -> {
                                        if (saved.getTeamId() != null) {
                                            activityService.logActivity(
                                                    saved.getOwnerId(),
                                                    saved.getTeamId(),
                                                    ActionType.ENTITY_NEW,
                                                    saved.getId(),
                                                    "LegalEntity",
                                                    "Created entity: " + legalEntityMapper.mapName(saved.getDetails()));
                                        }
                                    })
                                    .map(legalEntityMapper::toDto);
                        }));
    }

    public Mono<LegalEntityDto> updateLegalEntity(String id, LegalEntityDto legalEntityDto) {
        UUID uuid = UUID.fromString(id);
        return legalEntityRepository.findById(uuid)
                .flatMap(entity -> {
                    @SuppressWarnings("unchecked")
                    LegalEntity<EntityDetails> existingEntity = (LegalEntity<EntityDetails>) entity;
                    if (legalEntityDto.getRelevance() != null) {
                        existingEntity.setRelevance(legalEntityMapper.mapRelevance(legalEntityDto.getRelevance()));
                    }
                    if (legalEntityDto.getDetails() != null) {
                        existingEntity.setDetails(legalEntityMapper.mapDetailsToEntity(legalEntityDto.getDetails()));
                    }
                    if (legalEntityDto.getTeamId() != null) {
                        existingEntity.setTeamId(UUID.fromString(legalEntityDto.getTeamId()));
                    }

                    existingEntity.setNotNew();

                    return legalEntityRepository.save(existingEntity)
                            .doOnSuccess(saved -> {
                                if (saved.getTeamId() != null) {
                                    activityService.logActivity(
                                            saved.getOwnerId(),
                                            saved.getTeamId(),
                                            ActionType.ENTITY_UPDATE,
                                            saved.getId(),
                                            "LegalEntity",
                                            "Updated entity: " + legalEntityMapper.mapName(saved.getDetails()));
                                }
                            });
                })
                .map(legalEntityMapper::toDto);
    }

    public Mono<Void> deleteLegalEntity(String id) {
        UUID uuid = UUID.fromString(id);
        return legalEntityRepository.findById(uuid)
                .switchIfEmpty(
                        Mono.error(new com.bntech.lexai.gateway.exception.ResourceNotFoundException("LegalEntity", id)))
                .flatMap(e -> legalEntityRepository.softDelete(uuid)
                        .doOnSuccess(unused -> {
                            if (e.getTeamId() != null) {
                                activityService.logActivity(
                                        e.getOwnerId(),
                                        e.getTeamId(),
                                        ActionType.ENTITY_DELETE,
                                        e.getId(),
                                        "LegalEntity",
                                        "Deleted entity: " + legalEntityMapper.mapName(e.getDetails()));
                            }
                        }))
                .doOnSuccess(v -> log.info("✅ LegalEntity soft deleted: {}", id));
    }

    public Flux<LegalEntityDto> getDeletedLegalEntities(String teamId) {
        if (teamId != null) {
            return legalEntityRepository.findDeletedByTeamId(UUID.fromString(teamId))
                    .map(legalEntityMapper::toDto);
        }
        return legalEntityRepository.findAllDeleted()
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> restoreLegalEntity(String id) {
        UUID uuid = UUID.fromString(id);
        return legalEntityRepository.restore(uuid)
                .then(legalEntityRepository.findById(uuid))
                .flatMap(restored -> {
                    restored.setNotNew();
                    if (restored.getTeamId() != null) {
                        activityService.logActivity(
                                restored.getOwnerId(),
                                restored.getTeamId(),
                                ActionType.ENTITY_RESTORE,
                                restored.getId(),
                                "LegalEntity",
                                "Restored entity: " + legalEntityMapper.mapName(restored.getDetails()));
                    }
                    return Mono.just(legalEntityMapper.toDto(restored));
                })
                .doOnSuccess(result -> log.info("✅ LegalEntity restored: {}", id));
    }

    public Mono<Void> permanentlyDeleteLegalEntity(String id) {
        return legalEntityRepository.deleteById(UUID.fromString(id))
                .doOnSuccess(v -> log.info("✅ LegalEntity permanently deleted: {}", id));
    }

    public Flux<LegalEntityDto> searchLegalEntities(String query) {
        return getCurrentUser()
                .flatMapMany(user -> legalEntityRepository.search(user.getId(), "%" + query + "%")
                        .map(legalEntityMapper::toDto));
    }
}
