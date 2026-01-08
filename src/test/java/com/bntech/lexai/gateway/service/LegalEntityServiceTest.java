package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.object.ActionType;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.object.RelevanceType;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.object.details.PersonDetails;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.mapper.LegalEntityMapper;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.rest.dto.document.details.PersonDetailsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegalEntityServiceTest {

    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private LegalEntityMapper legalEntityMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private ActivityService activityService;

    private LegalEntityService legalEntityService;

    @BeforeEach
    void setUp() {
        legalEntityService = new LegalEntityService(
                legalEntityRepository,
                legalEntityMapper,
                userRepository,
                teamMemberRepository,
                activityService
        );
    }

    @Test
    void updateLegalEntity_ShouldPreserveVersionAndInternalFields() {
        UUID id = UUID.randomUUID();
        Integer version = 1;
        Long ownerId = 123L;
        UUID teamId = UUID.randomUUID();

        LegalEntity<EntityDetails> existingEntity = new LegalEntity<>();
        existingEntity.setId(id);
        existingEntity.setVersion(version);
        existingEntity.setOwnerId(ownerId);
        existingEntity.setTeamId(teamId);
        existingEntity.setDetails(new PersonDetails());
        existingEntity.setRelevance(RelevanceType.MEDIUM);

        PersonDetailsDto detailsDto = new PersonDetailsDto();
        LegalEntityDto updateDto = LegalEntityDto.builder()
                .id(id.toString())
                .relevance("HIGH")
                .details(detailsDto)
                .build();

        LegalEntity<EntityDetails> mappedEntity = new LegalEntity<>();
        mappedEntity.setRelevance(RelevanceType.HIGH);
        mappedEntity.setDetails(new PersonDetails());
        // mappedEntity has version = null, ownerId = null by default

        when(legalEntityRepository.findById(id)).thenReturn(Mono.just(existingEntity));
        when(legalEntityRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(legalEntityMapper.toDto(any(LegalEntity.class))).thenReturn(new LegalEntityDto());

        StepVerifier.create(legalEntityService.updateLegalEntity(id.toString(), updateDto))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<LegalEntity> captor = ArgumentCaptor.forClass(LegalEntity.class);
        verify(legalEntityRepository).save(captor.capture());

        LegalEntity saved = captor.getValue();

        // These assertions are expected to FAIL before the fix
        assertThat(saved.getVersion()).isEqualTo(version);
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getTeamId()).isEqualTo(teamId);
    }

    @Test
    void deleteLegalEntity_ShouldLogActivity() {
        UUID id = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        LegalEntity<EntityDetails> entity = new LegalEntity<>();
        entity.setId(id);
        entity.setTeamId(teamId);
        entity.setOwnerId(1L);
        entity.setDetails(new PersonDetails());

        when(legalEntityRepository.softDelete(id)).thenReturn(Mono.empty());
        // In the current buggy implementation, findById is called AFTER softDelete via flatMap
        // But since softDelete returns Mono.empty(), findById is NEVER called.
        when(legalEntityRepository.findById(id)).thenReturn(Mono.just(entity));
        when(legalEntityMapper.mapName(any())).thenReturn("Test Entity");

        StepVerifier.create(legalEntityService.deleteLegalEntity(id.toString()))
                .verifyComplete();

        // This verification will fail if the flatMap is skipped
        verify(activityService).logActivity(any(), eq(teamId), eq(ActionType.ENTITY_DELETE), eq(id), eq("LegalEntity"), anyString());
    }
}
