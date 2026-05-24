package com.bntech.lexai.gateway.mapper.document;

import com.bntech.lexai.gateway.data.entity.document.Event;
import com.bntech.lexai.gateway.rest.dto.document.EventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    @Mapping(target = "participants", ignore = true)
    EventDto toDto(Event entity);

    @Mapping(target = "isNew", ignore = true)
    Event toEntity(EventDto dto);
}
