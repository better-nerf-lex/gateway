package com.bntech.lexai.gateway.data.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileServiceUploadResponse {
    private String key;
    private String s3Url;
}
