package com.bntech.lexai.gateway.data.object.details;

import com.bntech.lexai.gateway.data.object.EntityType;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "entityType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PersonDetails.class, name = "INDIVIDUAL"),
        @JsonSubTypes.Type(value = CompanyDetails.class, name = "COMPANY"),
        @JsonSubTypes.Type(value = CourtDetails.class, name = "COURT"),
        @JsonSubTypes.Type(value = LawFirmDetails.class, name = "LAW_FIRM")
})
public abstract class EntityDetails implements Serializable {

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

    @Builder.Default
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void addProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
}
