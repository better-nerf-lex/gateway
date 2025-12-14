package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.service.LegalEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/legal-entities")
@RequiredArgsConstructor
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    @GetMapping
    public Flux<LegalEntityDto> getAllLegalEntities() {
        return legalEntityService.getAllLegalEntities();
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
}
