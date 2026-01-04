package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.exception.UnauthorizedException;
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

    private final JwtService jwts;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Mono<LoginResponse> authenticate(LoginRequest loginRequest) {
        return findUserByUsernameOrEmail(loginRequest.getUsername())
                .doOnNext(user -> log.info("Found user: {} with enabled: {}", user.getUsername(), user.isEnabled()))
                .doOnError(error -> log.error("Error finding user: ", error))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No user found for username/email: {}", loginRequest.getUsername());
                    return Mono.error(new ResourceNotFoundException("User", loginRequest.getUsername()));
                }))
                .filter(User::isEnabled)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User is disabled: {}", loginRequest.getUsername());
                    return Mono.error(new UnauthorizedException("User account is disabled"));
                }))
                .filter(user -> {
                    String providedPassword = loginRequest.getPassword();
                    String storedHash = user.getPassword();

                    return passwordEncoder.matches(providedPassword, storedHash);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Password mismatch for user: {}", loginRequest.getUsername());
                    return Mono.error(new UnauthorizedException("Invalid credentials"));
                }))
                .map(user -> {
                    String token = jwts.generateToken(user);
                    String refreshToken = jwts.generateRefreshToken(user);
                    return LoginResponse.builder()
                            .token(token)
                            .refreshToken(refreshToken)
                            .user(mapToUserDto(user))
                            .build();
                });
    }

    private Mono<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        return users.findByUsername(usernameOrEmail)
                .switchIfEmpty(
                        users.findByEmail(usernameOrEmail));
    }

    public Mono<Void> logout(Authentication authentication) {
        return jwts.invalidateToken(authentication.getCredentials().toString());
    }

    public Mono<UserDto> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return users.findByUsername(username)
                .map(this::mapToUserDto);
    }

    public Mono<RefreshTokenResponse> refreshToken(String token) {
        return jwts.validateRefreshToken(token)
                .flatMap(users::findByUsername)
                .filter(User::isEnabled)
                .map(user -> {
                    String newToken = jwts.generateToken(user);
                    return RefreshTokenResponse.builder()
                            .token(newToken)
                            .build();
                });
    }

    private UserDto mapToUserDto(User user) {
        return userMapper.toDto(user);
    }
}
