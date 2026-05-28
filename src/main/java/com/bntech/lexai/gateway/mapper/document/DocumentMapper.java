package com.bntech.lexai.gateway.mapper.document;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.data.object.DocumentGenerationMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.bntech.lexai.gateway.data.object.ActionType;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {LegalEntityMapper.class}, imports = {ActionType.class, java.util.UUID.class})
public interface DocumentMapper {

    @Mapping(target = "status", source = "status")
    @Mapping(target = "documentGroupId", expression = "java(entity.getDocumentGroupId() != null ? entity.getDocumentGroupId().toString() : null)")
    DocumentDto toDto(Document entity);

    @AfterMapping
    default void ensureGenerationMetadata(Document entity, @MappingTarget DocumentDto dto) {
        if (dto.getGenerationMetadata() == null) {
            dto.setGenerationMetadata(DocumentGenerationMetadata.from(
                    List.of(),
                    List.of(),
                    entity != null ? entity.getId() : null,
                    entity != null ? entity.getDocumentGroupId() : null));
        }
    }

    @Mapping(target = "status", source = "status")
    @Mapping(target = "documentGroupId", expression = "java(dto.getDocumentGroupId() != null ? UUID.fromString(dto.getDocumentGroupId()) : null)")
    Document toEntity(DocumentDto dto);
}
