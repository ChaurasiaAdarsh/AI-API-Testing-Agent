package com.apitest.agent.dto;

import java.util.List;

public record TestRunSummaryResponse(
    TestRunResponse testRun,
    double passRate,
    double contractViolationRate,
    String aiSummary,
    List<String> topFailures,
    boolean analysisReady
) {}
