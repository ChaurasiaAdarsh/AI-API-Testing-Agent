package com.apitest.agent.exception;

public class SsrfViolationException extends ApiTestingException {
    public SsrfViolationException(String message) {
        super(message);
    }
}
