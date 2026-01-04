package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.repository.*;
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
        if (authentication == null || !authentication.isAuthenticated())
            return Mono.just(false);
        if (isBlankTeamId(teamId))
            return Mono.just(true);
        try {
            return hasTeamAccess(authentication, UUID.fromString(teamId), AccessLevel.READ.name());
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> canListLegalEntities(Authentication authentication, String teamId) {
        log.debug("canListLegalEntities called - auth: {}, teamId: {}",
                authentication != null ? authentication.getName() : "null", teamId);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("canListLegalEntities: authentication is null or not authenticated");
            return Mono.just(false);
        }

        if (isBlankTeamId(teamId)) {
            log.debug("canListLegalEntities: teamId is blank, allowing access");
            return Mono.just(true);
        }

        try {
            UUID uuid = UUID.fromString(teamId);
            return hasTeamAccess(authentication, uuid, AccessLevel.READ.name())
                    .doOnNext(hasAccess -> log.debug("canListLegalEntities: hasTeamAccess returned {}", hasAccess));
        } catch (IllegalArgumentException e) {
            log.error("canListLegalEntities: invalid teamId format: {}", teamId);
            return Mono.just(false);
        }
    }

    public Mono<Boolean> canListCases(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated())
            return Mono.just(false);
        if (isBlankTeamId(teamId))
            return Mono.just(true);
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
        log.debug("hasTeamAccessByDocument called - id: {}, level: {}", id, level);
        if (id == null || id.isBlank() || isBlankId(id)) {
            log.debug("hasTeamAccessByDocument: invalid id: {}", id);
            return Mono.just(false);
        }
        try {
            UUID uuid = UUID.fromString(id.trim());
            return documentRepository.findById(uuid)
                    .flatMap(doc -> {
                        if (doc.getTeamId() == null) {
                            log.debug("hasTeamAccessByDocument: document found but has no teamId: {}", id);
                            return Mono.just(false);
                        }
                        log.debug("hasTeamAccessByDocument: document found, teamId: {}, checking team access", doc.getTeamId());
                        return hasTeamAccess(authentication, doc.getTeamId(), level);
                    })
                    .defaultIfEmpty(false)
                    .doOnNext(hasAccess -> log.debug("hasTeamAccessByDocument: result for id {}: {}", id, hasAccess))
                    .onErrorResume(e -> {
                        log.error("hasTeamAccessByDocument: error occurred for id {}", id, e);
                        return Mono.just(false);
                    });
        } catch (IllegalArgumentException e) {
            log.error("hasTeamAccessByDocument: invalid UUID format: {}", id);
            return Mono.just(false);
        }
    }

    public Mono<Boolean> hasTeamAccessByLegalEntity(Authentication authentication, String id, String level) {
        if (id == null || id.isBlank() || isBlankId(id))
            return Mono.just(false);
        try {
            UUID uuid = UUID.fromString(id.trim());
            return legalEntityRepository.findById(uuid)
                    .flatMap(le -> {
                        if (le.getTeamId() == null) return Mono.just(false);
                        return hasTeamAccess(authentication, le.getTeamId(), level);
                    })
                    .defaultIfEmpty(false)
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> isOwnerOrAdminOfDocument(Authentication authentication, String id) {
        log.debug("isOwnerOrAdminOfDocument called - user: {}, id: {}",
                authentication != null ? authentication.getName() : "null", id);
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("isOwnerOrAdminOfDocument: authentication is null or not authenticated");
            return Mono.just(false);
        }
        if (id == null || id.isBlank() || isBlankId(id)) {
            log.debug("isOwnerOrAdminOfDocument: invalid id: {}", id);
            return Mono.just(false);
        }
        if (isAdmin(authentication)) {
            log.debug("isOwnerOrAdminOfDocument: user is admin, granting access");
            return Mono.just(true);
        }

        try {
            UUID uuid = UUID.fromString(id.trim());
            return userRepository.findByUsername(authentication.getName())
                    .flatMap(user -> {
                        log.debug("isOwnerOrAdminOfDocument: checking document {} for user id {}", uuid, user.getId());
                        return documentRepository.findById(uuid)
                            .map(doc -> {
                                boolean isOwner = user.getId().equals(doc.getOwnerId());
                                log.debug("isOwnerOrAdminOfDocument: user id: {}, document owner id: {}, isOwner: {}",
                                        user.getId(), doc.getOwnerId(), isOwner);
                                return isOwner;
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.debug("isOwnerOrAdminOfDocument: document not found: {}", uuid);
                                return Mono.just(false);
                            }));
                    })
                    .defaultIfEmpty(false)
                    .onErrorResume(e -> {
                        log.error("isOwnerOrAdminOfDocument: error occurred for id {}", id, e);
                        return Mono.just(false);
                    });
        } catch (IllegalArgumentException e) {
            log.error("isOwnerOrAdminOfDocument: invalid UUID format: {}", id);
            return Mono.just(false);
        }
    }

    public Mono<Boolean> isOwnerOrAdminOfLegalEntity(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated())
            return Mono.just(false);
        if (id == null || id.isBlank() || isBlankId(id))
            return Mono.just(false);
        if (isAdmin(authentication))
            return Mono.just(true);

        try {
            return userRepository.findByUsername(authentication.getName())
                    .flatMap(user -> legalEntityRepository.findById(UUID.fromString(id.trim()))
                            .map(e -> user.getId().equals(e.getOwnerId()))
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
        log.debug("hasTeamAccessByCase check - id: {}, level: {}", id, level);
        if (id == null || id.isBlank() || isBlankId(id)) {
            log.debug("hasTeamAccessByCase: invalid id: {}", id);
            return Mono.just(false);
        }
        try {
            return caseRepository.findById(UUID.fromString(id.trim()))
                    .flatMap(c -> {
                        if (c.getTeamId() == null) return Mono.just(false);
                        log.debug("hasTeamAccessByCase: case found, teamId: {}, checking team access", c.getTeamId());
                        return hasTeamAccess(authentication, c.getTeamId(), level);
                    })
                    .defaultIfEmpty(false)
                    .doOnNext(hasAccess -> log.debug("hasTeamAccessByCase result: {}", hasAccess))
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            log.error("hasTeamAccessByCase: invalid UUID format: {}", id);
            return Mono.just(false);
        }
    }

    public Mono<Boolean> isOwnerOrAdminOfCase(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated())
            return Mono.just(false);
        if (id == null || id.isBlank() || isBlankId(id))
            return Mono.just(false);
        if (isAdmin(authentication))
            return Mono.just(true);

        try {
            return userRepository.findByUsername(authentication.getName())
                    .flatMap(user -> caseRepository.findById(UUID.fromString(id.trim()))
                            .map(c -> user.getId().equals(c.getOwnerId()))
                            .defaultIfEmpty(false))
                    .defaultIfEmpty(false)
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
    }

    public Mono<Boolean> isOwnerOrAdminOfEvent(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated())
            return Mono.just(false);
        if (id == null || id.isBlank() || isBlankId(id))
            return Mono.just(false);
        if (isAdmin(authentication))
            return Mono.just(true);

        try {
            return userRepository.findByUsername(authentication.getName())
                    .flatMap(user -> eventRepository.findById(UUID.fromString(id.trim()))
                            .map(e -> user.getId().equals(e.getOwnerId()))
                            .defaultIfEmpty(false))
                    .defaultIfEmpty(false)
                    .onErrorReturn(false);
        } catch (IllegalArgumentException e) {
            return Mono.just(false);
        }
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
                        return Mono.just(true);
                    }
                    return isOwnerOrAdminOfCase(authentication, id)
                            .doOnNext(isOwner -> log.debug("canViewCase: Access result via owner/admin for {}: {}", id,
                                    isOwner));
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
