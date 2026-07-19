package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.document.CaseRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.document.EventRepository;
import com.bntech.lexai.gateway.data.repository.document.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.document.StorageFileRepository;
import com.bntech.lexai.gateway.data.repository.security.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessPermissionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentGroupRepository documentGroupRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private StorageFileRepository storageFileRepository;
    @Mock
    private Authentication authentication;

    private AccessPermissionService accessPermissionService;

    @BeforeEach
    void setUp() {
        accessPermissionService = new AccessPermissionService(
                userRepository,
                teamMemberRepository,
                caseRepository,
                documentRepository,
                documentGroupRepository,
                eventRepository,
                legalEntityRepository,
                storageFileRepository
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

    @Test
    void caseRead_ShouldReturnTrue_WhenUserHasTeamAccess() {
        String username = "testuser";
        Long userId = 1L;
        UUID caseId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        LegalCase legalCase = LegalCase.builder().id(caseId).teamId(teamId).build();
        User user = User.builder().id(userId).username(username).build();
        TeamMember teamMember = TeamMember.builder()
                .userId(userId)
                .teamId(teamId)
                .accessLevel(AccessLevel.READ)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(caseRepository.findById(caseId)).thenReturn(Mono.just(legalCase));
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(teamMemberRepository.findByUserIdAndTeamId(userId, teamId)).thenReturn(Mono.just(teamMember));

        StepVerifier.create(accessPermissionService.caseRead(authentication, caseId.toString()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void caseRead_ShouldReturnFalse_WhenCaseNotFound() {
        UUID caseId = UUID.randomUUID();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(caseRepository.findById(caseId)).thenReturn(Mono.empty());

        StepVerifier.create(accessPermissionService.caseRead(authentication, caseId.toString()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void caseWrite_ShouldReturnFalse_WhenUserHasOnlyReadAccess() {
        String username = "testuser";
        Long userId = 1L;
        UUID caseId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        LegalCase legalCase = LegalCase.builder().id(caseId).teamId(teamId).build();
        User user = User.builder().id(userId).username(username).build();
        TeamMember teamMember = TeamMember.builder()
                .userId(userId)
                .teamId(teamId)
                .accessLevel(AccessLevel.READ)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(caseRepository.findById(caseId)).thenReturn(Mono.just(legalCase));
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(teamMemberRepository.findByUserIdAndTeamId(userId, teamId)).thenReturn(Mono.just(teamMember));

        StepVerifier.create(accessPermissionService.caseWrite(authentication, caseId.toString()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void storageFilesRead_ShouldReturnTrue_WhenNoFilesWereRequested() {
        when(authentication.isAuthenticated()).thenReturn(true);

        StepVerifier.create(accessPermissionService.storageFilesRead(authentication, List.of()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void storageFilesRead_ShouldRequireAccessToEveryFile() {
        UUID accessibleFileId = UUID.randomUUID();
        UUID inaccessibleFileId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        StorageFile accessibleFile = StorageFile.builder()
                .id(accessibleFileId)
                .teamId(teamId)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(storageFileRepository.findById(accessibleFileId)).thenReturn(Mono.just(accessibleFile));
        when(storageFileRepository.findById(inaccessibleFileId)).thenReturn(Mono.empty());

        StepVerifier.create(accessPermissionService.storageFilesRead(
                        authentication,
                        List.of(accessibleFileId.toString(), inaccessibleFileId.toString())))
                .expectNext(false)
                .verifyComplete();
    }
}
