package com.apitest.agent.ai;

import com.apitest.agent.config.AppProperties;
import com.apitest.agent.dto.*;
import com.apitest.agent.exception.AiEngineException;
import com.apitest.agent.model.GeneratedTestCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ollamaAiEngine")
@Slf4j
public class OllamaAiEngine implements AiEngine {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public OllamaAiEngine(ChatModel chatModel, ObjectMapper objectMapper, AppProperties appProperties) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    private String callModel(String promptText, boolean jsonFormat) {
        if (jsonFormat) {
            try {
                OllamaOptions options = new OllamaOptions();
                options.setFormat("json");
                options.setTemperature(0.1);
                Prompt prompt = new Prompt(promptText, options);
                ChatResponse resp = chatModel.call(prompt);
                if (resp != null && resp.getResult() != null && resp.getResult().getOutput() != null) {
                    String content = resp.getResult().getOutput().getText();
                    if (content != null && !content.isBlank()) {
                        return content;
                    }
                }
            } catch (Exception e) {
                log.warn("Ollama call with format=json failed ({}), trying standard call", e.getMessage());
            }
        }
        return chatModel.call(promptText);
    }

    @Override
    public List<GeneratedTestCaseDto> generateTestCases(ApiContractDto contract, int maxTests) {
        log.info("AI test generation started: api={} v={}, endpoints={}, maxTests={}",
                contract.title(), contract.version(),
                contract.endpoints() != null ? contract.endpoints().size() : 0, maxTests);

        long startTime = System.currentTimeMillis();
        String prompt = buildTestGenerationPrompt(contract, maxTests);

        try {
            String response = callModel(prompt, true);

            if (response == null || response.isBlank()) {
                throw new AiEngineException("AI engine returned empty response for test generation");
            }

            String json = extractJson(response);
            List<GeneratedTestCaseDto> testCases = parseTestCases(json);

            List<GeneratedTestCaseDto> validCases = testCases.stream()
                    .filter(this::isValidTestCase)
                    .limit(maxTests)
                    .toList();

            if (validCases.isEmpty()) {
                throw new AiEngineException("AI generated no valid test cases from response: " + truncate(response, 200));
            }

            log.info("AI test generation completed: generated={}, valid={}, elapsed={}ms",
                    testCases.size(), validCases.size(), System.currentTimeMillis() - startTime);

            return validCases;

        } catch (AiEngineException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI test generation failed: {}", e.getMessage(), e);
            throw new AiEngineException("AI test generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public FailureAnalysisDto analyzeFailure(GeneratedTestCase testCase, TestExecutionResult result, ApiContractDto contract) {
        log.debug("AI failure analysis started: test={}, expectedStatus={}, actualStatus={}",
                testCase.getName(), testCase.getExpectedStatus(), result.actualStatus());

        try {
            String prompt = buildFailureAnalysisPrompt(testCase, result, contract);

            String response = callModel(prompt, true);

            if (response == null || response.isBlank()) {
                return defaultFailureAnalysis(testCase, result);
            }

            String json = extractJson(response);
            FailureAnalysisDto analysis = parseFailureAnalysis(json, testCase, result);

            log.debug("AI failure analysis completed: test={}, severity={}, contractViolation={}",
                    testCase.getName(), analysis.severity(), analysis.contractViolation());

            return analysis;

        } catch (Exception e) {
            log.warn("AI failure analysis failed for test '{}': {}", testCase.getName(), e.getMessage());
            return defaultFailureAnalysis(testCase, result);
        }
    }

    @Override
    public String generateTestRunSummary(TestRunSummaryData summaryData) {
        log.debug("AI summary generation started: api={}, total={}, passed={}",
                summaryData.apiTitle(), summaryData.totalTests(), summaryData.passed());

        try {
            String prompt = buildSummaryPrompt(summaryData);

            String response = callModel(prompt, false);

            if (response == null || response.isBlank()) {
                return defaultSummary(summaryData);
            }

            return response.trim();

        } catch (Exception e) {
            log.warn("AI summary generation failed: {}", e.getMessage());
            return defaultSummary(summaryData);
        }
    }

    private String buildTestGenerationPrompt(ApiContractDto contract, int maxTests) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert API quality engineer. Inspect this OpenAPI specification and generate exactly ")
                .append(maxTests)
                .append(" high-value, realistic automated test cases.\n\n");

        prompt.append("API TITLE: ").append(contract.title()).append("\n");
        prompt.append("API VERSION: ").append(contract.version()).append("\n");

        prompt.append("\nENDPOINTS:\n");

        List<EndpointDto> endpoints = contract.endpoints();
        if (endpoints == null) endpoints = List.of();

        int maxEndpoints = Math.min(endpoints.size(), 20);
        if (endpoints.size() > 20) {
            prompt.append("(Showing first 20 of ").append(endpoints.size()).append(" endpoints)\n");
        }

        for (int i = 0; i < maxEndpoints; i++) {
            prompt.append(buildEndpointSummary(endpoints.get(i)));
        }

        prompt.append("\nGENERATE ").append(maxTests).append(" diverse test cases distributed across:\n");
        prompt.append("1. POSITIVE tests: valid requests that should succeed\n");
        prompt.append("2. NEGATIVE tests: missing required fields, invalid types, invalid enums, empty values\n");
        prompt.append("3. BOUNDARY tests: values at exact min/max boundaries\n\n");
        prompt.append("Return ONLY valid JSON in this EXACT structure with no extra text:\n");
        prompt.append("{\n  \"testCases\": [\n    {\n");
        prompt.append("      \"name\": \"Short test title\",\n");
        prompt.append("      \"method\": \"POST\",\n");
        prompt.append("      \"path\": \"/demo/users\",\n");
        prompt.append("      \"category\": \"POSITIVE\",\n");
        prompt.append("      \"requestBody\": {\"field\": \"value\"},\n");
        prompt.append("      \"requestHeaders\": {\"Content-Type\": \"application/json\"},\n");
        prompt.append("      \"queryParams\": {},\n");
        prompt.append("      \"expectedStatus\": 200,\n");
        prompt.append("      \"purpose\": \"Purpose of test\",\n");
        prompt.append("      \"description\": \"Detailed explanation\"\n");
        prompt.append("    }\n  ]\n}\n");

        return prompt.toString();
    }

    private String buildEndpointSummary(EndpointDto endpoint) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(endpoint.method()).append(" ").append(endpoint.path());
        if (endpoint.summary() != null) sb.append(" - ").append(endpoint.summary());
        sb.append("\n");

        if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
            sb.append("  Parameters:\n");
            for (ParameterDto param : endpoint.parameters()) {
                sb.append("    ").append(param.name())
                        .append(" (").append(param.in()).append(")")
                        .append(param.required() ? " [required]" : " [optional]");
                if (param.schema() != null) {
                    sb.append(" type=").append(param.schema().type());
                    if (param.schema().minimum() != null) sb.append(" min=").append(param.schema().minimum());
                    if (param.schema().maximum() != null) sb.append(" max=").append(param.schema().maximum());
                    if (param.schema().enumValues() != null && !param.schema().enumValues().isEmpty()) {
                        sb.append(" enum=").append(param.schema().enumValues());
                    }
                }
                sb.append("\n");
            }
        }

