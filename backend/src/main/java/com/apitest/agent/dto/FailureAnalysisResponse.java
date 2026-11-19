package com.apitest.agent.dto;

import java.time.LocalDateTime;

public record FailureAnalysisResponse(
    Long id,
    String summary,
    String likelyCause,
    String severity,
    Boolean contractViolation,
    Boolean isExpectedFailure,
    String recommendation,
    LocalDateTime analyzedAt
) {}
