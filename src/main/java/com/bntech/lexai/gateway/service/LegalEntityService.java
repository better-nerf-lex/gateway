package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.mapper.LegalEntityMapper;
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

    public Flux<LegalEntityDto> getAllLegalEntities() {
        return getAllLegalEntities(null);
    }

    public Flux<LegalEntityDto> getAllLegalEntities(String teamId) {
        if (teamId != null && !teamId.isBlank() && !teamId.equals("null") && !teamId.equals("undefined")) {
            return legalEntityRepository.findByTeamId(UUID.fromString(teamId))
                    .map(legalEntityMapper::toDto);
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> userRepository.findByUsername(auth.getName())
                        .flatMapMany(user -> {
                            Flux<LegalEntity<?>> byUser = legalEntityRepository.findByOwnerId(user.getId());

                            Flux<UUID> teamIds = teamMemberRepository.findByUserId(user.getId())
                                    .map(TeamMember::getTeamId);

                            Flux<LegalEntity<?>> byTeams = teamIds.flatMap(legalEntityRepository::findByTeamId);

                            return Flux.merge(byUser, byTeams);
                        }))
                .distinct(LegalEntity::getId)
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> getLegalEntity(String id) {
        return legalEntityRepository.findById(UUID.fromString(id))
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> createLegalEntity(LegalEntityDto legalEntityDto) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String username = auth.getName();
                    return userRepository.findByUsername(username)
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

                                        LegalEntity<EntityDetails> legalEntity = legalEntityMapper
                                                .toEntity(legalEntityDto);
                                        if (legalEntity.getId() == null) {
                                            legalEntity.setId(UUID.randomUUID());
                                        }
                                        legalEntity.setNew(true);
                                        legalEntity.setOwnerId(user.getId());
                                        if (targetTeamId != null) {
                                            legalEntity.setTeamId(targetTeamId);
                                        }

                                        return legalEntityRepository.save(legalEntity)
                                                .map(legalEntityMapper::toDto);
                                    }));
                });
    }

    public Mono<LegalEntityDto> updateLegalEntity(String id, LegalEntityDto legalEntityDto) {
        UUID uuid = UUID.fromString(id);
        return legalEntityRepository.findById(uuid)
                .flatMap(existingEntity -> {
                    LegalEntity<EntityDetails> updatedEntity = legalEntityMapper.toEntity(legalEntityDto);
                    updatedEntity.setId(uuid);
                    updatedEntity.setNotNew();

                    return legalEntityRepository.save(updatedEntity);
                })
                .map(legalEntityMapper::toDto);
    }

    public Mono<Void> deleteLegalEntity(String id) {
        return legalEntityRepository.softDelete(UUID.fromString(id))
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
                .map(restored -> {
                    restored.setNotNew();
                    return legalEntityMapper.toDto(restored);
                })
                .doOnSuccess(result -> log.info("✅ LegalEntity restored: {}", id));
    }

    public Mono<Void> permanentlyDeleteLegalEntity(String id) {
        return legalEntityRepository.deleteById(UUID.fromString(id))
                .doOnSuccess(v -> log.info("✅ LegalEntity permanently deleted: {}", id));
    }

    public Flux<LegalEntityDto> searchLegalEntities(String query) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> userRepository.findByUsername(auth.getName())
                        .flatMapMany(user -> legalEntityRepository.search(user.getId(), "%" + query + "%")
                                .map(legalEntityMapper::toDto)));
    }
}
