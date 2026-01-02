package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.Case;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseMapper {

    CaseDto toDto(Case entity);

    Case toEntity(CaseDto dto);
}
