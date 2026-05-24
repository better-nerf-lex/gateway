package com.bntech.lexai.gateway.mapper.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentExampleMapper {
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    DocumentExampleDto toDto(DocumentExample entity);

    @Mapping(target = "name", source = "title")
    @Mapping(target = "exampleId", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    SubmitPromptResponse toSubmitPromptResponse(DocumentExample entity);

    @Mapping(target = "name", source = "title")
    @Mapping(target = "exampleId", source = "id")
    SubmitPromptResponse toSubmitPromptResponseFromEs(DocumentExampleEs es);

    @Mapping(target = "name", source = "title")
    @Mapping(target = "exampleId", source = "id")
    SubmitPromptResponse toSubmitPromptResponseFromDto(DocumentExampleDto dto);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? java.util.UUID.fromString(dto.getId()) : null)")
    DocumentExample toEntity(DocumentExampleDto dto);

    default DocumentExampleEs toEs(DocumentExample entity) {
        return DocumentExampleEs.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .questions(entity.getQuestions())
                .base(entity.getBase())
                .requirements(entity.getRequirements())
                .templateFiles(entity.getTemplateFiles())
                .build();
    }

    default DocumentExampleDto toDtoFromEs(DocumentExampleEs es) {
        return DocumentExampleDto.builder()
                .id(es.getId())
                .title(es.getTitle())
                .description(es.getDescription())
                .questions(es.getQuestions())
                .base(es.getBase())
                .requirements(es.getRequirements())
                .templateFiles(es.getTemplateFiles())
                .build();
    }
}
