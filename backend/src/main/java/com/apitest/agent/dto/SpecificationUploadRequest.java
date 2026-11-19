package com.apitest.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecificationUploadRequest(
    String format,

    @NotBlank(message = "Content is required")
    String content,

    String filename
) {}
