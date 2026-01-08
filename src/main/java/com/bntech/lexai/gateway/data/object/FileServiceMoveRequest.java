package com.bntech.lexai.gateway.data.object;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileServiceMoveRequest {
    private String sourceKey;
    private String destinationKey;
}
