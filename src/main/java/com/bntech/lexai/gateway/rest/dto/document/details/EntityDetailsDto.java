package com.bntech.lexai.gateway.rest.dto.document.details;

import com.bntech.lexai.gateway.data.object.EntityType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "entityType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PersonDetailsDto.class, names = {"INDIVIDUAL", "individual"}),
        @JsonSubTypes.Type(value = CompanyDetailsDto.class, names = {"COMPANY", "company"}),
        @JsonSubTypes.Type(value = CourtDetailsDto.class, names = {"COURT", "court"}),
        @JsonSubTypes.Type(value = LawFirmDetailsDto.class, names = {"LAW_FIRM", "law_firm"})
})
public abstract class EntityDetailsDto implements Serializable {

    private EntityType entityType;

    private String comments;
    private String relevanceScore;

    private String streetAddress;
    private String city;
    private String stateOrProvince;
    private String postalCode;
    private String country;

    private String email;
    private String phoneNumber;
}
