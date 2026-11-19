package com.apitest.agent.dto;

import java.time.LocalDateTime;

public record TestRunResponse(
    Long id,
    String status,
    Integer totalTests,
    Integer passedTests,
    Integer failedTests,
    Integer skippedTests,
    Integer contractViolations,
    Long executionTimeMs,
    String errorMessage,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    Long projectId,
    Long specificationId
) {}
