package com.apitest.agent.dto;

public record RequestBodyDto(
    boolean required,
    SchemaDto schema,
    String contentType
) {}
