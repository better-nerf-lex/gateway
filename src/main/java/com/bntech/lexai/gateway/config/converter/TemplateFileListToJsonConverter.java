package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.TemplateFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

import java.util.List;

@WritingConverter
@RequiredArgsConstructor
public class TemplateFileListToJsonConverter implements Converter<List<TemplateFile>, Json> {
    private final ObjectMapper objectMapper;

    @Override
    public Json convert(@NonNull List<TemplateFile> source) {
        try {
            return Json.of(objectMapper.writeValueAsString(source));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting List<TemplateFile> to JSON", e);
        }
    }
}
