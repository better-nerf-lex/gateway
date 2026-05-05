package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.repository.CaseRepository;
import com.bntech.lexai.gateway.data.repository.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.DocumentGroupMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentGroupServiceTest {

    @Mock
    private DocumentGroupRepository documentGroupRepository;

    @Mock
    private StorageFileService storageFileService;

    @Mock
    private DocumentGroupMapper documentGroupMapper;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CognitoUserSyncService cognitoUserSyncService;

    @InjectMocks
    private DocumentGroupService documentGroupService;

    private UUID groupId;
    private UUID caseId;
    private UUID teamId;
    private DocumentGroup documentGroup;
    private LegalCase legalCase;
    private DocumentGroupDto documentGroupDto;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        documentGroup = DocumentGroup.builder()
                .id(groupId)
                .name("Test Group")
                .teamId(teamId)
                .build();
        legalCase = LegalCase.builder()
                .id(caseId)
                .teamId(teamId)
                .build();
        documentGroupDto = DocumentGroupDto.builder()
                .id(groupId.toString())
                .name("Test Group")
                .build();
    }

    @Test
    void assignToCase_ShouldUpdateCaseIdAndMoveFiles() {
        when(documentGroupRepository.findById(groupId)).thenReturn(Mono.just(documentGroup));
        when(caseRepository.findById(caseId)).thenReturn(Mono.just(legalCase));
        when(storageFileService.moveGroupFiles(groupId, null, caseId)).thenReturn(Mono.empty());
        when(documentGroupRepository.save(any(DocumentGroup.class))).thenReturn(Mono.just(documentGroup));
        when(documentGroupMapper.toDto(any(DocumentGroup.class))).thenReturn(documentGroupDto);
        when(storageFileService.getDocumentFilesByGroupId(groupId.toString())).thenReturn(Flux.empty());

        StepVerifier.create(documentGroupService.assignToCase(groupId.toString(), caseId.toString()))
                .expectNextMatches(dto -> dto.getId().equals(groupId.toString()))
                .verifyComplete();

        verify(storageFileService).moveGroupFiles(groupId, null, caseId);
        verify(documentGroupRepository).save(argThat(group -> group.getCaseId().equals(caseId)));
    }

    @Test
    void removeFromCase_ShouldClearCaseIdAndMoveFiles() {
        documentGroup.setCaseId(caseId);
        when(documentGroupRepository.findById(groupId)).thenReturn(Mono.just(documentGroup));
        when(storageFileService.moveGroupFiles(groupId, caseId, null)).thenReturn(Mono.empty());
        when(documentGroupRepository.save(any(DocumentGroup.class))).thenReturn(Mono.just(documentGroup));
        when(documentGroupMapper.toDto(any(DocumentGroup.class))).thenReturn(documentGroupDto);
        when(storageFileService.getDocumentFilesByGroupId(groupId.toString())).thenReturn(Flux.empty());

        StepVerifier.create(documentGroupService.removeFromCase(groupId.toString()))
                .expectNextMatches(dto -> dto.getId().equals(groupId.toString()))
                .verifyComplete();

        verify(storageFileService).moveGroupFiles(groupId, caseId, null);
        verify(documentGroupRepository).save(argThat(group -> group.getCaseId() == null));
    }

    @Test
    void assignToCase_ShouldThrowExceptionWhenNotFound() {
        when(documentGroupRepository.findById(groupId)).thenReturn(Mono.empty());
        when(caseRepository.findById(caseId)).thenReturn(Mono.just(legalCase));

        StepVerifier.create(documentGroupService.assignToCase(groupId.toString(), caseId.toString()))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void assignToCase_ShouldRejectDifferentTeam() {
        legalCase.setTeamId(UUID.randomUUID());
        when(documentGroupRepository.findById(groupId)).thenReturn(Mono.just(documentGroup));
        when(caseRepository.findById(caseId)).thenReturn(Mono.just(legalCase));

        StepVerifier.create(documentGroupService.assignToCase(groupId.toString(), caseId.toString()))
                .expectError(AccessDeniedException.class)
                .verify();

        verify(storageFileService, never()).moveGroupFiles(any(), any(), any());
        verify(documentGroupRepository, never()).save(any());
    }
}
