package com.bntech.lexai.gateway.exception;

public class ServiceException extends RuntimeException {

    private final String service;

    public ServiceException(String message) {
        super(message);
        this.service = null;
    }

    public ServiceException(String service, String message) {
        super(message);
        this.service = service;
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.service = null;
    }

    public ServiceException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
