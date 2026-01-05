package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {DocumentFileMapper.class}, imports = {java.util.UUID.class})
public interface DocumentGroupMapper {

    @Mapping(target = "files", ignore = true)
    @Mapping(target = "caseId", expression = "java(entity.getCaseId() != null ? entity.getCaseId().toString() : null)")
    DocumentGroupDto toDto(DocumentGroup entity);

    @Mapping(target = "caseId", expression = "java(dto.getCaseId() != null ? UUID.fromString(dto.getCaseId()) : null)")
    DocumentGroup toEntity(DocumentGroupDto dto);
}
