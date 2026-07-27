package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.data.repository.document.CaseRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.document.StorageFileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentGenerationContextServiceTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private DocumentGroupRepository documentGroupRepository;
    @Mock
    private StorageFileRepository storageFileRepository;

    private DocumentGenerationContextService service;

    @BeforeEach
    void setUp() {
        service = new DocumentGenerationContextService(
                caseRepository,
                documentGroupRepository,
                storageFileRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                new ObjectMapper());
    }

    @Test
    void mergesExplicitGroupAndCaseFilesWithoutDuplicates() {
        UUID teamId = UUID.randomUUID();
        UUID selectedGroupId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID caseGroupId = UUID.randomUUID();
        UUID explicitFileId = UUID.randomUUID();
        UUID groupFileId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        DocumentGroup selectedGroup = DocumentGroup.builder()
                .id(selectedGroupId)
                .teamId(teamId)
                .build();
        LegalCase legalCase = LegalCase.builder()
                .id(caseId)
                .teamId(teamId)
                .build();
        DocumentGroup caseGroup = DocumentGroup.builder()
                .id(caseGroupId)
                .caseId(caseId)
                .teamId(teamId)
                .build();
        StorageFile groupFile = StorageFile.builder()
                .id(groupFileId)
                .documentGroupId(selectedGroupId)
                .teamId(teamId)
                .build();
        StorageFile duplicateGroupFile = StorageFile.builder()
                .id(groupFileId)
                .documentGroupId(caseGroupId)
                .teamId(teamId)
                .build();
        StorageFile caseFile = StorageFile.builder()
                .id(caseFileId)
                .documentGroupId(caseGroupId)
                .teamId(teamId)
                .build();

        when(documentGroupRepository.findActiveById(selectedGroupId)).thenReturn(Mono.just(selectedGroup));
        when(storageFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(selectedGroupId))
                .thenReturn(Flux.just(groupFile));
        when(caseRepository.findById(caseId)).thenReturn(Mono.just(legalCase));
        when(documentGroupRepository.findByCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Flux.just(caseGroup));
        when(storageFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(caseGroupId))
                .thenReturn(Flux.just(duplicateGroupFile, caseFile));

        StepVerifier.create(service.resolveContextFileIds(
                        List.of(explicitFileId.toString(), groupFileId.toString()),
                        selectedGroupId.toString(),
                        caseId.toString(),
                        teamId))
                .expectNext(List.of(
                        explicitFileId.toString(),
                        groupFileId.toString(),
                        caseFileId.toString()))
                .verifyComplete();
    }
}
