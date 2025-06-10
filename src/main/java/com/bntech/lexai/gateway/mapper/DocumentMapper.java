package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "status", source = "status")
    DocumentDto toDto(Document entity);

    @Mapping(target = "status", source = "status")
    Document toEntity(DocumentDto dto);

    LegalEntityDto toDto(LegalEntity entity);

    @Mapping(target = "documentId", ignore = true)
    LegalEntity toEntity(LegalEntityDto dto);

    @Mapping(target = "document", ignore = true)
    List<LegalEntity> toEntity(List<LegalEntityDto> dto);

    ActivityDto toDto(Activity entity);

    Activity toEntity(ActivityDto dto);
}
