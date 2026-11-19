package com.apitest.agent.dto;

import java.time.LocalDateTime;

public record SpecificationResponse(
    Long id,
    String title,
    String version,
    String format,
    LocalDateTime uploadedAt,
    Long projectId
) {}
