package com.apitest.agent.validator;

import com.apitest.agent.dto.SchemaDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ContractValidator {

    private final ObjectMapper objectMapper;

    public ContractValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> compareStatusCode(int expected, int actual) {
        if (expected != actual) {
            return Optional.of(String.format(
                    "Status code mismatch: OpenAPI contract declares %d but API returned %d", expected, actual));
        }
        return Optional.empty();
    }

    public List<String> validateResponseSchema(String responseBody, SchemaDto expectedSchema) {
        List<String> violations = new ArrayList<>();

        if (expectedSchema == null || responseBody == null || responseBody.isBlank()) {
            return violations;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            validateSchema(rootNode, expectedSchema, "", violations);
        } catch (Exception e) {
            log.debug("Could not parse response body for schema validation: {}", e.getMessage());
            violations.add("Response body could not be parsed as JSON: " + e.getMessage());
        }

        return violations;
    }

    private void validateSchema(JsonNode node, SchemaDto schema, String path, List<String> violations) {
        if (schema == null || node == null) return;

        if ("object".equals(schema.type()) && schema.required() != null) {
            for (String requiredField : schema.required()) {
                if (!node.has(requiredField) || node.get(requiredField).isNull()) {
                    violations.add(String.format("Missing required field '%s%s' in response",
                            path.isEmpty() ? "" : path + ".", requiredField));
                }
            }
        }

        if ("object".equals(schema.type()) && schema.properties() != null) {
            for (Map.Entry<String, SchemaDto> entry : schema.properties().entrySet()) {
                String fieldName = entry.getKey();
                SchemaDto fieldSchema = entry.getValue();
                JsonNode fieldNode = node.get(fieldName);

                if (fieldNode != null && !fieldNode.isNull() && fieldSchema != null) {
                    validateFieldType(fieldNode, fieldSchema, path.isEmpty() ? fieldName : path + "." + fieldName, violations);
                    if ("object".equals(fieldSchema.type())) {
                        validateSchema(fieldNode, fieldSchema, path.isEmpty() ? fieldName : path + "." + fieldName, violations);
                    }
                }
            }
        }
    }

    private void validateFieldType(JsonNode node, SchemaDto schema, String path, List<String> violations) {
        if (schema.type() == null) return;

        boolean valid = switch (schema.type()) {
            case "integer", "number" -> node.isNumber();
            case "string" -> node.isTextual();
            case "boolean" -> node.isBoolean();
            case "array" -> node.isArray();
            case "object" -> node.isObject();
            default -> true;
        };

        if (!valid) {
            violations.add(String.format("Field '%s' has wrong type: expected %s but got %s",
                    path, schema.type(), getNodeType(node)));
        }
    }

    private String getNodeType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isInt() || node.isLong()) return "integer";
        if (node.isDouble() || node.isFloat()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        if (node.isNull()) return "null";
        return "unknown";
    }

    public Optional<String> validateContentType(String expectedContentType, String actualContentType) {
        if (expectedContentType == null || expectedContentType.isBlank()) return Optional.empty();
        if (actualContentType == null || actualContentType.isBlank()) {
            return Optional.of("Missing Content-Type header; expected: " + expectedContentType);
        }

        String expectedBase = expectedContentType.split(";")[0].trim().toLowerCase();
        String actualBase = actualContentType.split(";")[0].trim().toLowerCase();

        if (!actualBase.equals(expectedBase)) {
            return Optional.of(String.format("Content-Type mismatch: expected '%s' but received '%s'",
                    expectedContentType, actualContentType));
        }
        return Optional.empty();
    }

    public String generateViolationSummary(List<String> violations) {
        if (violations == null || violations.isEmpty()) return null;
        return String.join("; ", violations);
    }
}
