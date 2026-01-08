package com.bntech.lexai.gateway.mapper;

import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.object.RelevanceType;
import com.bntech.lexai.gateway.data.object.details.CompanyDetails;
import com.bntech.lexai.gateway.data.object.details.CourtDetails;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.bntech.lexai.gateway.data.object.details.LawFirmDetails;
import com.bntech.lexai.gateway.data.object.details.PersonDetails;
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
public interface LegalEntityMapper {

    @Mapping(target = "details", expression = "java(mapDetailsToDto(entity.getDetails()))")
    @Mapping(target = "name", expression = "java(mapName(entity.getDetails()))")
    LegalEntityDto toDto(LegalEntity<? extends EntityDetails> entity);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? UUID.fromString(dto.getId()) : null)")
    @Mapping(target = "details", expression = "java(mapDetailsToEntity(dto.getDetails()))")
    @Mapping(target = "relevance", expression = "java(mapRelevance(dto.getRelevance()))")
    @Mapping(target = "documentId", ignore = true)
    LegalEntity<EntityDetails> toEntity(LegalEntityDto dto);

    @Mapping(target = "id", expression = "java(dto.getId() != null ? UUID.fromString(dto.getId()) : null)")
    @Mapping(target = "documentId", ignore = true)
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
        return switch (details) {
            case null -> null;
            case PersonDetails pd -> toDto(pd);
            case CompanyDetails cd -> toDto(cd);
            case CourtDetails cd2 -> toDto(cd2);
            case LawFirmDetails lf -> toDto(lf);
            default -> throw new IllegalArgumentException("Unsupported EntityDetails subtype: " + details.getClass());
        };
    }

    default EntityDetails mapDetailsToEntity(EntityDetailsDto detailsDto) {
        return switch (detailsDto) {
            case null -> null;
            case PersonDetailsDto pd -> toEntity(pd);
            case CompanyDetailsDto cd -> toEntity(cd);
            case CourtDetailsDto cd2 -> toEntity(cd2);
            case LawFirmDetailsDto lf -> toEntity(lf);
            default ->
                    throw new IllegalArgumentException("Unsupported EntityDetailsDto subtype: " + detailsDto.getClass());
        };
    }

    default RelevanceType mapRelevance(String relevance) {
        if (relevance == null) return null;
        return RelevanceType.fromValue(relevance);
    }

    default String mapName(EntityDetails details) {
        switch (details) {
            case null -> {
                return null;
            }
            case PersonDetails pd -> {
                String first = pd.getFirstName() != null ? pd.getFirstName() : "";
                String last = pd.getLastName() != null ? pd.getLastName() : "";
                return (first + " " + last).trim();
            }
            case CompanyDetails cd -> {
                return cd.getFullName();
            }
            case CourtDetails cd -> {
                return cd.getFullName();
            }
            case LawFirmDetails lf -> {
                return lf.getFullName();
            }
            default -> {
            }
        }
        return null;
    }
}
