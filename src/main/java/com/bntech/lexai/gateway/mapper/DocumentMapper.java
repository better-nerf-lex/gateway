package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.DocumentStepData;
import com.bntech.lexai.gateway.rest.dto.document.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStepDataDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "status", source = "status")
    DocumentDto toDto(Document entity);

    @Mapping(target = "status", source = "status")
    Document toEntity(DocumentDto dto);

    @Mapping(target = "questionType", source = "questionType")
    DocumentStepDataDto toDto(DocumentStepData entity);

    @Mapping(target = "document", ignore = true)
    DocumentStepData toEntity(DocumentStepDataDto dto);

    ActivityDto toDto(Activity entity);

    Activity toEntity(ActivityDto dto);
}
