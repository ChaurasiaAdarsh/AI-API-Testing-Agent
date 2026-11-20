package com.apitest.agent.controller;

import com.apitest.agent.dto.*;
import com.apitest.agent.service.TestRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-runs")
@Tag(name = "Test Runs", description = "Test run management")
@Slf4j
public class TestRunController {

    private final TestRunService testRunService;

    public TestRunController(TestRunService testRunService) {
        this.testRunService = testRunService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test run by ID")
    public ResponseEntity<TestRunResponse> getTestRun(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.getTestRun(id));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute the test run")
    public ResponseEntity<TestRunResponse> executeTestRun(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.executeTestRun(id));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get test execution results")
    public ResponseEntity<List<TestExecutionResponse>> getResults(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.getTestRunResults(id));
    }

    @GetMapping("/{id}/violations")
    @Operation(summary = "Get contract violations")
    public ResponseEntity<List<TestExecutionResponse>> getViolations(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.getTestRunViolations(id));
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "Get AI failure analyses")
    public ResponseEntity<List<TestExecutionResponse>> getAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.getTestRunResults(id));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Get test run summary with AI insights")
    public ResponseEntity<TestRunSummaryResponse> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.getTestRunSummary(id));
    }

    @GetMapping("/{id}/test-cases")
    @Operation(summary = "Get generated test cases for a test run")
    public ResponseEntity<List<TestCaseResponse>> getTestCases(@PathVariable Long id) {
        return ResponseEntity.ok(testRunService.getTestRunTestCases(id));
    }
}
