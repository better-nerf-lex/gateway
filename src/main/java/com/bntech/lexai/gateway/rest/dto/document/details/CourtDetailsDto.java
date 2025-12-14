package com.bntech.lexai.gateway.rest.dto.document.details;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CourtDetailsDto extends EntityDetailsDto {
    private String fullName;
    private String jurisdiction;
    private String courtLevel;
    private String division;
    private String judgeName;
    private String roomNumber;
}
