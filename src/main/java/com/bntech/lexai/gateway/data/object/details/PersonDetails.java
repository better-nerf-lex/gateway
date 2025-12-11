package com.bntech.lexai.gateway.data.object.details;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class PersonDetails extends EntityDetails {
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
