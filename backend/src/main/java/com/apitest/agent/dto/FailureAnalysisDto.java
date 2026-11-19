package com.apitest.agent.dto;

public record FailureAnalysisDto(
    String summary,
    String likelyCause,
    String severity,
    boolean contractViolation,
    boolean isExpectedFailure,
    String recommendation
) {}
