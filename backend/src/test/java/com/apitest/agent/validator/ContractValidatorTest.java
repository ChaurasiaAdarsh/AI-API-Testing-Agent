package com.apitest.agent.validator;

import com.apitest.agent.dto.SchemaDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ContractValidatorTest {

    private ContractValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ContractValidator(new ObjectMapper());
    }

    @Test
    void shouldDetectStatusCodeMismatch() {
        Optional<String> violation = validator.compareStatusCode(201, 200);
        assertThat(violation).isPresent();
        assertThat(violation.get()).contains("201");
        assertThat(violation.get()).contains("200");
    }

    @Test
    void shouldPassWhenStatusCodesMatch() {
        Optional<String> violation = validator.compareStatusCode(200, 200);
        assertThat(violation).isEmpty();
    }

    @Test
    void shouldDetect404vs200Mismatch() {
        Optional<String> violation = validator.compareStatusCode(201, 404);
        assertThat(violation).isPresent();
    }

    @Test
    void shouldDetectContentTypeMismatch() {
        Optional<String> violation = validator.validateContentType("application/json", "text/plain");
        assertThat(violation).isPresent();
    }

    @Test
    void shouldPassWhenContentTypesMatchExactly() {
        Optional<String> violation = validator.validateContentType("application/json", "application/json");
        assertThat(violation).isEmpty();
    }

    @Test
    void shouldPassWhenContentTypeHasCharset() {
        Optional<String> violation = validator.validateContentType("application/json", "application/json; charset=utf-8");
        assertThat(violation).isEmpty();
    }

    @Test
    void shouldPassWhenNoExpectedContentType() {
        Optional<String> violation = validator.validateContentType(null, "application/json");
        assertThat(violation).isEmpty();
    }

    @Test
    void shouldPassWhenExpectedContentTypeIsBlank() {
        Optional<String> violation = validator.validateContentType("", "application/json");
        assertThat(violation).isEmpty();
    }

    @Test
    void shouldPassWhenAllRequiredFieldsPresent() {
        SchemaDto schema = buildObjectSchema(
                List.of("id", "name"),
                Map.of(
                        "id", buildPrimitiveSchema("integer"),
                        "name", buildPrimitiveSchema("string")
                )
        );

        List<String> violations = validator.validateResponseSchema("{\"id\": 1, \"name\": \"John\"}", schema);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldDetectMissingRequiredField() {
        SchemaDto schema = buildObjectSchema(
                List.of("id", "name"),
                Map.of(
                        "id", buildPrimitiveSchema("integer"),
                        "name", buildPrimitiveSchema("string")
                )
        );

        List<String> violations = validator.validateResponseSchema("{\"id\": 1}", schema);
        assertThat(violations).anyMatch(v -> v.contains("name"));
    }

    @Test
    void shouldDetectMultipleMissingRequiredFields() {
        SchemaDto schema = buildObjectSchema(
                List.of("id", "name", "email"),
                Map.of(
                        "id", buildPrimitiveSchema("integer"),
                        "name", buildPrimitiveSchema("string"),
                        "email", buildPrimitiveSchema("string")
                )
        );

        List<String> violations = validator.validateResponseSchema("{}", schema);
        assertThat(violations).hasSize(3);
    }

    @Test
    void shouldReturnEmptyListForNullSchema() {
        List<String> violations = validator.validateResponseSchema("{\"id\": 1}", null);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForNullResponseBody() {
        SchemaDto schema = buildObjectSchema(List.of("id"), Map.of("id", buildPrimitiveSchema("integer")));
        List<String> violations = validator.validateResponseSchema(null, schema);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        SchemaDto schema = buildObjectSchema(List.of("id"), Map.of("id", buildPrimitiveSchema("integer")));
        List<String> violations = validator.validateResponseSchema("not-json", schema);
        assertThat(violations).isNotNull();
    }

    @Test
    void shouldGenerateViolationSummary() {
        List<String> violations = List.of("Status mismatch: 201 vs 200", "Missing field 'name'");
        String summary = validator.generateViolationSummary(violations);
        assertThat(summary).contains("Status mismatch");
        assertThat(summary).contains("Missing field 'name'");
    }

    @Test
    void shouldReturnNullForEmptyViolationList() {
        String summary = validator.generateViolationSummary(List.of());
        assertThat(summary).isNull();
    }

    private SchemaDto buildObjectSchema(List<String> required, Map<String, SchemaDto> properties) {
        return new SchemaDto("object", null, null, null, null, null, null, null,
                required, properties, null, false);
    }

    private SchemaDto buildPrimitiveSchema(String type) {
        return new SchemaDto(type, null, null, null, null, null, null, null,
                null, null, null, false);
    }
}
