package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {DocumentFileMapper.class})
public interface DocumentGroupMapper {

    @Mapping(target = "files", ignore = true)
    DocumentGroupDto toDto(DocumentGroup entity);

    DocumentGroup toEntity(DocumentGroupDto dto);
}
