package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.DocumentFile;
import com.bntech.lexai.gateway.rest.dto.document.DocumentFileDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentFileMapper {

    DocumentFileDto toDto(DocumentFile entity);

    DocumentFile toEntity(DocumentFileDto dto);
}
