package com.apitest.agent.service;

import com.apitest.agent.ai.AiEngine;
import com.apitest.agent.config.AppProperties;
import com.apitest.agent.config.SsrfProtectionService;
import com.apitest.agent.dto.*;
import com.apitest.agent.exception.*;
import com.apitest.agent.executor.TestExecutionEngine;
import com.apitest.agent.model.*;
import com.apitest.agent.parser.OpenApiParserService;
import com.apitest.agent.repository.*;
import com.apitest.agent.util.JsonUtil;
import com.apitest.agent.validator.ContractValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final GeneratedTestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final FailureAnalysisRepository failureAnalysisRepository;
    private final ApiSpecificationRepository specificationRepository;
    private final ProjectService projectService;
    private final OpenApiParserService parserService;
    private final AiEngine aiEngine;
    private final TestExecutionEngine executionEngine;
    private final ContractValidator contractValidator;
    private final SsrfProtectionService ssrfProtectionService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final Executor testExecutionExecutor;

    public TestRunService(TestRunRepository testRunRepository,
                          GeneratedTestCaseRepository testCaseRepository,
                          TestExecutionRepository testExecutionRepository,
                          FailureAnalysisRepository failureAnalysisRepository,
                          ApiSpecificationRepository specificationRepository,
                          ProjectService projectService,
                          OpenApiParserService parserService,
                          AiEngine aiEngine,
                          TestExecutionEngine executionEngine,
                          ContractValidator contractValidator,
                          SsrfProtectionService ssrfProtectionService,
                          AppProperties appProperties,
                          ObjectMapper objectMapper,
                          @Qualifier("testExecutionExecutor") Executor testExecutionExecutor) {
        this.testRunRepository = testRunRepository;
        this.testCaseRepository = testCaseRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.failureAnalysisRepository = failureAnalysisRepository;
        this.specificationRepository = specificationRepository;
        this.projectService = projectService;
        this.parserService = parserService;
        this.aiEngine = aiEngine;
        this.executionEngine = executionEngine;
        this.contractValidator = contractValidator;
        this.ssrfProtectionService = ssrfProtectionService;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.testExecutionExecutor = testExecutionExecutor;
    }

    @Transactional
    public TestRunResponse generateTests(Long projectId, GenerateTestsRequest request) {
        Project project = projectService.findProject(projectId);

        ApiSpecification spec = specificationRepository.findTopByProjectIdOrderByUploadedAtDesc(projectId)
                .orElseThrow(() -> new InvalidSpecificationException("No specification found for project " + projectId));

        String targetUrl = request.targetBaseUrl() != null && !request.targetBaseUrl().isBlank()
                ? request.targetBaseUrl() : project.getTargetBaseUrl();
        ssrfProtectionService.validateTargetUrl(targetUrl);

        TestRun testRun = new TestRun();
        testRun.setProject(project);
        testRun.setSpecification(spec);
        testRun.setStatus(TestRunStatus.CREATING);
        testRun = testRunRepository.save(testRun);

        final Long testRunId = testRun.getId();
        final int maxTests = Math.min(request.effectiveMaxTests(), appProperties.getTesting().getMaxGeneratedTests());

        CompletableFuture.runAsync(() -> doGenerateTests(testRunId, spec, maxTests), testExecutionExecutor);

        return toResponse(testRun);
    }

    @Transactional
    public TestRunResponse executeTestRun(Long testRunId) {
        TestRun testRun = findTestRun(testRunId);

        if (testRun.getStatus() != TestRunStatus.READY) {
            throw new ApiTestingException("Test run " + testRunId + " is not in READY state. Current state: " + testRun.getStatus());
        }

        String targetUrl = testRun.getProject().getTargetBaseUrl();
        String rawContent = testRun.getSpecification().getRawContent();
        String projectName = testRun.getProject().getName();
        ssrfProtectionService.validateTargetUrl(targetUrl);

        testRun.setStatus(TestRunStatus.RUNNING);
        testRun.setStartedAt(LocalDateTime.now());
        testRunRepository.save(testRun);

        CompletableFuture.runAsync(() -> doExecuteTests(testRunId, targetUrl, rawContent, projectName), testExecutionExecutor);

        return toResponse(testRun);
    }

    private void doGenerateTests(Long testRunId, ApiSpecification spec, int maxTests) {
        try {
            updateTestRunStatus(testRunId, TestRunStatus.GENERATING_TESTS);

            log.info("AI test generation started for testRun={}, maxTests={}", testRunId, maxTests);
            long startTime = System.currentTimeMillis();

            ApiContractDto contract = parserService.parse(spec.getRawContent());
            List<GeneratedTestCaseDto> generatedCases = aiEngine.generateTestCases(contract, maxTests);

            log.info("AI generated {} test cases in {}ms for testRun={}",
                    generatedCases.size(), System.currentTimeMillis() - startTime, testRunId);

            saveGeneratedTestCases(testRunId, generatedCases);
            updateTestRunStatus(testRunId, TestRunStatus.READY);

        } catch (Exception e) {
            log.error("Test generation failed for testRun={}: {}", testRunId, e.getMessage(), e);
            updateTestRunError(testRunId, "Test generation failed: " + e.getMessage());
        }
    }

    @Transactional
    public void saveGeneratedTestCases(Long testRunId, List<GeneratedTestCaseDto> cases) {
        TestRun testRun = findTestRun(testRunId);

        for (GeneratedTestCaseDto dto : cases) {
            GeneratedTestCase tc = new GeneratedTestCase();
            tc.setName(dto.name());
            tc.setMethod(dto.method().toUpperCase());
            tc.setPath(dto.path());
            tc.setCategory(dto.category());
            tc.setExpectedStatus(dto.expectedStatus());
            tc.setPurpose(dto.purpose());
            tc.setDescription(dto.description());
            tc.setTestRun(testRun);

            if (dto.requestBody() != null && !dto.requestBody().isEmpty()) {
                try { tc.setRequestBody(objectMapper.writeValueAsString(dto.requestBody())); }
                catch (Exception e) { log.debug("Could not serialize request body: {}", e.getMessage()); }
            }
            if (dto.requestHeaders() != null && !dto.requestHeaders().isEmpty()) {
                try { tc.setRequestHeaders(objectMapper.writeValueAsString(dto.requestHeaders())); }
                catch (Exception e) { log.debug("Could not serialize headers: {}", e.getMessage()); }
            }
            if (dto.queryParams() != null && !dto.queryParams().isEmpty()) {
                try { tc.setQueryParams(objectMapper.writeValueAsString(dto.queryParams())); }
                catch (Exception e) { log.debug("Could not serialize query params: {}", e.getMessage()); }
            }

            testCaseRepository.save(tc);
        }
    }

    private void doExecuteTests(Long testRunId, String targetUrl, String rawContent, String projectName) {
        try {
            List<GeneratedTestCase> testCases = testCaseRepository.findByTestRunId(testRunId);
            ApiContractDto contract = parserService.parse(rawContent);

            log.info("Test execution started: testRun={}, tests={}, target={}", testRunId, testCases.size(), targetUrl);

            int passed = 0;
            int failed = 0;
            int violations = 0;
            List<String> failureReasons = new ArrayList<>();

            for (GeneratedTestCase tc : testCases) {
                TestExecution exec = new TestExecution();
                exec.setTestRun(findTestRun(testRunId));
                exec.setTestCase(tc);
                exec.setTestName(tc.getName());
                exec.setMethod(tc.getMethod());
                exec.setEndpoint(tc.getPath());
                exec.setRequestBody(tc.getRequestBody());
                exec.setRequestHeaders(tc.getRequestHeaders());
                exec.setExpectedStatus(tc.getExpectedStatus());
                exec.setStatus(TestExecutionStatus.PENDING);
                exec = testExecutionRepository.save(exec);

                TestExecutionResult result = executionEngine.executeTest(tc, targetUrl);

                List<String> contractViolations = new ArrayList<>();

                if (result.actualStatus() == -1) {
                    exec.setStatus(TestExecutionStatus.ERROR);
                    exec.setActualResponseBody(result.actualResponseBody());
                    exec.setExecutionTimeMs(result.executionTimeMs());
                    exec.setExecutedAt(LocalDateTime.now());
                    failed++;
                } else {
                    contractValidator.compareStatusCode(tc.getExpectedStatus(), result.actualStatus())
                            .ifPresent(contractViolations::add);

                    String expectedContentType = getExpectedContentType(contract, tc.getMethod(), tc.getPath(), tc.getExpectedStatus());
                    String actualContentType = result.actualResponseHeaders() != null ?
                            result.actualResponseHeaders().get("content-type") : null;
                    contractValidator.validateContentType(expectedContentType, actualContentType)
                            .ifPresent(contractViolations::add);

                    if (result.actualResponseBody() != null && !result.actualResponseBody().isBlank()) {
                        SchemaDto expectedSchema = getExpectedResponseSchema(contract, tc.getMethod(), tc.getPath(), tc.getExpectedStatus());
                        if (expectedSchema != null) {
                            contractViolations.addAll(contractValidator.validateResponseSchema(result.actualResponseBody(), expectedSchema));
                        }
                    }

                    boolean isContractViolation = !contractViolations.isEmpty();
                    boolean isPassed = result.actualStatus() == tc.getExpectedStatus() && !isContractViolation;

                    exec.setActualStatus(result.actualStatus());
                    exec.setActualResponseBody(result.actualResponseBody());
                    exec.setActualResponseHeaders(JsonUtil.toJsonSafe(result.actualResponseHeaders()));
                    exec.setExecutionTimeMs(result.executionTimeMs());
                    exec.setContractViolation(isContractViolation);
                    exec.setContractViolationDetails(contractValidator.generateViolationSummary(contractViolations));
                    exec.setStatus(isPassed ? TestExecutionStatus.PASSED : TestExecutionStatus.FAILED);
                    exec.setExecutedAt(LocalDateTime.now());

                    if (isPassed) {
                        passed++;
                    } else {
                        failed++;
                        if (isContractViolation) violations++;
                        failureReasons.add(tc.getName() + ": " + (contractViolations.isEmpty() ?
                                "Status " + result.actualStatus() + " != " + tc.getExpectedStatus() :
                                contractViolations.get(0)));
                    }
                }

                testExecutionRepository.save(exec);
            }

            int finalPassed = passed;
            int finalFailed = failed;
            int finalViolations = violations;
            List<String> finalFailureReasons = failureReasons;

            doAiAnalysis(testRunId, contract, finalFailureReasons);

            updateTestRunCompleted(testRunId, testCases.size(), finalPassed, finalFailed, finalViolations);
            log.info("Test execution completed: testRun={}, passed={}, failed={}, violations={}",
                    testRunId, finalPassed, finalFailed, finalViolations);

        } catch (Exception e) {
            log.error("Test execution failed for testRun={}: {}", testRunId, e.getMessage(), e);
            updateTestRunError(testRunId, "Test execution failed: " + e.getMessage());
        }
    }

    private void doAiAnalysis(Long testRunId, ApiContractDto contract, List<String> failureReasons) {
        List<TestExecution> failedExecs = testExecutionRepository
                .findByTestRunIdAndStatusNot(testRunId, TestExecutionStatus.PASSED);

        for (TestExecution exec : failedExecs) {
            if (exec.getStatus() == TestExecutionStatus.ERROR) continue;
            if (exec.getTestCase() == null) continue;

            try {
                log.debug("AI analysis started for execution {}", exec.getId());

                TestExecutionResult result = new TestExecutionResult(
                        exec.getActualStatus() != null ? exec.getActualStatus() : -1,
                        exec.getActualResponseBody(),
                        Map.of(),
                        exec.getExecutionTimeMs() != null ? exec.getExecutionTimeMs() : 0,
                        Boolean.TRUE.equals(exec.getContractViolation()),
                        exec.getContractViolationDetails()
                );

                FailureAnalysisDto analysisDto = aiEngine.analyzeFailure(exec.getTestCase(), result, contract);

                FailureAnalysis analysis = new FailureAnalysis();
                analysis.setSummary(analysisDto.summary());
                analysis.setLikelyCause(analysisDto.likelyCause());
                analysis.setSeverity(analysisDto.severity());
                analysis.setContractViolation(analysisDto.contractViolation());
                analysis.setIsExpectedFailure(analysisDto.isExpectedFailure());
                analysis.setRecommendation(analysisDto.recommendation());
                analysis.setTestExecution(exec);
                failureAnalysisRepository.save(analysis);

            } catch (Exception e) {
                log.warn("AI analysis failed for execution {}: {}", exec.getId(), e.getMessage());
            }
        }
    }

    private String getExpectedContentType(ApiContractDto contract, String method, String path, int status) {
        return findEndpoint(contract, method, path)
                .map(ep -> {
                    if (ep.responses() == null) return null;
                    ResponseDto resp = ep.responses().get(String.valueOf(status));
                    return resp != null ? resp.contentType() : null;
                }).orElse(null);
    }

    private SchemaDto getExpectedResponseSchema(ApiContractDto contract, String method, String path, int status) {
        return findEndpoint(contract, method, path)
                .map(ep -> {
                    if (ep.responses() == null) return null;
                    ResponseDto resp = ep.responses().get(String.valueOf(status));
                    return resp != null ? resp.schema() : null;
                }).orElse(null);
    }

    private Optional<EndpointDto> findEndpoint(ApiContractDto contract, String method, String path) {
        if (contract == null || contract.endpoints() == null) return Optional.empty();
        return contract.endpoints().stream()
                .filter(e -> method.equalsIgnoreCase(e.method()) && pathMatches(e.path(), path))
                .findFirst();
    }

    private boolean pathMatches(String templatePath, String actualPath) {
        if (templatePath == null || actualPath == null) return false;
        if (templatePath.equals(actualPath)) return true;
        String regex = templatePath.replaceAll("\\{[^}]+\\}", "[^/]+");
        return actualPath.matches(regex);
    }

    @Transactional(readOnly = true)
    public TestRunResponse getTestRun(Long id) {
        return toResponse(findTestRun(id));
    }

    @Transactional(readOnly = true)
    public List<TestExecutionResponse> getTestRunResults(Long id) {
        findTestRun(id);
        return testExecutionRepository.findByTestRunId(id).stream()
                .map(this::toExecutionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestExecutionResponse> getTestRunViolations(Long id) {
        findTestRun(id);
        return testExecutionRepository.findByTestRunIdAndContractViolationTrue(id).stream()
                .map(this::toExecutionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestRunTestCases(Long id) {
        findTestRun(id);
        return testCaseRepository.findByTestRunId(id).stream()
                .map(this::toTestCaseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestRunSummaryResponse getTestRunSummary(Long id) {
        TestRun testRun = findTestRun(id);

        double passRate = testRun.getTotalTests() != null && testRun.getTotalTests() > 0 ?
                (double) testRun.getPassedTests() / testRun.getTotalTests() * 100 : 0;
        double violationRate = testRun.getTotalTests() != null && testRun.getTotalTests() > 0 ?
                (double) testRun.getContractViolations() / testRun.getTotalTests() * 100 : 0;

        List<TestExecution> failedExecs = testExecutionRepository
                .findByTestRunIdAndStatusNot(id, TestExecutionStatus.PASSED);
        List<String> topFailures = failedExecs.stream()
                .filter(e -> e.getContractViolationDetails() != null)
                .map(e -> e.getTestName() + ": " + e.getContractViolationDetails())
                .limit(5)
                .toList();

        String aiSummary = null;
        if (testRun.getStatus() == TestRunStatus.COMPLETED) {
            try {
                TestRunSummaryData summaryData = new TestRunSummaryData(
                        testRun.getProject() != null ? testRun.getProject().getName() : "Unknown",
                        testRun.getSpecification() != null ? testRun.getSpecification().getTitle() : "Unknown",
                        testRun.getTotalTests() != null ? testRun.getTotalTests() : 0,
                        testRun.getPassedTests() != null ? testRun.getPassedTests() : 0,
                        testRun.getFailedTests() != null ? testRun.getFailedTests() : 0,
                        testRun.getContractViolations() != null ? testRun.getContractViolations() : 0,
                        topFailures
                );
                aiSummary = aiEngine.generateTestRunSummary(summaryData);
            } catch (Exception e) {
                log.warn("Could not generate AI summary: {}", e.getMessage());
                aiSummary = String.format("Test run completed. %d/%d tests passed with %d contract violations.",
                        testRun.getPassedTests(), testRun.getTotalTests(), testRun.getContractViolations());
            }
        }

        return new TestRunSummaryResponse(toResponse(testRun), passRate, violationRate, aiSummary, topFailures,
                testRun.getStatus() == TestRunStatus.COMPLETED);
    }

    private void updateTestRunStatus(Long id, TestRunStatus status) {
        testRunRepository.findById(id).ifPresent(tr -> {
            tr.setStatus(status);
            testRunRepository.save(tr);
        });
    }

    private void updateTestRunError(Long id, String error) {
        testRunRepository.findById(id).ifPresent(tr -> {
            tr.setStatus(TestRunStatus.FAILED);
            tr.setErrorMessage(error);
            tr.setCompletedAt(LocalDateTime.now());
            testRunRepository.save(tr);
        });
    }

    @Transactional
    public void updateTestRunCompleted(Long id, int total, int passed, int failed, int violations) {
        testRunRepository.findById(id).ifPresent(tr -> {
            tr.setStatus(TestRunStatus.COMPLETED);
            tr.setTotalTests(total);
            tr.setPassedTests(passed);
            tr.setFailedTests(failed);
            tr.setSkippedTests(0);
            tr.setContractViolations(violations);
            tr.setCompletedAt(LocalDateTime.now());
            if (tr.getStartedAt() != null) {
                tr.setExecutionTimeMs(Duration.between(tr.getStartedAt(), tr.getCompletedAt()).toMillis());
            }
            testRunRepository.save(tr);
        });
    }

    public TestRun findTestRun(Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException("Test run not found with id: " + id));
    }

    private TestRunResponse toResponse(TestRun tr) {
        return new TestRunResponse(
                tr.getId(), tr.getStatus().name(),
                tr.getTotalTests(), tr.getPassedTests(), tr.getFailedTests(), tr.getSkippedTests(),
                tr.getContractViolations(), tr.getExecutionTimeMs(), tr.getErrorMessage(),
                tr.getStartedAt(), tr.getCompletedAt(), tr.getCreatedAt(),
                tr.getProject() != null ? tr.getProject().getId() : null,
                tr.getSpecification() != null ? tr.getSpecification().getId() : null
        );
    }

    private TestExecutionResponse toExecutionResponse(TestExecution exec) {
        FailureAnalysisResponse analysis = null;
        if (exec.getFailureAnalysis() != null) {
            FailureAnalysis fa = exec.getFailureAnalysis();
            analysis = new FailureAnalysisResponse(fa.getId(), fa.getSummary(), fa.getLikelyCause(),
                    fa.getSeverity(), fa.getContractViolation(), fa.getIsExpectedFailure(),
                    fa.getRecommendation(), fa.getAnalyzedAt());
        }
        return new TestExecutionResponse(
                exec.getId(), exec.getTestName(), exec.getMethod(), exec.getEndpoint(),
                exec.getRequestBody(), exec.getRequestHeaders(),
                exec.getExpectedStatus(), exec.getActualStatus(),
                exec.getActualResponseBody(), exec.getActualResponseHeaders(),
                exec.getExecutionTimeMs(), exec.getStatus().name(),
                exec.getContractViolation(), exec.getContractViolationDetails(),
                exec.getExecutedAt(), exec.getTestRun() != null ? exec.getTestRun().getId() : null,
                analysis
        );
    }

    private TestCaseResponse toTestCaseResponse(GeneratedTestCase tc) {
        return new TestCaseResponse(tc.getId(), tc.getName(), tc.getMethod(), tc.getPath(),
                tc.getCategory(), tc.getRequestBody(), tc.getRequestHeaders(), tc.getQueryParams(),
                tc.getExpectedStatus(), tc.getPurpose(), tc.getDescription(),
                tc.getTestRun() != null ? tc.getTestRun().getId() : null);
    }
}
