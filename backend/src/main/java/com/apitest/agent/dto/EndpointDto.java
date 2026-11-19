package com.apitest.agent.dto;

import java.util.List;
import java.util.Map;

public record EndpointDto(
    String method,
    String path,
    String summary,
    List<ParameterDto> parameters,
    RequestBodyDto requestBody,
    Map<String, ResponseDto> responses,
    List<String> securityRequirements
) {}
