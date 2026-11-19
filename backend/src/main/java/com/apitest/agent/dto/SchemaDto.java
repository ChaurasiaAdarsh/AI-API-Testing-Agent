package com.apitest.agent.dto;

import java.util.List;
import java.util.Map;

public record SchemaDto(
    String type,
    String format,
    Object minimum,
    Object maximum,
    Integer minLength,
    Integer maxLength,
    String pattern,
    List<String> enumValues,
    List<String> required,
    Map<String, SchemaDto> properties,
    SchemaDto items,
    boolean nullable
) {}
