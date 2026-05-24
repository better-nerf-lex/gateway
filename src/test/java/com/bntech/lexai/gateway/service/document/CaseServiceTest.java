package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.object.ActionType;
import com.bntech.lexai.gateway.data.repository.document.CaseDocumentRepository;
import com.bntech.lexai.gateway.data.repository.document.CasePartyRepository;
import com.bntech.lexai.gateway.data.repository.document.CaseRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.document.EventParticipantRepository;
import com.bntech.lexai.gateway.data.repository.document.EventRepository;
import com.bntech.lexai.gateway.data.repository.document.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.security.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.document.CaseMapper;
import com.bntech.lexai.gateway.mapper.document.CasePartyMapper;
import com.bntech.lexai.gateway.mapper.document.DocumentMapper;
import com.bntech.lexai.gateway.mapper.document.EventMapper;
import com.bntech.lexai.gateway.mapper.document.LegalEntityMapper;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import com.bntech.lexai.gateway.service.security.CognitoUserSyncService;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private CaseMapper caseMapper;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private CaseDocumentRepository caseDocumentRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentMapper documentMapper;
    @Mock private CasePartyRepository casePartyRepository;
    @Mock private CasePartyMapper casePartyMapper;
    @Mock private LegalEntityRepository legalEntityRepository;
    @Mock private LegalEntityMapper legalEntityMapper;
    @Mock private EventRepository eventRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EventParticipantRepository eventParticipantRepository;
    @Mock private DocumentGroupService documentGroupService;
    @Mock private DocumentGroupRepository documentGroupRepository;
    @Mock private ActivityService activityService;
    @Mock private CognitoUserSyncService cognitoUserSyncService;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(
                caseRepository, caseMapper, userRepository, teamMemberRepository,
                caseDocumentRepository, documentRepository, documentMapper,
                casePartyRepository, casePartyMapper, legalEntityRepository,
                legalEntityMapper, eventRepository, eventMapper,
                eventParticipantRepository, documentGroupService,
                documentGroupRepository, activityService, cognitoUserSyncService
        );
    }

    @Test
    void deleteCase_ShouldLogActivity() {
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        LegalCase legalCase = new LegalCase();
        legalCase.setId(id);
        legalCase.setTeamId(teamId);
        legalCase.setOwnerId(1L);
        legalCase.setTitle("Test Case");

        when(caseRepository.findById(id)).thenReturn(Mono.just(legalCase));
        when(caseRepository.softDelete(id)).thenReturn(Mono.empty());

        StepVerifier.create(caseService.deleteCase(id.toString()))
                .verifyComplete();

        verify(activityService).logActivity(any(), eq(teamId), eq(ActionType.CASE_DELETE), eq(id), eq("LegalCase"), anyString());
    }

    @Test
    void getCase_ShouldReturnForbidden_WhenCurrentUserIsNotTeamMember() {
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        LegalCase legalCase = new LegalCase();
        legalCase.setId(id);
        legalCase.setTeamId(teamId);
        legalCase.setOwnerId(1L);

        User user = User.builder()
                .id(2L)
                .username("case-fixture@example.invalid")
                .email("case-fixture@example.invalid")
                .enabled(true)
                .superadmin(false)
                .build();

        when(caseRepository.findById(id)).thenReturn(Mono.just(legalCase));
        when(cognitoUserSyncService.getCurrentUser()).thenReturn(Mono.just(user));
        when(teamMemberRepository.findByUserIdAndTeamId(user.getId(), teamId)).thenReturn(Mono.empty());

        StepVerifier.create(caseService.getCase(id.toString()))
                .expectError(AccessDeniedException.class)
                .verify();

        verify(caseMapper, never()).toDto(any());
    }

    @Test
    void getCase_ShouldReturnError_WhenMapperReturnsEmptyDto() {
        UUID id = UUID.randomUUID();
        LegalCase legalCase = new LegalCase();
        legalCase.setId(id);
        legalCase.setOwnerId(1L);

        User user = User.builder()
                .id(1L)
                .username("case-fixture@example.invalid")
                .email("case-fixture@example.invalid")
                .enabled(true)
                .superadmin(false)
                .build();

        when(caseRepository.findById(id)).thenReturn(Mono.just(legalCase));
        when(cognitoUserSyncService.getCurrentUser()).thenReturn(Mono.just(user));
        when(caseMapper.toDto(legalCase)).thenReturn(new CaseDto());

        StepVerifier.create(caseService.getCase(id.toString()))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(casePartyRepository, never()).findByCaseId(any());
        verify(documentGroupService, never()).getDocumentGroupsByCaseId(any());
    }
}
