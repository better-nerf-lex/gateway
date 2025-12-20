package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.rest.mapper.LegalEntityMapper;
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
        if (teamId != null && !teamId.isBlank()) {
            return teamAccessService.requireAccess(teamId, com.bntech.lexai.gateway.security.access.AccessLevel.READ)
                    .thenMany(legalEntityRepository.findByTeamId(teamId))
                    .map(legalEntityMapper::toDto);
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> userRepository.findByUsername(auth.getName())
                        .flatMapMany(user -> {
                            Flux<LegalEntity> byUser = legalEntityRepository.findByUserId(String.valueOf(user.getId()));

                            Flux<String> teamIds = teamMemberRepository.findByUserId(user.getId())
                                    .map(TeamMember::getTeamId);

                            Flux<LegalEntity> byTeams = teamIds.flatMap(legalEntityRepository::findByTeamId);

                            return Flux.merge(byUser, byTeams);
                        }))
                .distinct(LegalEntity::getId)
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> getLegalEntity(String id) {
        return legalEntityRepository.findById(id)
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> createLegalEntity(LegalEntityDto legalEntityDto) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String username = auth.getName();
                    return userRepository.findByUsername(username)
                            .flatMap(user -> teamMemberRepository.findByUserId(user.getId())
                                    .next()
                                    .map(member -> member.getTeamId())
                                    .defaultIfEmpty("")
                                    .flatMap(teamId -> {
                                        LegalEntity<EntityDetails> legalEntity = legalEntityMapper
                                                .toEntity(legalEntityDto);
                                        if (legalEntity.getId() == null) {
                                            legalEntity.setId(UUID.randomUUID().toString());
                                        }
                                        legalEntity.setNew(true);
                                        legalEntity.setUserId(String.valueOf(user.getId()));
                                        if (!teamId.isEmpty()) {
                                            legalEntity.setTeamId(teamId);
                                        }

                                        return legalEntityRepository.save(legalEntity)
                                                .map(legalEntityMapper::toDto);
                                    }));
                });
    }

    public Mono<LegalEntityDto> updateLegalEntity(String id, LegalEntityDto legalEntityDto) {
        return legalEntityRepository.findById(id)
                .flatMap(existingEntity -> {
                    LegalEntity<EntityDetails> updatedEntity = legalEntityMapper.toEntity(legalEntityDto);
                    updatedEntity.setId(id);
                    updatedEntity.setNew(false);
                    if (updatedEntity.getDocumentId() == null) {
                        updatedEntity.setDocumentId(existingEntity.getDocumentId());
                    }
                    return legalEntityRepository.save(updatedEntity);
                })
                .map(legalEntityMapper::toDto);
    }

    public Mono<Void> deleteLegalEntity(String id) {
        return legalEntityRepository.deleteById(id);
    }
}
