package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "document_examples")
public class DocumentExampleEs {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Object)
    private List<QuestionDto> questions;

    @Field(type = FieldType.Keyword)
    private List<String> base;

    @Field(type = FieldType.Keyword)
    private List<String> requirements;

    @Field(type = FieldType.Object)
    private List<DocumentExample.TemplateFile> templateFiles;
}
