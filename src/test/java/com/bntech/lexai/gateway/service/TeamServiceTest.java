package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.TeamRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.security.access.AccessLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    void updateMemberRole_ShouldSetNotNewBeforeSaving() {
        UUID teamId = UUID.randomUUID();
        Long userId = 1L;
        AccessLevel newLevel = AccessLevel.WRITE;

        TeamMember member = TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .accessLevel(AccessLevel.READ)
                .isNew(true) // Default
                .build();

        when(teamMemberRepository.findByUserIdAndTeamId(userId, teamId)).thenReturn(Mono.just(member));
        
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
            TeamMember savedMember = invocation.getArgument(0);
            if (savedMember.isNew()) {
                return Mono.error(new RuntimeException("DuplicateKeyException simulation"));
            }
            return Mono.just(savedMember);
        });

        StepVerifier.create(teamService.updateMemberRole(teamId, userId, newLevel))
                .verifyComplete();

        verify(teamMemberRepository).save(argThat(m -> !m.isNew() && m.getAccessLevel() == newLevel));
    }
}
