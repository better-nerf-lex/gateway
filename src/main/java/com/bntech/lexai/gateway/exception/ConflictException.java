package com.bntech.lexai.gateway.exception;

public class ConflictException extends RuntimeException {

    private final String resourceType;
    private final String conflictField;

    public ConflictException(String message) {
        super(message);
        this.resourceType = null;
        this.conflictField = null;
    }

    public ConflictException(String resourceType, String conflictField, String value) {
        super(String.format("%s with %s '%s' already exists", resourceType, conflictField, value));
        this.resourceType = resourceType;
        this.conflictField = conflictField;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getConflictField() {
        return conflictField;
    }
}
