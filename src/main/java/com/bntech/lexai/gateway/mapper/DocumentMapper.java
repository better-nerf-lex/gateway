
package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.bntech.lexai.gateway.data.object.ActionType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {LegalEntityMapper.class}, imports = {ActionType.class})
public interface DocumentMapper {

    @Mapping(target = "status", source = "status")
    DocumentDto toDto(Document entity);

    @Mapping(target = "status", source = "status")
    Document toEntity(DocumentDto dto);

    ActivityDto toDto(Activity entity);

    @Mapping(target = "action", expression = "java(ActionType.fromValue(dto.getAction()))")
    @Mapping(target = "createdAt", source = "date")
    Activity toEntity(ActivityDto dto);
}
