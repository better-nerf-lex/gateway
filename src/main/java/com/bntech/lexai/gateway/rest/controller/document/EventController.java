package com.bntech.lexai.gateway.rest.controller.document;

import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.service.document.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    @PreAuthorize("@access.eventWrite(authentication, #id)")
    public Mono<Void> deleteEvent(@PathVariable String id) {
        return eventService.deleteEvent(id);
    }

    @PostMapping("/{id}/participants/{legalEntityId}")
    @PreAuthorize("@access.eventWrite(authentication, #id)")
    public Mono<Void> addParticipant(@PathVariable String id, @PathVariable String legalEntityId) {
        return eventService.addParticipant(id, legalEntityId);
    }

    @DeleteMapping("/{id}/participants/{legalEntityId}")
    @PreAuthorize("@access.eventWrite(authentication, #id)")
    public Mono<Void> removeParticipant(@PathVariable String id, @PathVariable String legalEntityId) {
        return eventService.removeParticipant(id, legalEntityId);
    }
}
