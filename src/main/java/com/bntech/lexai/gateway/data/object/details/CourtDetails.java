package com.bntech.lexai.gateway.data.object.details;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class CourtDetails extends EntityDetails {
    private String fullName;
    private String jurisdiction;
    private String courtLevel;
    private String division;
    private String judgeName;
    private String roomNumber;
}
