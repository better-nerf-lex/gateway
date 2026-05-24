package com.bntech.lexai.gateway.mapper.document;

import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseMapper {

    @Mapping(target = "parties", ignore = true)
    @Mapping(target = "events", ignore = true)
    CaseDto toDto(LegalCase entity);

    @Mapping(target = "isNew", ignore = true)
    LegalCase toEntity(CaseDto dto);
}
