package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
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

    public Mono<Boolean> canListDocuments(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) return Mono.just(false);
        if (isBlankTeamId(teamId)) return Mono.just(true);
        return hasTeamAccess(authentication, UUID.fromString(teamId), AccessLevel.READ.name());
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

        return hasTeamAccess(authentication, UUID.fromString(teamId), AccessLevel.READ.name())
                .doOnNext(hasAccess -> log.debug("canListLegalEntities: hasTeamAccess returned {}", hasAccess));
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

        AccessLevel required = parseLevel(level);
        log.debug("hasTeamAccess: required access level: {}", required);

        if (isAdmin(authentication) && required == AccessLevel.ADMIN) {
            log.debug("hasTeamAccess: user is admin, granting access");
            return Mono.just(true);
        }

        return userRepository.findByUsername(authentication.getName())
                .doOnNext(user -> log.debug("hasTeamAccess: found user with id: {}", user.getId()))
                .flatMap(user -> teamMemberRepository.findByUserIdAndTeamId(user.getId(), teamId)
                        .doOnNext(tm -> log.debug("hasTeamAccess: team member found with access level: {} (ordinal: {}), required: {} (ordinal: {})",
                                tm.getAccessLevel(), tm.getAccessLevel().ordinal(), required, required.ordinal()))
                        .map(tm -> {
                            boolean hasAccess = tm.getAccessLevel().ordinal() >= required.ordinal();
                            log.debug("hasTeamAccess: hasAccess result: {}", hasAccess);
                            return hasAccess;
                        })
                        .defaultIfEmpty(false)
                        .doOnNext(hasAccess -> {
                            if (!hasAccess) {
                                log.debug("hasTeamAccess: no team membership found for userId: {} and teamId: {}", user.getId(), teamId);
                            }
                        })
                )
                .defaultIfEmpty(false)
                .doOnError(e -> log.error("hasTeamAccess: exception occurred", e))
                .onErrorReturn(false);
    }

    public Mono<Boolean> hasTeamAccessByDocument(Authentication authentication, String id, String level) {
        if (id == null || id.isBlank() || isBlankId(id)) return Mono.just(false);
        return documentRepository.findById(UUID.fromString(id))
                .map(Document::getTeamId)
                .filter(teamId -> teamId != null)
                .flatMap(teamId -> hasTeamAccess(authentication, teamId, level))
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    public Mono<Boolean> hasTeamAccessByLegalEntity(Authentication authentication, String id, String level) {
        if (id == null || id.isBlank() || isBlankId(id)) return Mono.just(false);
        return legalEntityRepository.findById(UUID.fromString(id))
                .map(LegalEntity::getTeamId)
                .filter(teamId -> teamId != null)
                .flatMap(teamId -> hasTeamAccess(authentication, teamId, level))
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    public Mono<Boolean> isOwnerOrAdminOfDocument(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated()) return Mono.just(false);
        if (id == null || id.isBlank() || isBlankId(id)) return Mono.just(false);
        if (isAdmin(authentication)) return Mono.just(true);

        return userRepository.findByUsername(authentication.getName())
                .flatMap(user -> documentRepository.findById(UUID.fromString(id))
                        .map(doc -> user.getId().equals(doc.getUserId()))
                        .defaultIfEmpty(false)
                )
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    public Mono<Boolean> isOwnerOrAdminOfLegalEntity(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated()) return Mono.just(false);
        if (id == null || id.isBlank() || isBlankId(id)) return Mono.just(false);
        if (isAdmin(authentication)) return Mono.just(true);

        return userRepository.findByUsername(authentication.getName())
                .flatMap(user -> legalEntityRepository.findById(UUID.fromString(id))
                        .map(e -> user.getId().equals(e.getUserId()))
                        .defaultIfEmpty(false)
                )
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    public Mono<Boolean> canUpdateLegalEntity(Authentication authentication, String id) {
        return hasTeamAccessByLegalEntity(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasTeamAccess -> {
                    if (hasTeamAccess) return Mono.just(true);
                    return isOwnerOrAdminOfLegalEntity(authentication, id);
                });
    }

    public Mono<Boolean> canViewLegalEntity(Authentication authentication, String id) {
        return hasTeamAccessByLegalEntity(authentication, id, AccessLevel.READ.name())
                .flatMap(hasAccess -> {
                    if (hasAccess) return Mono.just(true);
                    return isOwnerOrAdminOfLegalEntity(authentication, id);
                });
    }

    public Mono<Boolean> canDeleteLegalEntity(Authentication authentication, String id) {
        return hasTeamAccessByLegalEntity(authentication, id, AccessLevel.WRITE.name())
                .flatMap(hasTeamAccess -> {
                    if (hasTeamAccess) return Mono.just(true);
                    return isOwnerOrAdminOfLegalEntity(authentication, id);
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
        if (id == null) return true;
        String v = id.trim();
        return v.isEmpty() || v.equalsIgnoreCase("null") || v.equalsIgnoreCase("undefined");
    }

    private boolean isBlankTeamId(String teamId) {
        if (teamId == null) return true;
        String v = teamId.trim();
        return v.isEmpty() || v.equalsIgnoreCase("null") || v.equalsIgnoreCase("undefined");
    }
}
