package com.apitest.agent.ai;

import com.apitest.agent.dto.*;
import com.apitest.agent.model.GeneratedTestCase;
import java.util.List;

public interface AiEngine {
    List<GeneratedTestCaseDto> generateTestCases(ApiContractDto contract, int maxTests);
    FailureAnalysisDto analyzeFailure(GeneratedTestCase testCase, TestExecutionResult result, ApiContractDto contract);
    String generateTestRunSummary(TestRunSummaryData summaryData);
}
