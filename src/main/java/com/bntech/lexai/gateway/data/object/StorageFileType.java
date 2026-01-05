package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StorageFileType {
    PDF("pdf"),
    JPG("jpg"),
    JPEG("jpeg"),
    PNG("png"),
    GIF("gif"),
    TXT("txt"),
    DOC("doc"),
    DOCX("docx"),
    XLS("xls"),
    XLSX("xlsx"),
    OTHER("other");

    private final String value;

    StorageFileType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static StorageFileType fromValue(String value) {
        for (StorageFileType type : StorageFileType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return OTHER;
    }
}
