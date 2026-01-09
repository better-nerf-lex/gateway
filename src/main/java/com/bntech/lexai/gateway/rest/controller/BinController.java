package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import com.bntech.lexai.gateway.rest.dto.team.TeamDto;
import com.bntech.lexai.gateway.service.CaseService;
import com.bntech.lexai.gateway.service.DocumentGroupService;
import com.bntech.lexai.gateway.service.DocumentService;
import com.bntech.lexai.gateway.service.EventService;
import com.bntech.lexai.gateway.service.LegalEntityService;
import com.bntech.lexai.gateway.service.StorageFileService;
import com.bntech.lexai.gateway.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bin")
@RequiredArgsConstructor
public class BinController {

    private final CaseService caseService;
    private final DocumentService documentService;
    private final DocumentGroupService documentGroupService;
    private final StorageFileService storageFileService;
    private final LegalEntityService legalEntityService;
    private final EventService eventService;
    private final TeamService teamService;

    @GetMapping("/cases")
    @PreAuthorize("@access.teamRead(authentication, #teamId)")
    public Flux<CaseDto> getDeletedCases(@RequestParam(required = false) String teamId) {
        return caseService.getDeletedCases(teamId);
    }

    @PostMapping("/cases/{id}/restore")
    public Mono<CaseDto> restoreCase(@PathVariable String id) {
        return caseService.restoreCase(id);
    }

    @DeleteMapping("/cases/{id}")
    public Mono<Void> permanentlyDeleteCase(@PathVariable String id) {
        return caseService.permanentlyDeleteCase(id);
    }

    @GetMapping("/documents")
    public Flux<DocumentDto> getDeletedDocuments(@RequestParam(required = false) String teamId) {
        return documentService.getDeletedDocuments(teamId);
    }

    @PostMapping("/documents/{id}/restore")
    public Mono<DocumentDto> restoreDocument(@PathVariable String id) {
        return documentService.restoreDocument(id);
    }

    @DeleteMapping("/documents/{id}")
    public Mono<Void> permanentlyDeleteDocument(@PathVariable String id) {
        return documentService.permanentlyDeleteDocument(id);
    }

    @GetMapping("/document-groups")
    public Flux<DocumentGroupDto> getDeletedDocumentGroups(@RequestParam(required = false) String teamId) {
        return documentGroupService.getDeletedDocumentGroups(teamId);
    }

    @PostMapping("/document-groups/{id}/restore")
    public Mono<DocumentGroupDto> restoreDocumentGroup(@PathVariable String id) {
        return documentGroupService.restoreDocumentGroup(id);
    }

    @DeleteMapping("/document-groups/{id}")
    public Mono<Void> permanentlyDeleteDocumentGroup(@PathVariable String id) {
        return documentGroupService.permanentlyDeleteDocumentGroup(id);
    }

    @GetMapping("/storage-files")
    public Flux<StorageFileDto> getDeletedStorageFiles(@RequestParam(required = false) String teamId) {
        return storageFileService.getDeletedDocumentFiles(teamId);
    }

    @PostMapping("/storage-files/{id}/restore")
    public Mono<StorageFileDto> restoreStorageFile(@PathVariable String id) {
        return storageFileService.restoreDocumentFile(id);
    }

    @DeleteMapping("/storage-files/{id}")
    public Mono<Void> permanentlyDeleteStorageFile(@PathVariable String id) {
        return storageFileService.permanentlyDeleteDocumentFile(id);
    }

    @GetMapping("/legal-entities")
    public Flux<LegalEntityDto> getDeletedLegalEntities(@RequestParam(required = false) String teamId) {
        return legalEntityService.getDeletedLegalEntities(teamId);
    }

    @PostMapping("/legal-entities/{id}/restore")
    public Mono<LegalEntityDto> restoreLegalEntity(@PathVariable String id) {
        return legalEntityService.restoreLegalEntity(id);
    }

    @DeleteMapping("/legal-entities/{id}")
    public Mono<Void> permanentlyDeleteLegalEntity(@PathVariable String id) {
        return legalEntityService.permanentlyDeleteLegalEntity(id);
    }

    @GetMapping("/events")
    public Flux<EventDto> getDeletedEvents(@RequestParam(required = false) String caseId) {
        return eventService.getDeletedEvents(caseId);
    }

    @PostMapping("/events/{id}/restore")
    public Mono<EventDto> restoreEvent(@PathVariable String id) {
        return eventService.restoreEvent(id);
    }

    @DeleteMapping("/events/{id}")
    public Mono<Void> permanentlyDeleteEvent(@PathVariable String id) {
        return eventService.permanentlyDeleteEvent(id);
    }

    @GetMapping("/teams")
    public Flux<TeamDto> getDeletedTeams() {
        return teamService.getDeletedTeams();
    }

    @PostMapping("/teams/{id}/restore")
    public Mono<TeamDto> restoreTeam(@PathVariable String id) {
        return teamService.restoreTeam(id);
    }

    @DeleteMapping("/teams/{id}")
    public Mono<Void> permanentlyDeleteTeam(@PathVariable String id) {
        return teamService.permanentlyDeleteTeam(id);
    }
}
