package com.apitest.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerateTestsRequest(
    String targetBaseUrl,

    @Min(value = 1, message = "Must generate at least 1 test")
    @Max(value = 100, message = "Cannot generate more than 100 tests")
    Integer maxTests
) {
    public int effectiveMaxTests() {
        return maxTests != null ? maxTests : 30;
    }
}
