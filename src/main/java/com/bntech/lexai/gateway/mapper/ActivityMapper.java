package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.data.object.ActionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {UUID.class, ActionType.class})
public interface ActivityMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "action", expression = "java(entity.getAction() != null ? entity.getAction().getValue() : null)")
    @Mapping(target = "date", source = "createdAt")
    @Mapping(target = "teamId", expression = "java(entity.getTeamId() != null ? entity.getTeamId().toString() : null)")
    @Mapping(target = "entityId", expression = "java(entity.getEntityId() != null ? entity.getEntityId().toString() : null)")
    @Mapping(target = "username", ignore = true)
    ActivityDto toDto(Activity entity);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? UUID.fromString(dto.getId()) : null)")
    @Mapping(target = "action", expression = "java(dto.getAction() != null ? ActionType.fromValue(dto.getAction()) : null)")
    @Mapping(target = "createdAt", source = "date")
    @Mapping(target = "teamId", expression = "java(dto.getTeamId() != null ? UUID.fromString(dto.getTeamId()) : null)")
    @Mapping(target = "entityId", expression = "java(dto.getEntityId() != null ? UUID.fromString(dto.getEntityId()) : null)")
    Activity toEntity(ActivityDto dto);
}
