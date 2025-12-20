package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.service.LegalEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.bntech.lexai.gateway.security.access.TeamAccess;
import com.bntech.lexai.gateway.security.access.UserAccess;
import static com.bntech.lexai.gateway.security.access.AccessLevel.READ;
import static com.bntech.lexai.gateway.security.access.AccessLevel.WRITE;

@RestController
@RequestMapping("/legal-entities")
@RequiredArgsConstructor
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    @GetMapping
    @TeamAccess(level = READ)
    public Flux<LegalEntityDto> getAllLegalEntities(@RequestParam(required = false) String teamId) {
        return legalEntityService.getAllLegalEntities(teamId);
    }

    @GetMapping("/{id}")
    @TeamAccess(level = READ)
    @UserAccess(level = READ)
    public Mono<LegalEntityDto> getLegalEntity(@PathVariable String id) {
        return legalEntityService.getLegalEntity(id);
    }

    @PostMapping
    public Mono<LegalEntityDto> createLegalEntity(@RequestBody LegalEntityDto legalEntityDto) {
        return legalEntityService.createLegalEntity(legalEntityDto);
    }

    @PutMapping("/{id}")
    @TeamAccess(level = WRITE)
    @UserAccess(level = WRITE)
    public Mono<LegalEntityDto> updateLegalEntity(@PathVariable String id, @RequestBody LegalEntityDto legalEntityDto) {
        return legalEntityService.updateLegalEntity(id, legalEntityDto);
    }

    @DeleteMapping("/{id}")
    @TeamAccess(level = WRITE)
    @UserAccess(level = WRITE)
    public Mono<Void> deleteLegalEntity(@PathVariable String id) {
        return legalEntityService.deleteLegalEntity(id);
    }
}
