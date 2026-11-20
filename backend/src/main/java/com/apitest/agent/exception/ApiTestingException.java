package com.apitest.agent.exception;

public class ApiTestingException extends RuntimeException {
    public ApiTestingException(String message) {
        super(message);
    }
    public ApiTestingException(String message, Throwable cause) {
        super(message, cause);
    }
}
