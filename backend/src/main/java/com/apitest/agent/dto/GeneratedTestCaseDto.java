package com.apitest.agent.dto;

import java.util.Map;

public record GeneratedTestCaseDto(
    String name,
    String method,
    String path,
    String category,
    Map<String, Object> requestBody,
    Map<String, String> requestHeaders,
    Map<String, String> queryParams,
    Integer expectedStatus,
    String purpose,
    String description
) {}
