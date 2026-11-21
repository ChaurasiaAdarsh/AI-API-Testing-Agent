package com.apitest.agent.ai;

import com.apitest.agent.dto.*;
import com.apitest.agent.model.GeneratedTestCase;

import java.util.*;

public class MockAiEngine implements AiEngine {

    @Override
    public List<GeneratedTestCaseDto> generateTestCases(ApiContractDto contract, int maxTests) {
        List<GeneratedTestCaseDto> testCases = new ArrayList<>();

        if (contract.endpoints() == null) return testCases;

        for (EndpointDto endpoint : contract.endpoints()) {
            testCases.add(new GeneratedTestCaseDto(
                    "Valid " + endpoint.method() + " request to " + endpoint.path(),
                    endpoint.method(),
                    resolvePathParams(endpoint.path()),
                    "POSITIVE",
                    buildValidBody(endpoint),
                    Map.of("Content-Type", "application/json"),
                    Map.of(),
                    getSuccessStatus(endpoint),
                    "Verify successful " + endpoint.method() + " to " + endpoint.path(),
                    "Happy path test for " + endpoint.path()
            ));

            if (endpoint.requestBody() != null && endpoint.requestBody().required()) {
                testCases.add(new GeneratedTestCaseDto(
                        "Missing required fields for " + endpoint.method() + " " + endpoint.path(),
                        endpoint.method(),
                        resolvePathParams(endpoint.path()),
                        "NEGATIVE",
                        Map.of(),
                        Map.of("Content-Type", "application/json"),
                        Map.of(),
                        400,
                        "Verify rejection when required fields are missing",
                        "Tests that the API rejects empty request body with 400"
                ));
            }

            if (endpoint.requestBody() != null && endpoint.requestBody().schema() != null
                    && endpoint.requestBody().schema().properties() != null) {
                endpoint.requestBody().schema().properties().forEach((fieldName, fieldSchema) -> {
                    if ("integer".equals(fieldSchema.type()) && fieldSchema.minimum() != null) {
                        Map<String, Object> body = new HashMap<>(buildValidBody(endpoint));
                        double min = ((Number) fieldSchema.minimum()).doubleValue();
                        body.put(fieldName, (int) min - 1);
                        testCases.add(new GeneratedTestCaseDto(
                                "Boundary: " + fieldName + " below minimum (" + ((int)min - 1) + ")",
                                endpoint.method(),
                                resolvePathParams(endpoint.path()),
                                "BOUNDARY",
                                body,
                                Map.of("Content-Type", "application/json"),
                                Map.of(),
                                400,
                                "Verify " + fieldName + " minimum boundary enforcement",
                                "Value " + ((int)min - 1) + " should be rejected as it is below minimum " + (int)min
                        ));
                    }
                });
            }

            if (testCases.size() >= maxTests) break;
        }

        return testCases.subList(0, Math.min(testCases.size(), maxTests));
    }

    @Override
    public FailureAnalysisDto analyzeFailure(GeneratedTestCase testCase, TestExecutionResult result, ApiContractDto contract) {
        boolean isExpected = "NEGATIVE".equals(testCase.getCategory())
                && result.actualStatus() >= 400
                && result.actualStatus() < 500;
        boolean isBoundaryExpected = "BOUNDARY".equals(testCase.getCategory())
                && result.actualStatus() == 400;
        boolean isExpectedFailure = isExpected || isBoundaryExpected;

        return new FailureAnalysisDto(
                "Mock analysis: " + testCase.getName() + " resulted in HTTP " + result.actualStatus(),
                isExpectedFailure
                        ? "API correctly rejected invalid input as expected"
                        : "API returned unexpected response - possible defect",
                isExpectedFailure ? "LOW" : "MEDIUM",
                result.contractViolation(),
                isExpectedFailure,
                isExpectedFailure
                        ? "No action required - this is expected behavior"
                        : "Review API implementation against the OpenAPI contract"
        );
    }

    @Override
    public String generateTestRunSummary(TestRunSummaryData summaryData) {
        int passPercent = summaryData.totalTests() > 0
                ? (int) (summaryData.passed() * 100.0 / summaryData.totalTests()) : 0;
        return String.format(
                "Mock test summary for %s: %d/%d tests passed (%d%%) with %d contract violations.",
                summaryData.apiTitle(),
                summaryData.passed(),
                summaryData.totalTests(),
                passPercent,
                summaryData.contractViolations()
        );
    }

    private String resolvePathParams(String path) {
        return path.replaceAll("\\{[^}]+\\}", "1");
    }

    private Map<String, Object> buildValidBody(EndpointDto endpoint) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (endpoint.requestBody() == null || endpoint.requestBody().schema() == null) return body;

        SchemaDto schema = endpoint.requestBody().schema();
        if (schema.properties() != null) {
            schema.properties().forEach((name, propSchema) -> {
                body.put(name, generateSampleValue(name, propSchema));
            });
        }
        return body;
    }

    private Object generateSampleValue(String fieldName, SchemaDto schema) {
        if (schema.enumValues() != null && !schema.enumValues().isEmpty()) {
            return schema.enumValues().get(0);
        }
        return switch (schema.type() != null ? schema.type() : "string") {
            case "integer", "number" -> {
                if (schema.minimum() != null) {
                    yield ((Number) schema.minimum()).intValue();
                }
                yield 1;
            }
            case "boolean" -> true;
            case "array" -> List.of();
            default -> fieldName.contains("email") ? "test@example.com" : "test-" + fieldName;
        };
    }

    private Integer getSuccessStatus(EndpointDto endpoint) {
        if (endpoint.responses() == null) {
            return "POST".equals(endpoint.method()) ? 201 : 200;
        }
        if (endpoint.responses().containsKey("201")) return 201;
        if (endpoint.responses().containsKey("200")) return 200;
        if (endpoint.responses().containsKey("204")) return 204;
        return "POST".equals(endpoint.method()) ? 201 : 200;
    }
}
