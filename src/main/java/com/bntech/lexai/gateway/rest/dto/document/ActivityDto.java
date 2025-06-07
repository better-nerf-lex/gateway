package com.bntech.lexai.gateway.rest.dto.document;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDto {
    private String id;
    private String action;
    private LocalDateTime date;
    private String status;
}
