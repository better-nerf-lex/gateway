package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.data.object.CasePartyRole;
import com.bntech.lexai.gateway.rest.dto.document.AddPartiesRequest;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.security.RequireSubscription;
import com.bntech.lexai.gateway.service.CaseService;
import com.bntech.lexai.gateway.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final EventService eventService;

    @GetMapping()
    @PreAuthorize("@access.teamRead(authentication, #teamId)")
    public Flux<CaseDto> getCases(@RequestParam(required = false) String teamId) {
        return caseService.getCases(teamId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.caseRead(authentication, #id)")
    public Mono<CaseDto> getCase(@PathVariable String id) {
        return caseService.getCase(id);
    }

    @PostMapping()
    @RequireSubscription(entityType = RequireSubscription.SubscriptionEntityType.TEAM,
                         message = "Active team subscription required to create cases")
    public Mono<CaseDto> createCase(@RequestBody CaseDto caseDto) {
        return caseService.createCase(caseDto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.caseWrite(authentication, #id)")
    public Mono<CaseDto> updateCase(@PathVariable String id, @RequestBody CaseDto caseDto) {
        return caseService.updateCase(id, caseDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.caseWrite(authentication, #id)")
    public Mono<Void> deleteCase(@PathVariable String id) {
        return caseService.deleteCase(id);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("@access.caseRead(authentication, #id)")
    public Flux<DocumentDto> getCaseDocuments(@PathVariable String id) {
        return caseService.getCaseDocuments(id);
    }

    @PostMapping("/{id}/documents/{documentId}")
    @PreAuthorize("@access.caseWrite(authentication, #id)")
    public Mono<Void> assignDocument(@PathVariable String id, @PathVariable String documentId) {
        return caseService.assignDocument(id, documentId);
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("@access.caseWrite(authentication, #id)")
    public Mono<Void> removeDocument(@PathVariable String id, @PathVariable String documentId) {
        return caseService.removeDocument(id, documentId);
    }

    @PostMapping("/{id}/parties")
    @PreAuthorize("@access.caseWrite(authentication, #id)")
    public Mono<Void> addParties(@PathVariable String id, @RequestBody AddPartiesRequest request) {
        return caseService.addParties(id, request.getParties());
    }

    @DeleteMapping("/{id}/parties/{legalEntityId}")
    @PreAuthorize("@access.caseWrite(authentication, #id)")
    public Mono<Void> removeParty(@PathVariable String id, @PathVariable String legalEntityId,
            @RequestParam CasePartyRole role) {
        return caseService.removeParty(id, legalEntityId, role);
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("@access.caseRead(authentication, #id)")
    public Flux<EventDto> getCaseEvents(@PathVariable String id) {
        return eventService.getCaseEvents(id);
    }
}
