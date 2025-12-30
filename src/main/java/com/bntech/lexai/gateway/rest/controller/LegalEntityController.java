package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.service.LegalEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/legal-entities")
@RequiredArgsConstructor
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    @GetMapping
    @PreAuthorize("@access.canListLegalEntities(authentication, #teamId)")
    public Flux<LegalEntityDto> getAllLegalEntities(@RequestParam(required = false) String teamId) {
        return legalEntityService.getAllLegalEntities(teamId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.canViewLegalEntity(authentication, #id)")
    public Mono<LegalEntityDto> getLegalEntity(@PathVariable String id) {
        return legalEntityService.getLegalEntity(id);
    }

    @PostMapping
    public Mono<LegalEntityDto> createLegalEntity(@RequestBody LegalEntityDto legalEntityDto) {
        return legalEntityService.createLegalEntity(legalEntityDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.canUpdateLegalEntity(authentication, #id)")
    public Mono<LegalEntityDto> updateLegalEntity(@PathVariable String id, @RequestBody LegalEntityDto legalEntityDto) {
        return legalEntityService.updateLegalEntity(id, legalEntityDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.canDeleteLegalEntity(authentication, #id)")
    public Mono<Void> deleteLegalEntity(@PathVariable String id) {
        return legalEntityService.deleteLegalEntity(id);
    }
}
