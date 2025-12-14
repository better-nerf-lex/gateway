package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.rest.dto.document.details.EntityDetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalEntityDto {
    private String id;
    private String name;
    private String relevance;
    private EntityDetailsDto details;
}
