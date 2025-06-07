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
                .filter(User::isEnabled)
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")))
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
                .flatMap(username -> userRepository.findByUsername(username))
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
