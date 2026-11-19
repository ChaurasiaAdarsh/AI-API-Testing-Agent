package com.apitest.agent.dto;

import java.time.LocalDateTime;

public record TestExecutionResponse(
    Long id,
    String testName,
    String method,
    String endpoint,
    String requestBody,
    String requestHeaders,
    Integer expectedStatus,
    Integer actualStatus,
    String actualResponseBody,
    String actualResponseHeaders,
    Long executionTimeMs,
    String status,
    Boolean contractViolation,
    String contractViolationDetails,
    LocalDateTime executedAt,
    Long testRunId,
    FailureAnalysisResponse failureAnalysis
) {}
