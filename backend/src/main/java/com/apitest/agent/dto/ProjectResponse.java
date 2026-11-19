package com.apitest.agent.dto;

import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    String targetBaseUrl,
    int specificationCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
