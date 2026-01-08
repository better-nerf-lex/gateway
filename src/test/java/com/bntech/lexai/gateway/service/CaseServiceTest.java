package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.object.ActionType;
import com.bntech.lexai.gateway.data.repository.*;
import com.bntech.lexai.gateway.mapper.*;
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

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(
                caseRepository, caseMapper, userRepository, teamMemberRepository,
                caseDocumentRepository, documentRepository, documentMapper,
                casePartyRepository, casePartyMapper, legalEntityRepository,
                legalEntityMapper, eventRepository, eventMapper,
                eventParticipantRepository, documentGroupService,
                documentGroupRepository, activityService
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
}
