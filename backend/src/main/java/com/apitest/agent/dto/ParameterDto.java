package com.apitest.agent.dto;

public record ParameterDto(
    String name,
    String in,
    boolean required,
    SchemaDto schema
) {}
