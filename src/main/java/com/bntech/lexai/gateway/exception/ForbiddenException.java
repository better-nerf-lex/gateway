package com.bntech.lexai.gateway.exception;

public class ForbiddenException extends RuntimeException {

    private final String resource;

    public ForbiddenException(String message) {
        super(message);
        this.resource = null;
    }

    public ForbiddenException(String resource, String action) {
        super(String.format("Access denied: cannot %s %s", action, resource));
        this.resource = resource;
    }

    public ForbiddenException() {
        super("Access denied");
        this.resource = null;
    }

    public String getResource() {
        return resource;
    }
}
