package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.io.IOException;

@ReadingConverter
@RequiredArgsConstructor
public class JsonToEntityDetailsConverter implements Converter<Json, EntityDetails> {
    private final ObjectMapper objectMapper;

    @Override
    public EntityDetails convert(Json source) {
        try {
            return objectMapper.readValue(source.asString(), EntityDetails.class);
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to EntityDetails", e);
        }
    }
}
