package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StorageFileMapper {

    StorageFileDto toDto(StorageFile entity);

    StorageFile toEntity(StorageFileDto dto);
}
