package com.bntech.lexai.gateway.config.converter;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConverterTest {

    private ObjectMapper objectMapper;
    private JsonToTemplateFileListConverter readingConverter;
    private TemplateFileListToJsonConverter writingConverter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        readingConverter = new JsonToTemplateFileListConverter(objectMapper);
        writingConverter = new TemplateFileListToJsonConverter(objectMapper);
    }

    @Test
    void shouldConvertTemplateFileListToJsonAndBack() {
        List<DocumentExample.TemplateFile> sourceList = List.of(
                DocumentExample.TemplateFile.builder()
                        .id("t1")
                        .name("test1.pdf")
                        .s3Key("key1")
                        .build(),
                DocumentExample.TemplateFile.builder()
                        .id("t2")
                        .name("test2.pdf")
                        .s3Key("key2")
                        .build()
        );

        Json json = writingConverter.convert(sourceList);
        assertNotNull(json);

        List<DocumentExample.TemplateFile> resultList = readingConverter.convert(json);
        assertNotNull(resultList);
        assertEquals(2, resultList.size());
        assertEquals("t1", resultList.get(0).getId());
        assertEquals("test1.pdf", resultList.get(0).getName());
        assertEquals("key1", resultList.get(0).getS3Key());
        assertEquals("t2", resultList.get(1).getId());
    }
}
