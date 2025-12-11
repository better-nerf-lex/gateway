package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
@RequiredArgsConstructor
public class EntityDetailsToJsonConverter implements Converter<EntityDetails, Json> {
    private final ObjectMapper objectMapper;

    @Override
    public Json convert(EntityDetails source) {
        try {
            return Json.of(objectMapper.writeValueAsString(source));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting EntityDetails to JSON", e);
        }
    }
}
