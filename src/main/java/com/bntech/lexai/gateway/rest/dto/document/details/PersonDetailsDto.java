package com.bntech.lexai.gateway.rest.dto.document.details;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PersonDetailsDto extends EntityDetailsDto {
    private String firstName;
    private String middleName;
    private String lastName;
    private String title;
    private String gender;
    private String dateOfBirth;
    private String placeOfBirth;
    private String nationality;
    private String occupation;
    private String maritalStatus;
    private String nationalIdentityNumber;
    private String passportNumber;
    private String taxId;
}
