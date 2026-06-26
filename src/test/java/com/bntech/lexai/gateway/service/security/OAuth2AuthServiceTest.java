package com.bntech.lexai.gateway.service.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.security.AuthorizationCodeRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.rest.dto.team.TeamDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationCodeRepository authorizationCodeRepository;

    @Mock
    private TeamService teamService;

    private OAuth2AuthService service;

    @BeforeEach
    void setUp() {
        service = new OAuth2AuthService(jwtService, userRepository, authorizationCodeRepository, teamService);
    }

    @Test
    void createsAccessAndRefreshTokensForOAuthLogin() {
        OAuth2User oauthUser = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "email", "User@Example.com",
                        "name", "User Example",
                        "email_verified", true),
                "email");

        User user = User.builder()
                .id(7L)
                .username("user@example.com")
                .email("user@example.com")
                .enabled(true)
                .build();
        user.setNotNew();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Mono.just(user));
        when(teamService.ensureDefaultTeam(user)).thenReturn(Mono.just(TeamDto.builder()
                .id(UUID.randomUUID())
                .name("Default team")
                .build()));
        when(jwtService.generateTokenFromOAuth2User(oauthUser, "user@example.com")).thenReturn("access-token");
        when(jwtService.generateRefreshTokenFromOAuth2User(oauthUser, "user@example.com")).thenReturn("refresh-token");

        StepVerifier.create(service.handleOAuth2Success(oauthUser))
                .expectNextMatches(response -> "access-token".equals(response.getToken())
                        && "refresh-token".equals(response.getRefreshToken())
                        && response.getUser().getId().equals(7L))
                .verifyComplete();
    }
}
