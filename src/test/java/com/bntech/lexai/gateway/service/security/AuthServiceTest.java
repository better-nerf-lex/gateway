package com.bntech.lexai.gateway.service.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.exception.UnauthorizedException;
import com.bntech.lexai.gateway.mapper.security.UserMapper;
import com.bntech.lexai.gateway.rest.dto.security.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jwtService, userRepository, passwordEncoder, userMapper);
    }

    @Test
    void authenticatesEnabledSuperadminForDeploymentFallback() {
        User user = User.builder()
                .id(1L)
                .username("user")
                .email("user@example.invalid")
                .password("hash")
                .enabled(true)
                .superadmin(true)
                .build();
        user.setNotNew();

        when(userRepository.findByUsername("user@example.invalid")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("user@example.invalid")).thenReturn(Mono.just(user));
        when(userRepository.findById(1L)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("Admin123!", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(userMapper.toDto(user)).thenReturn(UserDto.builder().id(1L).email(user.getEmail()).enabled(true).build());

        StepVerifier.create(authService.authenticateSuperadmin(LoginRequest.builder()
                        .username("user@example.invalid")
                        .password("Admin123!")
                        .build()))
                .expectNextMatches(response -> response.getUser().getId().equals(1L))
                .verifyComplete();
    }

    @Test
    void rejectsLocalFallbackForNonSuperadminUsers() {
        User user = User.builder()
                .id(2L)
                .username("regular@example.com")
                .email("regular@example.com")
                .password("hash")
                .enabled(true)
                .superadmin(false)
                .build();
        user.setNotNew();

        when(userRepository.findByUsername("regular@example.com")).thenReturn(Mono.just(user));
        when(userRepository.findByEmail("regular@example.com")).thenReturn(Mono.empty());
        when(userRepository.findById(2L)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder().id(2L).enabled(true).build());

        StepVerifier.create(authService.authenticateSuperadmin(LoginRequest.builder()
                        .username("regular@example.com")
                        .password("password")
                        .build()))
                .expectError(UnauthorizedException.class)
                .verify();
    }
}
