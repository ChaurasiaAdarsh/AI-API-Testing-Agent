package com.apitest.agent.dto;

public record ResponseDto(
    String description,
    SchemaDto schema,
    String contentType
) {}
