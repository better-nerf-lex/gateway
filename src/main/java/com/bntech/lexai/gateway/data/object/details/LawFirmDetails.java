package com.bntech.lexai.gateway.data.object.details;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class LawFirmDetails extends EntityDetails {
    private String fullName;
    private String shortName;
    private String legalStructure;
    private String website;
    private String representativeName;
    private String barAssociationNumber;
    private String taxId;
}
