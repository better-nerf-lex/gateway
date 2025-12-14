package com.bntech.lexai.gateway.rest.mapper;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.object.RelevanceType;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.rest.dto.document.details.EntityDetailsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegalEntityMapper {

    private final ObjectMapper objectMapper;

    public LegalEntityDto toDto(LegalEntity<EntityDetails> entity) {
        if (entity == null) {
            return null;
        }
        return LegalEntityDto.builder()
                .id(entity.getId())
                .name(extractName(entity.getDetails()))
                .relevance(entity.getRelevance() != null ? entity.getRelevance().name() : null)
                .details(objectMapper.convertValue(entity.getDetails(), EntityDetailsDto.class))
                .build();
    }

    public LegalEntity<EntityDetails> toEntity(LegalEntityDto dto) {
        if (dto == null) {
            return null;
        }
        LegalEntity<EntityDetails> entity = new LegalEntity<>();
        entity.setId(dto.getId());
        if (dto.getRelevance() != null) {
            try {
                entity.setRelevance(RelevanceType.valueOf(dto.getRelevance()));
            } catch (IllegalArgumentException e) {
                // Handle invalid enum value or default to null/LOW
            }
        }
        if (dto.getDetails() != null) {
            entity.setDetails(objectMapper.convertValue(dto.getDetails(), EntityDetails.class));
        }
        return entity;
    }

    private String extractName(EntityDetails details) {
        if (details == null) {
            return null;
        }
        // This logic depends on the specific fields of EntityDetails subclasses
        // We can use reflection or JSON conversion, or just check types if we had
        // access to subclasses here easily
        // For now, let's try to get it from the map if possible or cast
        // Since EntityDetails is abstract and we use Jackson, we can rely on specific
        // fields being present in the object

        try {
            java.util.Map<String, Object> map = objectMapper.convertValue(details, java.util.Map.class);
            if (map.containsKey("fullName")) {
                return (String) map.get("fullName");
            }
            if (map.containsKey("firstName") && map.containsKey("lastName")) {
                return map.get("firstName") + " " + map.get("lastName");
            }
        } catch (Exception e) {
            // ignore
        }
        return "Unknown Entity";
    }
}
