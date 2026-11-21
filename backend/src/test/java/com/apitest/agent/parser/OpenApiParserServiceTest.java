package com.apitest.agent.parser;

import com.apitest.agent.dto.*;
import com.apitest.agent.exception.OpenApiParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OpenApiParserServiceTest {

    private OpenApiParserService parserService;

    private static final String SAMPLE_YAML = """
            openapi: "3.0.3"
            info:
              title: "User API"
              version: "1.0.0"
            paths:
              /users:
                post:
                  summary: "Create user"
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema:
                          type: object
                          required: [name, email, age]
                          properties:
                            name:
                              type: string
                              minLength: 2
                              maxLength: 100
                            email:
                              type: string
                              format: email
                            age:
                              type: integer
                              minimum: 18
                              maximum: 120
                            role:
                              type: string
                              enum: [user, admin, moderator]
                  responses:
                    "201":
                      description: "User created"
                      content:
                        application/json:
                          schema:
                            type: object
                            required: [id, name]
                            properties:
                              id:
                                type: integer
                              name:
                                type: string
              /users/{id}:
                get:
                  summary: "Get user"
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema:
                        type: integer
                  responses:
                    "200":
                      description: "User found"
                    "404":
                      description: "Not found"
            """;

    @BeforeEach
    void setUp() {
        parserService = new OpenApiParserService();
    }

    @Test
    void shouldParseYamlSuccessfully() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        assertThat(contract).isNotNull();
        assertThat(contract.title()).isEqualTo("User API");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(contract.endpoints()).hasSize(2);
    }

    @Test
    void shouldExtractPostEndpointWithRequestBody() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto postEndpoint = contract.endpoints().stream()
                .filter(e -> "POST".equals(e.method()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("POST endpoint not found"));

        assertThat(postEndpoint.requestBody()).isNotNull();
        assertThat(postEndpoint.requestBody().required()).isTrue();
        assertThat(postEndpoint.requestBody().schema()).isNotNull();
        assertThat(postEndpoint.requestBody().schema().properties()).containsKey("name");
        assertThat(postEndpoint.requestBody().schema().properties()).containsKey("age");
    }

    @Test
    void shouldExtractMinMaxConstraints() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto postEndpoint = contract.endpoints().stream()
                .filter(e -> "POST".equals(e.method()))
                .findFirst()
                .orElseThrow();

        SchemaDto ageSchema = postEndpoint.requestBody().schema().properties().get("age");
        assertThat(ageSchema).isNotNull();
        assertThat(ageSchema.minimum()).isNotNull();
        assertThat(ageSchema.maximum()).isNotNull();
    }

    @Test
    void shouldExtractMinLengthMaxLength() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto postEndpoint = contract.endpoints().stream()
                .filter(e -> "POST".equals(e.method()))
                .findFirst()
                .orElseThrow();

        SchemaDto nameSchema = postEndpoint.requestBody().schema().properties().get("name");
        assertThat(nameSchema.minLength()).isEqualTo(2);
        assertThat(nameSchema.maxLength()).isEqualTo(100);
    }

    @Test
    void shouldExtractEnumValues() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto postEndpoint = contract.endpoints().stream()
                .filter(e -> "POST".equals(e.method()))
                .findFirst()
                .orElseThrow();

        SchemaDto roleSchema = postEndpoint.requestBody().schema().properties().get("role");
        assertThat(roleSchema.enumValues())
                .containsExactlyInAnyOrder("user", "admin", "moderator");
    }

    @Test
    void shouldExtractRequiredFields() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto postEndpoint = contract.endpoints().stream()
                .filter(e -> "POST".equals(e.method()))
                .findFirst()
                .orElseThrow();

        List<String> required = postEndpoint.requestBody().schema().required();
        assertThat(required).containsExactlyInAnyOrder("name", "email", "age");
    }

    @Test
    void shouldExtractPathParameters() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto getEndpoint = contract.endpoints().stream()
                .filter(e -> "GET".equals(e.method()))
                .findFirst()
                .orElseThrow();

        assertThat(getEndpoint.parameters()).isNotEmpty();
        ParameterDto idParam = getEndpoint.parameters().get(0);
        assertThat(idParam.name()).isEqualTo("id");
        assertThat(idParam.in()).isEqualTo("path");
        assertThat(idParam.required()).isTrue();
    }

    @Test
    void shouldExtractResponseStatusCodes() {
        ApiContractDto contract = parserService.parse(SAMPLE_YAML);

        EndpointDto postEndpoint = contract.endpoints().stream()
                .filter(e -> "POST".equals(e.method()))
                .findFirst()
                .orElseThrow();

        assertThat(postEndpoint.responses()).containsKey("201");
    }

    @Test
    void shouldDetectFormatAsYaml() {
        String format = parserService.detectFormat(SAMPLE_YAML);
        assertThat(format).isEqualTo("YAML");
    }

    @Test
    void shouldDetectFormatAsJson() {
        String jsonContent = "{\"openapi\": \"3.0.3\"}";
        String format = parserService.detectFormat(jsonContent);
        assertThat(format).isEqualTo("JSON");
    }

    @Test
    void shouldThrowExceptionForBlankContent() {
        assertThatThrownBy(() -> parserService.parse(""))
                .isInstanceOf(OpenApiParseException.class);
    }

    @Test
    void shouldThrowExceptionForNullContent() {
        assertThatThrownBy(() -> parserService.parse(null))
                .isInstanceOf(OpenApiParseException.class);
    }

    @Test
    void shouldThrowExceptionForInvalidContent() {
        assertThatThrownBy(() -> parserService.parse("not: valid: openapi"))
                .isInstanceOf(OpenApiParseException.class);
    }
}
