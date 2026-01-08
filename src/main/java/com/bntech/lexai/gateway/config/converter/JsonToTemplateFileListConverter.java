package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.List;

@ReadingConverter
@RequiredArgsConstructor
public class JsonToTemplateFileListConverter implements Converter<Json, List<DocumentExample.TemplateFile>> {
    private final ObjectMapper objectMapper;

    @Override
    public List<DocumentExample.TemplateFile> convert(@NonNull Json source) {
        try {
            return objectMapper.readValue(source.asString(), new TypeReference<List<DocumentExample.TemplateFile>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to List<DocumentExample.TemplateFile>", e);
        }
    }
}
