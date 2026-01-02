package com.bntech.lexai.gateway.config.converter;

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
public class JsonToStringListConverter implements Converter<Json, List<String>> {
    private final ObjectMapper objectMapper;

    @Override
    public List<String> convert(@NonNull Json source) {
        try {
            return objectMapper.readValue(source.asString(), new TypeReference<List<String>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to List<String>", e);
        }
    }
}
