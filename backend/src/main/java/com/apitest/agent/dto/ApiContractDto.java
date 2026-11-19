package com.apitest.agent.dto;

import java.util.List;

public record ApiContractDto(
    String title,
    String version,
    String baseUrl,
    List<EndpointDto> endpoints
) {}
