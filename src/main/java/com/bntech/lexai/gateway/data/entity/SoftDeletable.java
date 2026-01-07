package com.bntech.lexai.gateway.data.entity;

import java.time.LocalDateTime;

public interface SoftDeletable {
    LocalDateTime getDeletedAt();
    void setDeletedAt(LocalDateTime deletedAt);

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    default void softDelete() {
        setDeletedAt(LocalDateTime.now());
    }

    default void restore() {
        setDeletedAt(null);
    }
}
