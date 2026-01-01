package com.bntech.lexai.gateway.data.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity<T> implements Persistable<T> {
    @Id
    private T id;

    @Column()
    private LocalDateTime createdAt;

    @Override
    public abstract boolean isNew();
}
