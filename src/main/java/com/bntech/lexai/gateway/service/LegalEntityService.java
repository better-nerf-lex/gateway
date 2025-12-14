package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.rest.mapper.LegalEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;
    private final LegalEntityMapper legalEntityMapper;

    public Flux<LegalEntityDto> getAllLegalEntities() {
        return legalEntityRepository.findAll()
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> getLegalEntity(String id) {
        return legalEntityRepository.findById(id)
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> createLegalEntity(LegalEntityDto legalEntityDto) {
        LegalEntity<EntityDetails> legalEntity = legalEntityMapper.toEntity(legalEntityDto);
        if (legalEntity.getId() == null) {
            legalEntity.setId(UUID.randomUUID().toString());
        }
        legalEntity.setNew(true);

        return legalEntityRepository.save(legalEntity)
                .map(legalEntityMapper::toDto);
    }

    public Mono<LegalEntityDto> updateLegalEntity(String id, LegalEntityDto legalEntityDto) {
        return legalEntityRepository.findById(id)
                .flatMap(existingEntity -> {
                    LegalEntity<EntityDetails> updatedEntity = legalEntityMapper.toEntity(legalEntityDto);
                    updatedEntity.setId(id);
                    updatedEntity.setNew(false);
                    // Preserve documentId if it exists in the existing entity and is not provided
                    // (though for global entities it might be null)
                    if (updatedEntity.getDocumentId() == null) {
                        updatedEntity.setDocumentId(existingEntity.getDocumentId());
                    }
                    return legalEntityRepository.save(updatedEntity);
                })
                .map(legalEntityMapper::toDto);
    }

    public Mono<Void> deleteLegalEntity(String id) {
        return legalEntityRepository.deleteById(id);
    }
}
