package com.bntech.lexai.gateway.security.access;

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