        if (endpoint.requestBody() != null && endpoint.requestBody().schema() != null) {
            sb.append("  Request Body (")
                    .append(endpoint.requestBody().required() ? "required" : "optional")
                    .append("):\n");
            sb.append(buildSchemaDescription(endpoint.requestBody().schema(), "    "));
        }

        if (endpoint.responses() != null && !endpoint.responses().isEmpty()) {
            sb.append("  Responses:\n");
            endpoint.responses().forEach((status, resp) -> {
                sb.append("    ").append(status).append(": ");
                sb.append(resp.description() != null ? resp.description() : "");
                sb.append("\n");
            });
        }

        return sb.toString();
    }

    private String buildSchemaDescription(SchemaDto schema, String indent) {
        if (schema == null) return "";
        StringBuilder sb = new StringBuilder();

        if ("object".equals(schema.type()) && schema.properties() != null) {
            schema.properties().forEach((name, propSchema) -> {
                boolean isRequired = schema.required() != null && schema.required().contains(name);
                sb.append(indent).append(name)
                        .append(isRequired ? " [required]" : " [optional]")
                        .append(" type=").append(propSchema.type() != null ? propSchema.type() : "unknown");
                if (propSchema.minimum() != null) sb.append(" min=").append(propSchema.minimum());
                if (propSchema.maximum() != null) sb.append(" max=").append(propSchema.maximum());
                if (propSchema.minLength() != null) sb.append(" minLen=").append(propSchema.minLength());
                if (propSchema.maxLength() != null) sb.append(" maxLen=").append(propSchema.maxLength());
                if (propSchema.pattern() != null) sb.append(" pattern=").append(propSchema.pattern());
                if (propSchema.enumValues() != null && !propSchema.enumValues().isEmpty()) {
                    sb.append(" enum=").append(propSchema.enumValues());
                }
                sb.append("\n");
            });
        } else if (schema.type() != null) {
            sb.append(indent).append("type=").append(schema.type());
            if (schema.minimum() != null) sb.append(" min=").append(schema.minimum());
            if (schema.maximum() != null) sb.append(" max=").append(schema.maximum());
            if (schema.enumValues() != null) sb.append(" enum=").append(schema.enumValues());
            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildFailureAnalysisPrompt(GeneratedTestCase testCase, TestExecutionResult result, ApiContractDto contract) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an API contract intelligence agent. Analyze this test failure and determine whether it represents an actual API contract violation or expected validation behavior.\n\n");
        prompt.append("TEST DETAILS:\n");
        prompt.append("Name: ").append(testCase.getName()).append("\n");
        prompt.append("Method: ").append(testCase.getMethod()).append("\n");
        prompt.append("Path: ").append(testCase.getPath()).append("\n");
        prompt.append("Category: ").append(testCase.getCategory()).append("\n");
        prompt.append("Purpose: ").append(testCase.getPurpose()).append("\n");
        prompt.append("Expected Status: ").append(testCase.getExpectedStatus()).append("\n\n");

        if (testCase.getRequestBody() != null) {
            prompt.append("Request Body:\n").append(testCase.getRequestBody()).append("\n\n");
        }

        prompt.append("ACTUAL EXECUTION RESULT:\n");
        prompt.append("Actual Status: ").append(result.actualStatus()).append("\n");
        prompt.append("Execution Time: ").append(result.executionTimeMs()).append("ms\n");

        if (result.actualResponseBody() != null) {
            prompt.append("Actual Response Body:\n").append(truncate(result.actualResponseBody(), 1000)).append("\n\n");
        }

        if (contract != null && contract.endpoints() != null) {
            contract.endpoints().stream()
                    .filter(e -> e.method().equalsIgnoreCase(testCase.getMethod()) &&
                            pathTemplateMatches(e.path(), testCase.getPath()))
                    .findFirst()
                    .ifPresent(ep -> prompt.append("DECLARED OPENAPI CONTRACT:\n").append(buildEndpointSummary(ep)).append("\n\n"));
        }

        prompt.append("Provide structured analysis as valid JSON with these fields:\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Brief summary of what happened\",\n");
        prompt.append("  \"likelyCause\": \"Root cause explanation\",\n");
        prompt.append("  \"severity\": \"LOW|MEDIUM|HIGH|CRITICAL\",\n");
        prompt.append("  \"contractViolation\": true|false,\n");
        prompt.append("  \"isExpectedFailure\": true|false,\n");
        prompt.append("  \"recommendation\": \"Specific actionable recommendation\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private String buildSummaryPrompt(TestRunSummaryData data) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Summarize these API test results in 3-5 sentences for a developer dashboard.\n\n");
        prompt.append("Project: ").append(data.projectName()).append("\n");
        prompt.append("API: ").append(data.apiTitle()).append("\n");
        prompt.append("Results: ").append(data.passed()).append(" passed, ")
                .append(data.failed()).append(" failed out of ")
                .append(data.totalTests()).append(" total tests\n");
        prompt.append("Contract Violations: ").append(data.contractViolations()).append("\n");

        if (data.topFailureReasons() != null && !data.topFailureReasons().isEmpty()) {
            prompt.append("Top failure reasons:\n");
            data.topFailureReasons().forEach(r -> prompt.append("- ").append(r).append("\n"));
        }

        prompt.append("\nProvide a concise professional summary highlighting key findings and overall API health.\n");
        prompt.append("Return plain text only.");

        return prompt.toString();
    }

    private String extractJson(String response) {
        if (response == null) return "{}";
        String cleaned = response.trim();

        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastBlock = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastBlock > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastBlock).trim();
            }
        }

        int startObj = cleaned.indexOf('{');
        int startArr = cleaned.indexOf('[');
        int start = -1;
        char endChar = '}';

        if (startObj >= 0 && (startArr < 0 || startObj < startArr)) {
            start = startObj;
            endChar = '}';
        } else if (startArr >= 0) {
            start = startArr;
            endChar = ']';
        }

        if (start >= 0) {
            int end = cleaned.lastIndexOf(endChar);
            if (end > start) {
                cleaned = cleaned.substring(start, end + 1);
            } else {
                cleaned = cleaned.substring(start) + endChar;
            }
        }

        return cleaned;
    }

    private List<GeneratedTestCaseDto> parseTestCases(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode testCasesNode = null;

            if (root.isArray()) {
                testCasesNode = root;
            } else if (root.has("testCases") && root.get("testCases").isArray()) {
                testCasesNode = root.get("testCases");
            } else if (root.has("tests") && root.get("tests").isArray()) {
                testCasesNode = root.get("tests");
            } else if (root.has("name") && root.has("method")) {
                GeneratedTestCaseDto tc = parseTestCaseNode(root);
                return tc != null ? List.of(tc) : List.of();
            }

            if (testCasesNode != null && testCasesNode.isArray()) {
                List<GeneratedTestCaseDto> result = new ArrayList<>();
                for (JsonNode tcNode : testCasesNode) {
                    try {
                        GeneratedTestCaseDto tc = parseTestCaseNode(tcNode);
                        if (tc != null) {
                            result.add(tc);
                        }
                    } catch (Exception e) {
                        log.debug("Skipping invalid test case node: {}", e.getMessage());
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("Standard JSON parsing failed, attempting regex extraction: {}", e.getMessage());
        }

        return fallbackExtractCases(json);
    }

    private List<GeneratedTestCaseDto> fallbackExtractCases(String raw) {
        List<GeneratedTestCaseDto> cases = new ArrayList<>();
        if (raw == null || raw.isBlank()) return cases;

        Pattern pattern = Pattern.compile(
                "\\{[^{}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"method\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(raw);

        while (matcher.find()) {
            try {
                String candidate = matcher.group(0);
                JsonNode node = objectMapper.readTree(candidate);
                GeneratedTestCaseDto tc = parseTestCaseNode(node);
                if (tc != null && isValidTestCase(tc)) {
                    cases.add(tc);
                }
            } catch (Exception ignored) {
            }
        }
        return cases;
    }

    private GeneratedTestCaseDto parseTestCaseNode(JsonNode node) {
        String name = getText(node, "name");
        String method = getText(node, "method");
        String path = getText(node, "path");
        String category = getText(node, "category");
        if (category == null || category.isBlank()) {
            category = "POSITIVE";
        }
        Integer expectedStatus = getInt(node, "expectedStatus");
        if (expectedStatus == null) {
            expectedStatus = "POSITIVE".equalsIgnoreCase(category) ? 200 : 400;
        }
        String purpose = getText(node, "purpose");
        String description = getText(node, "description");

        Map<String, Object> requestBody = null;
        JsonNode rbNode = node.get("requestBody");
        if (rbNode != null && rbNode.isObject()) {
            requestBody = objectMapper.convertValue(rbNode, new TypeReference<Map<String, Object>>() {});
        }

        Map<String, String> requestHeaders = Map.of("Content-Type", "application/json");
        JsonNode rhNode = node.get("requestHeaders");
        if (rhNode != null && rhNode.isObject()) {
            requestHeaders = objectMapper.convertValue(rhNode, new TypeReference<Map<String, String>>() {});
        }

        Map<String, String> queryParams = Map.of();
        JsonNode qpNode = node.get("queryParams");
        if (qpNode != null && qpNode.isObject()) {
            queryParams = objectMapper.convertValue(qpNode, new TypeReference<Map<String, String>>() {});
        }

        return new GeneratedTestCaseDto(
                name, method, path, category,
                requestBody, requestHeaders, queryParams,
                expectedStatus, purpose, description
        );
    }

    private FailureAnalysisDto parseFailureAnalysis(String json, GeneratedTestCase testCase, TestExecutionResult result) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String summary = getText(root, "summary");
            String likelyCause = getText(root, "likelyCause");
            String severity = getText(root, "severity");
            boolean contractViolation = getBool(root, "contractViolation");
            boolean isExpectedFailure = getBool(root, "isExpectedFailure");
            String recommendation = getText(root, "recommendation");

            if (severity == null || !List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(severity)) {
                severity = "MEDIUM";
            }

            if (summary == null) summary = "Analysis of test failure for " + testCase.getName();
            if (likelyCause == null) likelyCause = "Unknown cause";
            if (recommendation == null) recommendation = "Review the test and API implementation";

            return new FailureAnalysisDto(summary, likelyCause, severity, contractViolation, isExpectedFailure, recommendation);

        } catch (Exception e) {
            log.warn("Could not parse AI failure analysis: {}", e.getMessage());
            return defaultFailureAnalysis(testCase, result);
        }
    }

    private boolean isValidTestCase(GeneratedTestCaseDto tc) {
        if (tc == null) return false;
        if (tc.name() == null || tc.name().isBlank()) return false;
        if (tc.method() == null || tc.method().isBlank()) return false;
        if (tc.path() == null || tc.path().isBlank()) return false;
        if (tc.expectedStatus() == null || tc.expectedStatus() < 100 || tc.expectedStatus() > 599) return false;
        return true;
    }

    private FailureAnalysisDto defaultFailureAnalysis(GeneratedTestCase testCase, TestExecutionResult result) {
        boolean isExpected = "NEGATIVE".equals(testCase.getCategory()) &&
                result.actualStatus() >= 400 && result.actualStatus() < 500;

        return new FailureAnalysisDto(
                "Analysis: test '" + testCase.getName() + "' resulted in HTTP " + result.actualStatus() +
                        " (expected " + testCase.getExpectedStatus() + ")",
                isExpected ? "Negative validation correctly rejected request" : "Potential contract or implementation mismatch",
                isExpected ? "LOW" : "MEDIUM",
                !isExpected,
                isExpected,
                isExpected ? "No action required" : "Verify API returns the status code specified in the OpenAPI contract"
        );
    }

    private String defaultSummary(TestRunSummaryData data) {
        return String.format(
                "Execution finished for %s: %d total tests run with %d passed and %d failed. %d contract violations detected.",
                data.apiTitle(), data.totalTests(), data.passed(), data.failed(), data.contractViolations()
        );
    }

    private boolean pathTemplateMatches(String template, String actual) {
        if (template == null || actual == null) return false;
        if (template.equals(actual)) return true;
        String regex = template.replaceAll("\\{[^}]+\\}", "[^/]+");
        return actual.matches(regex);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private String getText(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && n.isTextual()) ? n.asText() : null;
    }

    private Integer getInt(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null) return null;
        if (n.isNumber()) return n.asInt();
        if (n.isTextual()) {
            String txt = n.asText().trim();
            Matcher m = Pattern.compile("^\\d{3}").matcher(txt);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private boolean getBool(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n != null && n.isBoolean() && n.asBoolean();
    }
}
