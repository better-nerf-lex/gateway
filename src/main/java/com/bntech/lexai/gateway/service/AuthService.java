package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.mapper.UserMapper;
import com.bntech.lexai.gateway.rest.dto.security.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.security.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenResponse;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;


    public Mono<LoginResponse> authenticate(LoginRequest loginRequest) {
        return findUserByUsernameOrEmail(loginRequest.getUsername())
                .doOnNext(user -> log.info("Found user: {} with enabled: {}", user.getUsername(), user.isEnabled()))
                .doOnError(error -> log.error("Error finding user: ", error))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No user found for username/email: {}", loginRequest.getUsername());
                    return Mono.error(new RuntimeException("User not found"));
                }))
                .filter(User::isEnabled)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User is disabled: {}", loginRequest.getUsername());
                    return Mono.error(new RuntimeException("User is disabled"));
                }))
                .filter(user -> {
                    String providedPassword = loginRequest.getPassword();
                    String storedHash = user.getPassword();

                    log.info("=== PASSWORD DEBUGGING ===");
                    log.info("Username: {}", user.getUsername());
                    log.info("Provided password: '{}'", providedPassword);
                    log.info("Provided password length: {}", providedPassword.length());
                    log.info("Stored hash: '{}'", storedHash);
                    log.info("Stored hash length: {}", storedHash.length());
                    log.info("Password encoder class: {}", passwordEncoder.getClass().getName());

                    boolean testPassword = passwordEncoder.matches("password", storedHash);
                    boolean testPassword123 = passwordEncoder.matches("password123", storedHash);
                    boolean testEmpty = passwordEncoder.matches("", storedHash);

                    log.info("Test 'password' against stored hash: {}", testPassword);
                    log.info("Test 'password123' against stored hash: {}", testPassword123);
                    log.info("Test empty string against stored hash: {}", testEmpty);

                    boolean matches = passwordEncoder.matches(providedPassword, storedHash);
                    log.info("Actual password matches: {}", matches);

                    String newHash = passwordEncoder.encode(providedPassword);
                    log.info("New hash for provided password: {}", newHash);
                    boolean newHashMatches = passwordEncoder.matches(providedPassword, newHash);
                    log.info("New hash matches provided password: {}", newHashMatches);

                    log.info("=== END PASSWORD DEBUGGING ===");

                    return matches;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Password mismatch for user: {}", loginRequest.getUsername());
                    return Mono.error(new RuntimeException("Invalid password"));
                }))
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return LoginResponse.builder()
                            .token(token)
                            .user(mapToUserDto(user))
                            .build();
                });
    }

    private Mono<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .switchIfEmpty(
                        userRepository.findByEmail(usernameOrEmail)
                );
    }

    public Mono<Void> logout(Authentication authentication) {
        return jwtService.invalidateToken(authentication.getCredentials().toString());
    }

    public Mono<UserDto> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(this::mapToUserDto);
    }

    public Mono<RefreshTokenResponse> refreshToken(String token) {
        return jwtService.validateToken(token)
                .flatMap(userRepository::findByUsername)
                .filter(User::isEnabled)
                .map(user -> {
                    String newToken = jwtService.generateToken(user);
                    return RefreshTokenResponse.builder()
                            .token(newToken)
                            .build();
                });
    }

    private UserDto mapToUserDto(User user) {
        return mapper.toDto(user);
    }
}
