package com.apitest.agent.parser;

import com.apitest.agent.dto.*;
import com.apitest.agent.exception.OpenApiParseException;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenApiParserService {

    public ApiContractDto parse(String content) {
        if (StringUtils.isBlank(content)) {
            throw new OpenApiParseException("OpenAPI content is empty or null");
        }

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, parseOptions);

        if (result == null || result.getOpenAPI() == null) {
            String messages = result != null && result.getMessages() != null ?
                    String.join(", ", result.getMessages()) : "Unknown parse error";
            throw new OpenApiParseException("Failed to parse OpenAPI specification: " + messages);
        }

        OpenAPI openAPI = result.getOpenAPI();

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            log.warn("OpenAPI parse warnings: {}", result.getMessages());
        }

        String title = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown API";
        String version = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "1.0.0";
        String baseUrl = extractBaseUrl(openAPI);

        List<EndpointDto> endpoints = extractEndpoints(openAPI);
        log.info("Parsed OpenAPI spec: {} v{} with {} endpoints", title, version, endpoints.size());

        return new ApiContractDto(title, version, baseUrl, endpoints);
    }

    public String detectFormat(String content) {
        if (content == null) return "YAML";
        String trimmed = content.trim();
        return (trimmed.startsWith("{") || trimmed.startsWith("[")) ? "JSON" : "YAML";
    }

    private String extractBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            return openAPI.getServers().get(0).getUrl();
        }
        return "http://localhost:8080";
    }

    private List<EndpointDto> extractEndpoints(OpenAPI openAPI) {
        List<EndpointDto> endpoints = new ArrayList<>();
        if (openAPI.getPaths() == null) return endpoints;

        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            addOperationEndpoint(endpoints, path, "GET", pathItem.getGet(), pathItem.getParameters());
            addOperationEndpoint(endpoints, path, "POST", pathItem.getPost(), pathItem.getParameters());
            addOperationEndpoint(endpoints, path, "PUT", pathItem.getPut(), pathItem.getParameters());
            addOperationEndpoint(endpoints, path, "DELETE", pathItem.getDelete(), pathItem.getParameters());
            addOperationEndpoint(endpoints, path, "PATCH", pathItem.getPatch(), pathItem.getParameters());
        }

        return endpoints;
    }

    private void addOperationEndpoint(List<EndpointDto> endpoints, String path, String method,
                                       io.swagger.v3.oas.models.Operation operation,
                                       List<Parameter> pathLevelParams) {
        if (operation == null) return;

        List<ParameterDto> parameters = new ArrayList<>();

        if (pathLevelParams != null) {
            pathLevelParams.forEach(p -> parameters.add(convertParameter(p)));
        }
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(p -> parameters.add(convertParameter(p)));
        }

        RequestBodyDto requestBody = convertRequestBody(operation.getRequestBody());
        Map<String, ResponseDto> responses = convertResponses(operation.getResponses());

        List<String> security = new ArrayList<>();
        if (operation.getSecurity() != null) {
            operation.getSecurity().forEach(sr -> security.addAll(sr.keySet()));
        }

        endpoints.add(new EndpointDto(
                method, path,
                operation.getSummary(),
                parameters,
                requestBody,
                responses,
                security
        ));
    }

    private ParameterDto convertParameter(Parameter p) {
        return new ParameterDto(
                p.getName(),
                p.getIn(),
                Boolean.TRUE.equals(p.getRequired()),
                p.getSchema() != null ? convertSchema(p.getSchema()) : null
        );
    }

    private RequestBodyDto convertRequestBody(RequestBody rb) {
        if (rb == null) return null;

        Content content = rb.getContent();
        if (content == null) return new RequestBodyDto(Boolean.TRUE.equals(rb.getRequired()), null, null);

        MediaType mediaType = content.get("application/json");
        if (mediaType == null && !content.isEmpty()) {
            mediaType = content.values().iterator().next();
        }

        String contentType = content.containsKey("application/json") ? "application/json" :
                content.keySet().stream().findFirst().orElse(null);

        SchemaDto schema = mediaType != null && mediaType.getSchema() != null ?
                convertSchema(mediaType.getSchema()) : null;

        return new RequestBodyDto(Boolean.TRUE.equals(rb.getRequired()), schema, contentType);
    }

    private Map<String, ResponseDto> convertResponses(io.swagger.v3.oas.models.responses.ApiResponses responses) {
        if (responses == null) return Map.of();

        Map<String, ResponseDto> result = new LinkedHashMap<>();
        for (Map.Entry<String, ApiResponse> entry : responses.entrySet()) {
            ApiResponse resp = entry.getValue();
            String contentType = null;
            SchemaDto schema = null;

            if (resp.getContent() != null) {
                MediaType mt = resp.getContent().get("application/json");
                if (mt == null && !resp.getContent().isEmpty()) {
                    mt = resp.getContent().values().iterator().next();
                }
                contentType = resp.getContent().containsKey("application/json") ? "application/json" :
                        resp.getContent().keySet().stream().findFirst().orElse(null);
                schema = mt != null && mt.getSchema() != null ? convertSchema(mt.getSchema()) : null;
            }

            result.put(entry.getKey(), new ResponseDto(resp.getDescription(), schema, contentType));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private SchemaDto convertSchema(Schema<?> schema) {
        if (schema == null) return null;

        List<String> enumValues = null;
        if (schema.getEnum() != null) {
            enumValues = schema.getEnum().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        Map<String, SchemaDto> properties = null;
        if (schema.getProperties() != null) {
            properties = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : ((Map<String, Schema>) schema.getProperties()).entrySet()) {
                properties.put(entry.getKey(), convertSchema(entry.getValue()));
            }
        }

        List<String> required = schema.getRequired();

        SchemaDto items = null;
        if (schema instanceof ArraySchema as) {
            items = convertSchema(as.getItems());
        }

        return new SchemaDto(
                schema.getType(),
                schema.getFormat(),
                schema.getMinimum() != null ? schema.getMinimum() : schema.getExclusiveMinimumValue(),
                schema.getMaximum() != null ? schema.getMaximum() : schema.getExclusiveMaximumValue(),
                schema.getMinLength(),
                schema.getMaxLength(),
                schema.getPattern(),
                enumValues,
                required,
                properties,
                items,
                Boolean.TRUE.equals(schema.getNullable())
        );
    }
}
