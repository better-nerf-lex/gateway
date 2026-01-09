package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.service.LegalEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/legal-entities")
@RequiredArgsConstructor
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    @GetMapping
    @PreAuthorize("@access.teamRead(authentication, #teamId)")
    public Flux<LegalEntityDto> getAllLegalEntities(@RequestParam(required = false) String teamId) {
        return legalEntityService.getAllLegalEntities(teamId);
    }

    @GetMapping("/{id}")
    public Mono<LegalEntityDto> getLegalEntity(@PathVariable String id) {
        return legalEntityService.getLegalEntity(id);
    }

    @PostMapping
    public Mono<LegalEntityDto> createLegalEntity(@RequestBody LegalEntityDto legalEntityDto) {
        return legalEntityService.createLegalEntity(legalEntityDto);
    }

    @PutMapping("/{id}")
    public Mono<LegalEntityDto> updateLegalEntity(@PathVariable String id, @RequestBody LegalEntityDto legalEntityDto) {
        return legalEntityService.updateLegalEntity(id, legalEntityDto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteLegalEntity(@PathVariable String id) {
        return legalEntityService.deleteLegalEntity(id);
    }

    @GetMapping("/search")
    public Flux<LegalEntityDto> searchLegalEntities(@RequestParam String query) {
        return legalEntityService.searchLegalEntities(query);
    }
}
