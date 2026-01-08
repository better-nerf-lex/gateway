package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.repository.CaseRepository;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.EventRepository;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component("access")
@RequiredArgsConstructor
public class AccessPermissionService {

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final DocumentRepository documentRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CaseRepository caseRepository;
    private final EventRepository eventRepository;

    public Mono<Boolean> canListDocuments(Authentication authentication, String teamId) {
        return canListByTeam(authentication, teamId);
    }

    public Mono<Boolean> canListLegalEntities(Authentication authentication, String teamId) {
        return canListByTeam(authentication, teamId);
    }

    public Mono<Boolean> canListCases(Authentication authentication, String teamId) {
        return canListByTeam(authentication, teamId);
    }

    private Mono<Boolean> canListByTeam(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (isBlankTeamId(teamId)) {
            return Mono.just(true);
        }
        try {
            return hasTeamAccess(authentication, UUID.fromString(teamId), AccessLevel.READ.name());
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> hasTeamAccess(Authentication authentication, UUID teamId, String level) {
        log.debug("hasTeamAccess called - user: {}, teamId: {}, level: {}",
                authentication != null ? authentication.getName() : "null", teamId, level);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("hasTeamAccess: authentication is null or not authenticated");
            return Mono.just(false);
        }

        if (teamId == null) {
            log.debug("hasTeamAccess: teamId is null");
            return Mono.just(false);
        }

        if (isAdmin(authentication)) {
            log.debug("hasTeamAccess: user is admin, granting access");
            return Mono.just(true);
        }

        AccessLevel required = parseLevel(level);
        log.debug("hasTeamAccess: required access level: {}", required);

        return userRepository.findByUsername(authentication.getName())
                .doOnNext(user -> log.debug("hasTeamAccess: found user with id: {}", user.getId()))
                .flatMap(user -> teamMemberRepository.findByUserIdAndTeamId(user.getId(), teamId)
                        .doOnNext(tm -> log.debug(
                                "hasTeamAccess: team member found with access level: {} (ordinal: {}), required: {} (ordinal: {})",
                                tm.getAccessLevel(), tm.getAccessLevel().ordinal(), required, required.ordinal()))
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

    public Mono<Boolean> hasTeamAccessByDocument(Authentication authentication, String id, String level) {
        return hasTeamAccessByEntity(authentication, id, level,
                uuid -> documentRepository.findById(uuid).map(doc -> doc.getTeamId()));
    }

    public Mono<Boolean> hasTeamAccessByLegalEntity(Authentication authentication, String id, String level) {
        return hasTeamAccessByEntity(authentication, id, level,
                uuid -> legalEntityRepository.findById(uuid).map(le -> le.getTeamId()));
    }

    private Mono<Boolean> hasTeamAccessByEntity(Authentication authentication, String id, String level,
            java.util.function.Function<UUID, Mono<UUID>> teamIdFinder) {
        if (id == null || id.isBlank() || isBlankId(id)) {
            return Mono.just(false);
        }
        try {
            UUID uuid = UUID.fromString(id.trim());
            return teamIdFinder.apply(uuid)
                    .flatMap(teamId -> {
                        if (teamId == null) {
                            return Mono.just(false);
                        }
                        return hasTeamAccess(authentication, teamId, level);
                    })
                    .defaultIfEmpty(false)
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> isOwnerOrAdminOfDocument(Authentication authentication, String id) {
        return isOwnerOrAdminOfEntity(authentication, id,
                uuid -> documentRepository.findById(uuid).map(doc -> doc.getOwnerId()));
    }

    public Mono<Boolean> isOwnerOrAdminOfLegalEntity(Authentication authentication, String id) {
        return isOwnerOrAdminOfEntity(authentication, id,
                uuid -> legalEntityRepository.findById(uuid).map(e -> e.getOwnerId()));
    }

    private Mono<Boolean> isOwnerOrAdminOfEntity(Authentication authentication, String id,
            java.util.function.Function<UUID, Mono<Long>> ownerIdFinder) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(false);
        }
        if (id == null || id.isBlank() || isBlankId(id)) {
            return Mono.just(false);
        }
        if (isAdmin(authentication)) {
            return Mono.just(true);
        }

        try {
            UUID uuid = UUID.fromString(id.trim());
            return userRepository.findByUsername(authentication.getName())
                    .flatMap(user -> ownerIdFinder.apply(uuid)
                            .map(ownerId -> user.getId().equals(ownerId))
                            .defaultIfEmpty(false))
                    .defaultIfEmpty(false)
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> canUpdateLegalEntity(Authentication authentication, String id) {
        return hasTeamAccessByLegalEntity(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasTeamAccess -> {
                    if (hasTeamAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfLegalEntity(authentication, id);
                });
    }

    public Mono<Boolean> canViewDocument(Authentication authentication, String id) {
        log.debug("canViewDocument called - user: {}, id: {}",
                authentication != null ? authentication.getName() : "null", id);
        return hasTeamAccessByDocument(authentication, id, AccessLevel.READ.name())
                .flatMap(hasAccess -> {
                    if (hasAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfDocument(authentication, id);
                });
    }

    public Mono<Boolean> canUpdateDocument(Authentication authentication, String id) {
        log.debug("canUpdateDocument called - user: {}, id: {}",
                authentication != null ? authentication.getName() : "null", id);
        return hasTeamAccessByDocument(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasAccess -> {
                    if (hasAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfDocument(authentication, id);
                });
    }

    public Mono<Boolean> canDeleteDocument(Authentication authentication, String id) {
        log.debug("canDeleteDocument called - user: {}, id: {}",
                authentication != null ? authentication.getName() : "null", id);
        return hasTeamAccessByDocument(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasAccess -> {
                    if (hasAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfDocument(authentication, id);
                });
    }

    public Mono<Boolean> canViewLegalEntity(Authentication authentication, String id) {
        return hasTeamAccessByLegalEntity(authentication, id, AccessLevel.READ.name())
                .flatMap(hasAccess -> {
                    if (hasAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfLegalEntity(authentication, id);
                });
    }

    public Mono<Boolean> canDeleteLegalEntity(Authentication authentication, String id) {
        return hasTeamAccessByLegalEntity(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasTeamAccess -> {
                    if (hasTeamAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfLegalEntity(authentication, id);
                });
    }

    public Mono<Boolean> hasTeamAccessByCase(Authentication authentication, String id, String level) {
        return hasTeamAccessByEntity(authentication, id, level,
                uuid -> caseRepository.findById(uuid).map(c -> c.getTeamId()));
    }

    public Mono<Boolean> isOwnerOrAdminOfCase(Authentication authentication, String id) {
        return isOwnerOrAdminOfEntity(authentication, id,
                uuid -> caseRepository.findById(uuid).map(c -> c.getOwnerId()));
    }

    public Mono<Boolean> isOwnerOrAdminOfEvent(Authentication authentication, String id) {
        return isOwnerOrAdminOfEntity(authentication, id,
                uuid -> eventRepository.findById(uuid).map(e -> e.getOwnerId()));
    }

    public Mono<Boolean> canUpdateCase(Authentication authentication, String id) {
        log.debug("canUpdateCase check - user: {}, caseId: {}",
                authentication != null ? authentication.getName() : "null", id);
        return hasTeamAccessByCase(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasTeamAccess -> {
                    if (hasTeamAccess) {
                        log.debug("canUpdateCase: granted via team for {}", id);
                        return Mono.just(true);
                    }
                    return isOwnerOrAdminOfCase(authentication, id)
                            .doOnNext(isOwner -> log.debug("canUpdateCase result via owner/admin for {}: {}", id,
                                    isOwner));
                });
    }

    public Mono<Boolean> canViewCase(Authentication authentication, String id) {
        log.debug("canViewCase check - user: {}, caseId: {}",
                authentication != null ? authentication.getName() : "null", id);
        return hasTeamAccessByCase(authentication, id, AccessLevel.READ.name())
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        log.debug("canViewCase: Access granted via team for {}", id);
                    }
                    return isOwnerOrAdminOfCase(authentication, id)
                            .doOnNext(isOwner -> log.debug("canViewCase: Access result via owner/admin for {}: {}", id,
                                    isOwner));
                })
                .doOnNext(allowed -> {
                    if (!allowed) {
                        log.warn("canViewCase: ACCESS DENIED for user: {}, caseId: {}",
                                authentication != null ? authentication.getName() : "null", id);
                    }
                });
    }

    public Mono<Boolean> canDeleteCase(Authentication authentication, String id) {
        return hasTeamAccessByCase(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasTeamAccess -> {
                    if (hasTeamAccess)
                        return Mono.just(true);
                    return isOwnerOrAdminOfCase(authentication, id);
                });
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

    private boolean isBlankId(String id) {
        if (id == null)
            return true;
        String v = id.trim();
        return v.isEmpty() || v.equalsIgnoreCase("null") || v.equalsIgnoreCase("undefined");
    }

    private boolean isBlankTeamId(String teamId) {
        if (teamId == null)
            return true;
        String v = teamId.trim();
        return v.isEmpty() || v.equalsIgnoreCase("null") || v.equalsIgnoreCase("undefined");
    }
}
