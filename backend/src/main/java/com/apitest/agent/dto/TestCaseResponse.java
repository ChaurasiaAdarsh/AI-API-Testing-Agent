package com.apitest.agent.dto;

public record TestCaseResponse(
    Long id,
    String name,
    String method,
    String path,
    String category,
    String requestBody,
    String requestHeaders,
    String queryParams,
    Integer expectedStatus,
    String purpose,
    String description,
    Long testRunId
) {}
