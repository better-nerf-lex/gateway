package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.Team;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.TeamRepository;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoUserSyncServiceTest {

    @Test
    void syncUserFromIdTokenMarksExistingUserAsNotNewBeforeSave() {
        User existingUser = User.builder()
                .id(7L)
                .username("user@example.invalid")
                .email("user@example.invalid")
                .enabled(true)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isNew(true)
                .build();
        AtomicReference<User> savedUser = new AtomicReference<>();
        UserRepository userRepository = userRepositoryReturning(existingUser, savedUser);
        CognitoUserSyncService service = new CognitoUserSyncService(
                userRepository,
                emptyTeamRepository(),
                teamMemberRepositoryWithExistingMembership());

        StepVerifier.create(service.syncUserFromIdTokenString(idToken("cognito-sub", "user@example.invalid", true)))
                .assertNext(user -> {
                    assertThat(user.getCognitoSub()).isEqualTo("cognito-sub");
                    assertThat(user.getEmailVerified()).isTrue();
                })
                .verifyComplete();

        assertThat(savedUser.get()).isNotNull();
        assertThat(savedUser.get().isNew()).isFalse();
    }

    private TeamRepository emptyTeamRepository() {
        return (TeamRepository) Proxy.newProxyInstance(
                TeamRepository.class.getClassLoader(),
                new Class<?>[]{TeamRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return Mono.just((Team) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private TeamMemberRepository teamMemberRepositoryWithExistingMembership() {
        TeamMember member = new TeamMember();
        member.setId(UUID.randomUUID());
        member.setTeamId(UUID.randomUUID());
        member.setUserId(7L);
        return (TeamMemberRepository) Proxy.newProxyInstance(
                TeamMemberRepository.class.getClassLoader(),
                new Class<?>[]{TeamMemberRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByUserId" -> Flux.just(member);
                    case "save" -> Mono.just((TeamMember) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private UserRepository userRepositoryReturning(User existingUser, AtomicReference<User> savedUser) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByCognitoSub" -> Mono.empty();
                    case "findByEmail" -> Mono.just(existingUser);
                    case "save" -> {
                        User user = (User) args[0];
                        savedUser.set(user);
                        yield Mono.just(user);
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private String idToken(String sub, String email, boolean emailVerified) {
        String header = encode("{\"alg\":\"none\"}");
        String payload = encode(String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"email_verified\":%s}",
                sub,
                email,
                emailVerified));
        return header + "." + payload + ".signature";
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
