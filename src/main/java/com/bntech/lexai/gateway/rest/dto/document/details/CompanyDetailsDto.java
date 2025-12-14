package com.bntech.lexai.gateway.rest.dto.document.details;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CompanyDetailsDto extends EntityDetailsDto {
    private String fullName;
    private String shortName;
    private String legalStructure;
    private String dateOfIncorporation;
    private String industry;
    private String website;
    private String representativeName;
    private String registrationNumber;
    private String taxId;
}
