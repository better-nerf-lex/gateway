package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
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
        CognitoUserSyncService service = new CognitoUserSyncService(userRepository);

        StepVerifier.create(service.syncUserFromIdTokenString(idToken("cognito-sub", "user@example.invalid", true)))
                .assertNext(user -> {
                    assertThat(user.getCognitoSub()).isEqualTo("cognito-sub");
                    assertThat(user.getEmailVerified()).isTrue();
                })
                .verifyComplete();

        assertThat(savedUser.get()).isNotNull();
        assertThat(savedUser.get().isNew()).isFalse();
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
