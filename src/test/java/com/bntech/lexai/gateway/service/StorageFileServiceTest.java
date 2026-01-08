package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.data.repository.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.StorageFileRepository;
import com.bntech.lexai.gateway.mapper.StorageFileMapper;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageFileServiceTest {

    @Mock
    private StorageFileRepository storageFileRepository;
    @Mock
    private DocumentGroupRepository documentGroupRepository;
    @Mock
    private StorageFileMapper documentFileMapper;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private StorageFileService storageFileService;

    @BeforeEach
    void setUp() {
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void moveFile_ShouldSetNotNewBeforeSaving() {
        UUID fileId = UUID.randomUUID();
        UUID targetGroupId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        StorageFile file = StorageFile.builder()
                .id(fileId)
                .name("test.pdf")
                .s3Url("s3://bucket/old/path/test.pdf")
                .isNew(true) // Default is true
                .build();

        DocumentGroup targetGroup = DocumentGroup.builder()
                .id(targetGroupId)
                .teamId(teamId)
                .caseId(caseId)
                .build();

        when(storageFileRepository.findById(fileId)).thenReturn(Mono.just(file));
        when(documentGroupRepository.findById(targetGroupId)).thenReturn(Mono.just(targetGroup));

        // Mock WebClient
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        // When save is called, it should be an update (isNew should be false)
        when(storageFileRepository.save(any(StorageFile.class))).thenAnswer(invocation -> {
            StorageFile savedFile = invocation.getArgument(0);
            if (savedFile.isNew()) {
                return Mono.error(new RuntimeException("DuplicateKeyException simulation: Tried to INSERT instead of UPDATE"));
            }
            return Mono.just(savedFile);
        });

        when(documentFileMapper.toDto(any(StorageFile.class))).thenReturn(new StorageFileDto());

        StepVerifier.create(storageFileService.moveFile(fileId.toString(), targetGroupId.toString()))
                .expectNextCount(1)
                .verifyComplete();

        verify(storageFileRepository).save(argThat(f -> !f.isNew()));
    }
}
