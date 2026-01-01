package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EntityType {
    INDIVIDUAL("individual"),
    COMPANY("company"),
    COURT("court"),
    LAW_FIRM("law_firm");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EntityType fromValue(String value) {
        for (EntityType type : EntityType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EntityType value: " + value);
    }
}
