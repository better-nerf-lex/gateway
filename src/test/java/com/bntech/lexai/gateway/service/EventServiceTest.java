package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.Event;
import com.bntech.lexai.gateway.data.entity.document.EventParticipant;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.*;
import com.bntech.lexai.gateway.mapper.EventMapper;
import com.bntech.lexai.gateway.mapper.LegalEntityMapper;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventParticipantRepository eventParticipantRepository;
    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private LegalEntityMapper legalEntityMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private Authentication authentication;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository,
                eventMapper,
                eventParticipantRepository,
                legalEntityRepository,
                legalEntityMapper,
                userRepository,
                teamMemberRepository
        );
    }

    @Test
    void createEvent_ShouldSaveParticipants_WhenParticipantsArePresent() {
        // Arrange
        String username = "testuser";
        Long userId = 1L;
        UUID caseId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID legalEntityId = UUID.randomUUID();

        EventDto eventDto = EventDto.builder()
                .caseId(caseId.toString())
                .participants(List.of(LegalEntityDto.builder().id(legalEntityId.toString()).build()))
                .build();

        User user = User.builder().id(userId).username(username).build();
        Event event = new Event();
        event.setId(eventId);
        event.setCaseId(caseId);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        SecurityContext securityContext = new SecurityContextImpl(authentication);

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(eventMapper.toEntity(eventDto)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(event));
        when(eventParticipantRepository.save(any(EventParticipant.class))).thenReturn(Mono.just(new EventParticipant()));
        
        // Mocking for enrichEvent
        when(eventMapper.toDto(any(Event.class))).thenReturn(eventDto);
        when(eventParticipantRepository.findByEventId(eventId)).thenReturn(Flux.empty()); // Initially empty for the enrichEvent call in the current implementation

        // Act
        StepVerifier.create(eventService.createEvent(eventDto)
                        .contextWrite(ctx -> ctx.put(SecurityContext.class, Mono.just(securityContext))))
                .assertNext(dto -> {
                    // Verify
                    // We expect participants to be saved
                })
                .verifyComplete();

        // Check if eventParticipantRepository.save was called
        verify(eventParticipantRepository).save(any(EventParticipant.class));
    }
}
