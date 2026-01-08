package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
    DOCUMENT_NEW("document_new"),
    DOCUMENT_DELETE("document_delete"),
    DOCUMENT_UPDATE("document_update"),
    DOCUMENT_RESTORE("document_restore"),
    ENTITY_NEW("entity_new"),
    ENTITY_DELETE("entity_delete"),
    ENTITY_UPDATE("entity_update"),
    ENTITY_RESTORE("entity_restore"),
    CASE_NEW("case_new"),
    CASE_DELETE("case_delete"),
    CASE_UPDATE("case_update"),
    CASE_RESTORE("case_restore");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ActionType fromValue(String value) {
        for (ActionType type : ActionType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ActionType value: " + value);
    }
}
