package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping()
    public Mono<EventDto> createEvent(@RequestBody EventDto eventDto) {
        return eventService.createEvent(eventDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isOwnerOrAdminOfEvent(authentication, #id)")
    public Mono<Void> deleteEvent(@PathVariable String id) {
        return eventService.deleteEvent(id);
    }

    @PostMapping("/{id}/participants/{legalEntityId}")
    @PreAuthorize("@access.isOwnerOrAdminOfEvent(authentication, #id)")
    public Mono<Void> addParticipant(@PathVariable String id, @PathVariable String legalEntityId) {
        return eventService.addParticipant(id, legalEntityId);
    }

    @DeleteMapping("/{id}/participants/{legalEntityId}")
    @PreAuthorize("@access.isOwnerOrAdminOfEvent(authentication, #id)")
    public Mono<Void> removeParticipant(@PathVariable String id, @PathVariable String legalEntityId) {
        return eventService.removeParticipant(id, legalEntityId);
    }
}
