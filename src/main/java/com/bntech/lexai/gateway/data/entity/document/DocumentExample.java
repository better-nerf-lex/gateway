package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "document_examples")
public class DocumentExample {
    @Id
    private UUID id;
    private String title;
    private String description;
    @Column("questions")
    private List<QuestionDto> questions;
    private List<String> base;
    private List<String> requirements;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
