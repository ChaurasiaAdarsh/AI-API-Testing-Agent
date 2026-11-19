package com.apitest.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    String name,

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    String description,

    @NotBlank(message = "Target base URL is required")
    String targetBaseUrl
) {}
