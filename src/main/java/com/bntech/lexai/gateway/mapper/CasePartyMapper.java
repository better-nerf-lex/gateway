package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.CaseParty;
import com.bntech.lexai.gateway.rest.dto.document.CasePartyDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CasePartyMapper {

    @Mapping(target = "legalEntity", ignore = true)
    CasePartyDto toDto(CaseParty entity);

    CaseParty toEntity(CasePartyDto dto);
}
