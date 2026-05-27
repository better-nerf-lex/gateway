package com.bntech.lexai.gateway.data.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredAttachment {
    private String id;
    private String description;
    private String documentId;
    private String documentGroupId;
}
