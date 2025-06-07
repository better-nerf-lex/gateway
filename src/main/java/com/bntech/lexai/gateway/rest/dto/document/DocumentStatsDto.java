package com.bntech.lexai.gateway.rest.dto.document;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatsDto {
    private Integer totalDocuments;
    private Integer thisMonth;
    private Integer pending;
    private Integer completed;
}
