package com.apitest.agent.dto;

import java.util.List;

public record TestRunSummaryData(
        String projectName,
        String apiTitle,
        int totalTests,
        int passed,
        int failed,
        int contractViolations,
        List<String> topFailureReasons
) {}
