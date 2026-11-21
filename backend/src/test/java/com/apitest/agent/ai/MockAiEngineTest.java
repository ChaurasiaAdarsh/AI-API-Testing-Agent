package com.apitest.agent.ai;

import com.apitest.agent.dto.*;
import com.apitest.agent.model.GeneratedTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class MockAiEngineTest {

    private MockAiEngine mockAiEngine;

    @BeforeEach
    void setUp() {
        mockAiEngine = new MockAiEngine();
    }

    @Test
    void shouldGenerateTestCasesForEndpoints() {
        ApiContractDto contract = createContractWithBody();
        List<GeneratedTestCaseDto> testCases = mockAiEngine.generateTestCases(contract, 50);
        assertThat(testCases).isNotEmpty();
    }

    @Test
    void shouldGeneratePositiveTest() {
        ApiContractDto contract = createSimpleContract();
        List<GeneratedTestCaseDto> testCases = mockAiEngine.generateTestCases(contract, 50);
        assertThat(testCases).anyMatch(tc -> "POSITIVE".equals(tc.category()));
    }

    @Test
    void shouldGenerateNegativeTestForRequiredBody() {
        ApiContractDto contract = createContractWithBody();
        List<GeneratedTestCaseDto> testCases = mockAiEngine.generateTestCases(contract, 50);
        assertThat(testCases).anyMatch(tc -> "NEGATIVE".equals(tc.category()));
    }

    @Test
    void shouldGenerateBoundaryTestForIntegerMin() {
        ApiContractDto contract = createContractWithIntegerMin();
        List<GeneratedTestCaseDto> testCases = mockAiEngine.generateTestCases(contract, 50);
        assertThat(testCases).anyMatch(tc -> "BOUNDARY".equals(tc.category()));
    }

    @Test
    void shouldRespectMaxTestsLimit() {
        ApiContractDto contract = createContractWithBody();
        List<GeneratedTestCaseDto> testCases = mockAiEngine.generateTestCases(contract, 1);
        assertThat(testCases).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void shouldReturnEmptyListForEmptyContract() {
        ApiContractDto contract = new ApiContractDto("Empty", "1.0", "http://localhost", List.of());
        List<GeneratedTestCaseDto> testCases = mockAiEngine.generateTestCases(contract, 50);
        assertThat(testCases).isEmpty();
    }

    @Test
    void shouldAnalyzeExpectedNegativeFailure() {
        GeneratedTestCase tc = new GeneratedTestCase();
        tc.setName("Missing required fields");
        tc.setCategory("NEGATIVE");
        tc.setExpectedStatus(400);

        TestExecutionResult result = new TestExecutionResult(400, "{\"error\":\"validation\"}", Map.of(), 100L, false, null);
        ApiContractDto contract = createSimpleContract();

        FailureAnalysisDto analysis = mockAiEngine.analyzeFailure(tc, result, contract);

        assertThat(analysis.isExpectedFailure()).isTrue();
        assertThat(analysis.severity()).isEqualTo("LOW");
        assertThat(analysis.contractViolation()).isFalse();
    }

    @Test
    void shouldAnalyzeUnexpectedPositiveFailure() {
        GeneratedTestCase tc = new GeneratedTestCase();
        tc.setName("Valid user creation");
        tc.setCategory("POSITIVE");
        tc.setExpectedStatus(201);

        TestExecutionResult result = new TestExecutionResult(500, "{\"error\":\"internal\"}", Map.of(), 150L, false, null);
        ApiContractDto contract = createSimpleContract();

        FailureAnalysisDto analysis = mockAiEngine.analyzeFailure(tc, result, contract);

        assertThat(analysis.isExpectedFailure()).isFalse();
        assertThat(analysis.severity()).isEqualTo("MEDIUM");
    }

    @Test
    void shouldGenerateTestRunSummary() {
        TestRunSummaryData data = new TestRunSummaryData(
                "Test Project", "User API", 10, 8, 2, 1,
                List.of("POST /users: status 200 vs 201")
        );
        String summary = mockAiEngine.generateTestRunSummary(data);
        assertThat(summary).isNotNull();
        assertThat(summary).contains("User API");
    }

    private ApiContractDto createSimpleContract() {
        EndpointDto endpoint = new EndpointDto(
                "GET", "/users/1", "Get user", List.of(), null,
                Map.of("200", new ResponseDto("User found", null, "application/json")),
                List.of()
        );
        return new ApiContractDto("Test API", "1.0", "http://localhost", List.of(endpoint));
    }

    private ApiContractDto createContractWithBody() {
        SchemaDto nameSchema = new SchemaDto("string", null, null, null, 2, 100, null, null, null, null, null, false);
        SchemaDto schema = new SchemaDto("object", null, null, null, null, null, null, null,
                List.of("name"),
                Map.of("name", nameSchema),
                null, false);
        RequestBodyDto body = new RequestBodyDto(true, schema, "application/json");
        EndpointDto endpoint = new EndpointDto(
                "POST", "/users", "Create user", List.of(), body,
                Map.of("201", new ResponseDto("Created", null, "application/json")),
                List.of()
        );
        return new ApiContractDto("Test API", "1.0", "http://localhost", List.of(endpoint));
    }

    private ApiContractDto createContractWithIntegerMin() {
        SchemaDto ageSchema = new SchemaDto("integer", null, 18.0, 120.0, null, null, null, null, null, null, null, false);
        SchemaDto schema = new SchemaDto("object", null, null, null, null, null, null, null,
                List.of("age"),
                Map.of("age", ageSchema),
                null, false);
        RequestBodyDto body = new RequestBodyDto(true, schema, "application/json");
        EndpointDto endpoint = new EndpointDto(
                "POST", "/users", "Create user", List.of(), body,
                Map.of("201", new ResponseDto("Created", null, "application/json")),
                List.of()
        );
        return new ApiContractDto("Test API", "1.0", "http://localhost", List.of(endpoint));
    }
}
