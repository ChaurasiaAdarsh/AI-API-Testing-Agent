package com.apitest.agent.dto;

import java.util.Map;

public record TestExecutionResult(
    int actualStatus,
    String actualResponseBody,
    Map<String, String> actualResponseHeaders,
    long executionTimeMs,
    boolean contractViolation,
    String contractViolationDetails
) {}
