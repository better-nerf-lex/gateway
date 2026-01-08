package com.bntech.lexai.gateway.rest.dto;

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
    private String status; // Keeping 'status' although not in Entity explicit list, might be mapped or
                           // just legacy. Plan didn't explicitly remove, but new fields are key.
    private Long userId;
    private String username; // Enriched in Mapper/Service
    private String teamId;
    private String entityId;
    private String entityType;
    private String details;
}
