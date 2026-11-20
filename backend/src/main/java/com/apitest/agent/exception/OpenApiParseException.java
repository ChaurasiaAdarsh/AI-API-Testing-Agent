package com.apitest.agent.exception;

public class OpenApiParseException extends ApiTestingException {
    public OpenApiParseException(String message) {
        super(message);
    }
    public OpenApiParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
