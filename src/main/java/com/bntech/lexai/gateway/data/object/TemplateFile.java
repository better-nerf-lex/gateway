package com.bntech.lexai.gateway.data.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateFile {
    private String id;
    private String name;
    private String s3Key;
}
