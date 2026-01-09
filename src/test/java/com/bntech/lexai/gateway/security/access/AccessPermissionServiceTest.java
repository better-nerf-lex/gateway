package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessPermissionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private Authentication authentication;

    private AccessPermissionService accessPermissionService;

    @BeforeEach
    void setUp() {
        accessPermissionService = new AccessPermissionService(
                userRepository,
                teamMemberRepository
        );
    }

    @Test
    void teamRead_ShouldReturnTrue_WhenUserIsAdmin() {
        UUID teamId = UUID.randomUUID();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        StepVerifier.create(accessPermissionService.teamRead(authentication, teamId.toString()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void teamRead_ShouldReturnFalse_WhenUserIsNotMember() {
        String username = "testuser";
        Long userId = 1L;
        UUID teamId = UUID.randomUUID();

        User user = User.builder().id(userId).username(username).build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(teamMemberRepository.findByUserIdAndTeamId(userId, teamId)).thenReturn(Mono.empty());

        StepVerifier.create(accessPermissionService.teamRead(authentication, teamId.toString()))
                .expectNext(false)
                .verifyComplete();
    }
}
