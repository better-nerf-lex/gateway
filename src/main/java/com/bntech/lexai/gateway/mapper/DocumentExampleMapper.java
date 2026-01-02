package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentExampleMapper {
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    DocumentExampleDto toDto(DocumentExample entity);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? java.util.UUID.fromString(dto.getId()) : null)")
    DocumentExample toEntity(DocumentExampleDto dto);
}
