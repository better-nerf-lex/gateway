
package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.object.details.CompanyDetails;
import com.bntech.lexai.gateway.data.object.details.CourtDetails;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.object.details.LawFirmDetails;
import com.bntech.lexai.gateway.data.object.details.PersonDetails;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.LegalEntityDto;
import com.bntech.lexai.gateway.rest.dto.document.details.CompanyDetailsDto;
import com.bntech.lexai.gateway.rest.dto.document.details.CourtDetailsDto;
import com.bntech.lexai.gateway.rest.dto.document.details.EntityDetailsDto;
import com.bntech.lexai.gateway.rest.dto.document.details.LawFirmDetailsDto;
import com.bntech.lexai.gateway.rest.dto.document.details.PersonDetailsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "status", source = "status")
    DocumentDto toDto(Document entity);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "new", ignore = true)
    Document toEntity(DocumentDto dto);

    @Mapping(target = "details", expression = "java(mapDetailsToDto(entity.getDetails()))")
    @Mapping(target = "name", ignore = true)
    LegalEntityDto toDto(LegalEntity<EntityDetails> entity);

    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "new", ignore = true)
    @Mapping(target = "details", expression = "java(mapDetailsToEntity(dto.getDetails()))")
    LegalEntity<EntityDetails> toEntity(LegalEntityDto dto);

    @Mapping(target = "document", ignore = true)
    @Mapping(target = "new", ignore = true)
    List<LegalEntity<EntityDetails>> toEntity(List<LegalEntityDto> dto);

    PersonDetailsDto toDto(PersonDetails details);
    CompanyDetailsDto toDto(CompanyDetails details);
    CourtDetailsDto toDto(CourtDetails details);
    LawFirmDetailsDto toDto(LawFirmDetails details);

    PersonDetails toEntity(PersonDetailsDto detailsDto);
    CompanyDetails toEntity(CompanyDetailsDto detailsDto);
    CourtDetails toEntity(CourtDetailsDto detailsDto);
    LawFirmDetails toEntity(LawFirmDetailsDto detailsDto);

    default EntityDetailsDto mapDetailsToDto(EntityDetails details) {
        if (details == null) return null;
        if (details instanceof PersonDetails pd) return toDto(pd);
        if (details instanceof CompanyDetails cd) return toDto(cd);
        if (details instanceof CourtDetails cd2) return toDto(cd2);
        if (details instanceof LawFirmDetails lf) return toDto(lf);
        throw new IllegalArgumentException("Unsupported EntityDetails subtype: " + details.getClass());
    }

    default EntityDetails mapDetailsToEntity(EntityDetailsDto detailsDto) {
        if (detailsDto == null) return null;
        if (detailsDto instanceof PersonDetailsDto pd) return toEntity(pd);
        if (detailsDto instanceof CompanyDetailsDto cd) return toEntity(cd);
        if (detailsDto instanceof CourtDetailsDto cd2) return toEntity(cd2);
        if (detailsDto instanceof LawFirmDetailsDto lf) return toEntity(lf);
        throw new IllegalArgumentException("Unsupported EntityDetailsDto subtype: " + detailsDto.getClass());
    }

    ActivityDto toDto(Activity entity);

    Activity toEntity(ActivityDto dto);
}
