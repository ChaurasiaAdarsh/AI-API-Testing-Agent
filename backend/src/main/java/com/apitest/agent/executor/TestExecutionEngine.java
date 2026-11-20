package com.apitest.agent.executor;

import com.apitest.agent.config.AppProperties;
import com.apitest.agent.dto.TestExecutionResult;
import com.apitest.agent.model.GeneratedTestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class TestExecutionEngine {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TestExecutionEngine(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(appProperties.getTesting().getTimeoutSeconds() > 0 ?
                        appProperties.getTesting().getTimeoutSeconds() : 10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public TestExecutionResult executeTest(GeneratedTestCase testCase, String baseUrl) {
        long startTime = System.currentTimeMillis();

        try {
            String path = testCase.getPath();
            String queryString = buildQueryString(testCase.getQueryParams());
            String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            String fullUrl = normalizedBaseUrl + normalizedPath + (queryString.isEmpty() ? "" : "?" + queryString);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(appProperties.getTesting().getTimeoutSeconds() > 0 ?
                            appProperties.getTesting().getTimeoutSeconds() : 10));

            addHeaders(requestBuilder, testCase.getRequestHeaders());
            setMethod(requestBuilder, testCase.getMethod(), testCase.getRequestBody());

            HttpRequest request = requestBuilder.build();
            log.debug("Executing {} {}", testCase.getMethod(), fullUrl);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long executionTime = System.currentTimeMillis() - startTime;

            String responseBody = truncateResponse(response.body());
            Map<String, String> responseHeaders = extractHeaders(response.headers());

            log.debug("Test '{}': {} {} -> {} in {}ms",
                    testCase.getName(), testCase.getMethod(), path, response.statusCode(), executionTime);

            return new TestExecutionResult(
                    response.statusCode(),
                    responseBody,
                    responseHeaders,
                    executionTime,
                    false,
                    null
            );

        } catch (HttpTimeoutException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.warn("Test '{}' timed out after {}ms", testCase.getName(), executionTime);
            return new TestExecutionResult(-1, "Request timed out", Map.of(), executionTime, false, "Connection timed out");
        } catch (IOException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.warn("Test '{}' IO error: {}", testCase.getName(), e.getMessage());
            return new TestExecutionResult(-1, "Connection error: " + e.getMessage(), Map.of(), executionTime, false, e.getMessage());
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Test '{}' unexpected error: {}", testCase.getName(), e.getMessage());
            return new TestExecutionResult(-1, "Unexpected error: " + e.getMessage(), Map.of(), executionTime, false, e.getMessage());
        }
    }

    private String buildQueryString(String queryParamsJson) {
        if (queryParamsJson == null || queryParamsJson.isBlank()) return "";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(queryParamsJson, Map.class);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Could not parse query params: {}", e.getMessage());
            return "";
        }
    }

    private void addHeaders(HttpRequest.Builder builder, String headersJson) {
        if (headersJson == null || headersJson.isBlank()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> headers = objectMapper.readValue(headersJson, Map.class);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String name = entry.getKey();
                if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                    builder.header(name, entry.getValue());
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse request headers: {}", e.getMessage());
        }
    }

    private void setMethod(HttpRequest.Builder builder, String method, String bodyJson) {
        HttpRequest.BodyPublisher bodyPublisher = (bodyJson != null && !bodyJson.isBlank())
                ? HttpRequest.BodyPublishers.ofString(bodyJson)
                : HttpRequest.BodyPublishers.noBody();

        switch (method.toUpperCase()) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(
                    bodyJson != null ? bodyJson : ""));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(
                    bodyJson != null ? bodyJson : ""));
            case "DELETE" -> builder.DELETE();
            case "PATCH" -> builder.method("PATCH", bodyPublisher);
            default -> builder.method(method, bodyPublisher);
        }
    }

    private String truncateResponse(String body) {
        if (body == null) return null;
        int maxBytes = appProperties.getTesting().getMaxResponseBodyBytes();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return body;
        return new String(bytes, 0, maxBytes, StandardCharsets.UTF_8) + "\n... [truncated]";
    }

    private Map<String, String> extractHeaders(HttpHeaders headers) {
        Map<String, String> result = new LinkedHashMap<>();
        headers.map().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("authorization") && !name.equalsIgnoreCase("set-cookie")) {
                result.put(name.toLowerCase(), String.join(", ", values));
            }
        });
        return result;
    }
}
