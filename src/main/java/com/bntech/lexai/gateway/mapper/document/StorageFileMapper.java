package com.bntech.lexai.gateway.mapper.document;

import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {UUID.class})
public interface StorageFileMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "documentGroupId", expression = "java(entity.getDocumentGroupId() != null ? entity.getDocumentGroupId().toString() : null)")
    @Mapping(target = "teamId", expression = "java(entity.getTeamId() != null ? entity.getTeamId().toString() : null)")
    StorageFileDto toDto(StorageFile entity);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? UUID.fromString(dto.getId()) : null)")
    @Mapping(target = "documentGroupId", expression = "java(dto.getDocumentGroupId() != null ? UUID.fromString(dto.getDocumentGroupId()) : null)")
    @Mapping(target = "teamId", expression = "java(dto.getTeamId() != null ? UUID.fromString(dto.getTeamId()) : null)")
    StorageFile toEntity(StorageFileDto dto);
}
