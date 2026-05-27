package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.DocumentGenerationMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.io.IOException;

@ReadingConverter
@RequiredArgsConstructor
public class JsonToDocumentGenerationMetadataConverter implements Converter<Json, DocumentGenerationMetadata> {
    private final ObjectMapper objectMapper;

    @Override
    public DocumentGenerationMetadata convert(@NonNull Json source) {
        try {
            return objectMapper.readValue(source.asString(), DocumentGenerationMetadata.class);
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to DocumentGenerationMetadata", e);
        }
    }
}
