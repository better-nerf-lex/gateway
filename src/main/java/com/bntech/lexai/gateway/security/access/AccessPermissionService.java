package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.repository.document.CaseRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.document.EventRepository;
import com.bntech.lexai.gateway.data.repository.document.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.document.StorageFileRepository;
import com.bntech.lexai.gateway.data.repository.security.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.data.entity.OwnableEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component("access")
@RequiredArgsConstructor
public class AccessPermissionService {

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CaseRepository caseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentGroupRepository documentGroupRepository;
    private final EventRepository eventRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final StorageFileRepository storageFileRepository;

    public Mono<Boolean> teamRead(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (isBlankTeamId(teamId)) {
            return Mono.just(true);
        }
        try {
            return evaluate(authentication, UUID.fromString(teamId), AccessLevel.READ.name());
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> teamAdmin(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (isBlankTeamId(teamId)) {
            return Mono.just(true);
        }
        try {
            return evaluate(authentication, UUID.fromString(teamId), AccessLevel.ADMIN.name());
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> teamWrite(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (isBlankTeamId(teamId)) {
            return Mono.just(true);
        }
        try {
            return evaluate(authentication, UUID.fromString(teamId), AccessLevel.WRITE.name());
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> caseRead(Authentication authentication, String caseId) {
        return fetchTeamIdAndEvaluate(authentication, caseId, caseRepository, AccessLevel.READ);
    }

    public Mono<Boolean> caseWrite(Authentication authentication, String caseId) {
        return fetchTeamIdAndEvaluate(authentication, caseId, caseRepository, AccessLevel.WRITE);
    }

    public Mono<Boolean> caseAdmin(Authentication authentication, String caseId) {
        return fetchTeamIdAndEvaluate(authentication, caseId, caseRepository, AccessLevel.ADMIN);
    }

    public Mono<Boolean> documentRead(Authentication authentication, String documentId) {
        return fetchTeamIdAndEvaluate(authentication, documentId, documentRepository, AccessLevel.READ);
    }

    public Mono<Boolean> documentWrite(Authentication authentication, String documentId) {
        return fetchTeamIdAndEvaluate(authentication, documentId, documentRepository, AccessLevel.WRITE);
    }

    public Mono<Boolean> documentAdmin(Authentication authentication, String documentId) {
        return fetchTeamIdAndEvaluate(authentication, documentId, documentRepository, AccessLevel.ADMIN);
    }

    public Mono<Boolean> documentGroupRead(Authentication authentication, String documentGroupId) {
        return fetchTeamIdAndEvaluate(authentication, documentGroupId, documentGroupRepository, AccessLevel.READ);
    }

    public Mono<Boolean> documentGroupWrite(Authentication authentication, String documentGroupId) {
        return fetchTeamIdAndEvaluate(authentication, documentGroupId, documentGroupRepository, AccessLevel.WRITE);
    }

    public Mono<Boolean> documentGroupAdmin(Authentication authentication, String documentGroupId) {
        return fetchTeamIdAndEvaluate(authentication, documentGroupId, documentGroupRepository, AccessLevel.ADMIN);
    }

    public Mono<Boolean> eventRead(Authentication authentication, String eventId) {
        return fetchTeamIdAndEvaluate(authentication, eventId, eventRepository, AccessLevel.READ);
    }

    public Mono<Boolean> eventWrite(Authentication authentication, String eventId) {
        return fetchTeamIdAndEvaluate(authentication, eventId, eventRepository, AccessLevel.WRITE);
    }

    public Mono<Boolean> eventAdmin(Authentication authentication, String eventId) {
        return fetchTeamIdAndEvaluate(authentication, eventId, eventRepository, AccessLevel.ADMIN);
    }

    public Mono<Boolean> legalEntityRead(Authentication authentication, String legalEntityId) {
        return fetchTeamIdAndEvaluate(authentication, legalEntityId, legalEntityRepository, AccessLevel.READ);
    }

    public Mono<Boolean> legalEntityWrite(Authentication authentication, String legalEntityId) {
        return fetchTeamIdAndEvaluate(authentication, legalEntityId, legalEntityRepository, AccessLevel.WRITE);
    }

    public Mono<Boolean> legalEntityAdmin(Authentication authentication, String legalEntityId) {
        return fetchTeamIdAndEvaluate(authentication, legalEntityId, legalEntityRepository, AccessLevel.ADMIN);
    }

    public Mono<Boolean> storageFileRead(Authentication authentication, String storageFileId) {
        return fetchTeamIdAndEvaluate(authentication, storageFileId, storageFileRepository, AccessLevel.READ);
    }

    public Mono<Boolean> storageFilesRead(Authentication authentication, List<String> storageFileIds) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (storageFileIds == null || storageFileIds.isEmpty()) {
            return Mono.just(true);
        }
        return Flux.fromIterable(storageFileIds)
                .concatMap(storageFileId -> storageFileRead(authentication, storageFileId))
                .all(Boolean.TRUE::equals);
    }

    public Mono<Boolean> storageFileWrite(Authentication authentication, String storageFileId) {
        return fetchTeamIdAndEvaluate(authentication, storageFileId, storageFileRepository, AccessLevel.WRITE);
    }

    public Mono<Boolean> storageFileAdmin(Authentication authentication, String storageFileId) {
        return fetchTeamIdAndEvaluate(authentication, storageFileId, storageFileRepository, AccessLevel.ADMIN);
    }

    private <T> Mono<Boolean> fetchTeamIdAndEvaluate(Authentication authentication, String entityId, 
                                                       org.springframework.data.repository.reactive.ReactiveCrudRepository<T, UUID> repository,
                                                       AccessLevel level) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (entityId == null || entityId.trim().isEmpty()) {
            return Mono.just(false);
        }
        try {
            UUID uuid = UUID.fromString(entityId);
            return repository.findById(uuid)
                    .flatMap(entity -> {
                        UUID teamId = extractTeamId(entity);
                        if (teamId == null) {
                            Long ownerId = extractOwnerId(entity);
                            if (ownerId == null) {
                                return Mono.just(false);
                            }
                            return evaluateOwner(authentication, ownerId);
                        }
                        return evaluate(authentication, teamId, level.name());
                    })
                    .defaultIfEmpty(false)
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    private Long extractOwnerId(Object entity) {
        if (entity instanceof OwnableEntity ownableEntity) {
            return ownableEntity.getOwnerId();
        }
        return null;
    }

    private UUID extractTeamId(Object entity) {
        try {
            if (entity instanceof com.bntech.lexai.gateway.data.entity.document.LegalCase) {
                return ((com.bntech.lexai.gateway.data.entity.document.LegalCase) entity).getTeamId();
            } else if (entity instanceof com.bntech.lexai.gateway.data.entity.document.Document) {
                return ((com.bntech.lexai.gateway.data.entity.document.Document) entity).getTeamId();
            } else if (entity instanceof com.bntech.lexai.gateway.data.entity.document.DocumentGroup) {
                return ((com.bntech.lexai.gateway.data.entity.document.DocumentGroup) entity).getTeamId();
            } else if (entity instanceof com.bntech.lexai.gateway.data.entity.document.Event) {
                return ((com.bntech.lexai.gateway.data.entity.document.Event) entity).getTeamId();
            } else if (entity instanceof com.bntech.lexai.gateway.data.entity.document.LegalEntity) {
                return ((com.bntech.lexai.gateway.data.entity.document.LegalEntity<?>) entity).getTeamId();
            } else if (entity instanceof com.bntech.lexai.gateway.data.entity.document.StorageFile) {
                return ((com.bntech.lexai.gateway.data.entity.document.StorageFile) entity).getTeamId();
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting teamId from entity", e);
            return null;
        }
    }

    private Mono<Boolean> evaluate(Authentication authentication, UUID teamId, String level) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }

        if (teamId == null) {
            return Mono.just(false);
        }

        if (isAdmin(authentication)) {
            return Mono.just(true);
        }

        AccessLevel required = parseLevel(level);

        return userRepository.findByUsername(authentication.getName())
                .doOnNext(user -> log.debug("hasTeamAccess: found user with id: {}", user.getId()))
                .flatMap(user -> teamMemberRepository.findByUserIdAndTeamId(user.getId(), teamId)
                        .map(tm -> {
                            boolean hasAccess = tm.getAccessLevel().ordinal() >= required.ordinal();
                            log.debug("hasTeamAccess: hasAccess result: {}", hasAccess);
                            return hasAccess;
                        })
                        .defaultIfEmpty(false)
                        .doOnNext(hasAccess -> {
                            if (!hasAccess) {
                                log.debug("hasTeamAccess: no team membership found for ownerId: {} and teamId: {}",
                                        user.getId(), teamId);
                            }
                        }))
                .defaultIfEmpty(false)
                .doOnError(e -> log.error("hasTeamAccess: exception occurred", e))
                .onErrorReturn(false);
    }

    private Mono<Boolean> evaluateOwner(Authentication authentication, Long ownerId) {
        if (isAdmin(authentication)) {
            return Mono.just(true);
        }
        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(ownerId))
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private AccessLevel parseLevel(String level) {
        try {
            return AccessLevel.valueOf(level);
        } catch (Exception e) {
            return AccessLevel.READ;
        }
    }

    private boolean isBlankTeamId(String teamId) {
        if (teamId == null)
            return true;
        String v = teamId.trim();
        return v.isEmpty() || v.equalsIgnoreCase("null") || v.equalsIgnoreCase("undefined");
    }
}
