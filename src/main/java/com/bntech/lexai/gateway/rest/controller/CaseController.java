package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.data.object.CasePartyRole;
import com.bntech.lexai.gateway.rest.dto.document.AddPartiesRequest;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.service.CaseService;
import com.bntech.lexai.gateway.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final EventService eventService;

    @GetMapping()
    @PreAuthorize("@access.canListCases(authentication, #teamId)")
    public Flux<CaseDto> getCases(@RequestParam(required = false) String teamId) {
        return caseService.getCases(teamId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.canViewCase(authentication, #id)")
    public Mono<CaseDto> getCase(@PathVariable String id) {
        return caseService.getCase(id);
    }

    @PostMapping()
    public Mono<CaseDto> createCase(@RequestBody CaseDto caseDto) {
        return caseService.createCase(caseDto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.canUpdateCase(authentication, #id)")
    public Mono<CaseDto> updateCase(@PathVariable String id, @RequestBody CaseDto caseDto) {
        return caseService.updateCase(id, caseDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.canDeleteCase(authentication, #id)")
    public Mono<Void> deleteCase(@PathVariable String id) {
        return caseService.deleteCase(id);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("@access.canViewCase(authentication, #id)")
    public Flux<DocumentDto> getCaseDocuments(@PathVariable String id) {
        return caseService.getCaseDocuments(id);
    }

    @PostMapping("/{id}/documents/{documentId}")
    @PreAuthorize("@access.canUpdateCase(authentication, #id)")
    public Mono<Void> assignDocument(@PathVariable String id, @PathVariable String documentId) {
        return caseService.assignDocument(id, documentId);
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("@access.canUpdateCase(authentication, #id)")
    public Mono<Void> removeDocument(@PathVariable String id, @PathVariable String documentId) {
        return caseService.removeDocument(id, documentId);
    }

    @PostMapping("/{id}/parties")
    @PreAuthorize("@access.canUpdateCase(authentication, #id)")
    public Mono<Void> addParties(@PathVariable String id, @RequestBody AddPartiesRequest request) {
        return caseService.addParties(id, request.getParties());
    }

    @DeleteMapping("/{id}/parties/{legalEntityId}")
    @PreAuthorize("@access.canUpdateCase(authentication, #id)")
    public Mono<Void> removeParty(@PathVariable String id, @PathVariable String legalEntityId,
            @RequestParam CasePartyRole role) {
        return caseService.removeParty(id, legalEntityId, role);
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("@access.canViewCase(authentication, #id)")
    public Flux<EventDto> getCaseEvents(@PathVariable String id) {
        return eventService.getCaseEvents(id);
    }
}
