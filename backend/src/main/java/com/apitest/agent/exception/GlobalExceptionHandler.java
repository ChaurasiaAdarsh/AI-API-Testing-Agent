package com.apitest.agent.exception;

import com.apitest.agent.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenApiParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleOpenApiParse(OpenApiParseException ex, HttpServletRequest req) {
        log.error("OpenAPI parse error: {}", ex.getMessage());
        return new ErrorResponse(LocalDateTime.now(), 400, "INVALID_OPENAPI", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleProjectNotFound(ProjectNotFoundException ex, HttpServletRequest req) {
        return new ErrorResponse(LocalDateTime.now(), 404, "PROJECT_NOT_FOUND", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(TestRunNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTestRunNotFound(TestRunNotFoundException ex, HttpServletRequest req) {
        return new ErrorResponse(LocalDateTime.now(), 404, "TEST_RUN_NOT_FOUND", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(SsrfViolationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleSsrf(SsrfViolationException ex, HttpServletRequest req) {
        log.warn("SSRF attempt blocked: {}", ex.getMessage());
        return new ErrorResponse(LocalDateTime.now(), 403, "SSRF_VIOLATION", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(AiEngineException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleAiEngine(AiEngineException ex, HttpServletRequest req) {
        log.error("AI engine error: {}", ex.getMessage());
        return new ErrorResponse(LocalDateTime.now(), 503, "AI_ENGINE_ERROR", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(InvalidSpecificationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidSpec(InvalidSpecificationException ex, HttpServletRequest req) {
        return new ErrorResponse(LocalDateTime.now(), 400, "INVALID_SPECIFICATION", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return new ErrorResponse(LocalDateTime.now(), 400, "VALIDATION_ERROR", message, req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return new ErrorResponse(LocalDateTime.now(), 500, "INTERNAL_ERROR", "An unexpected error occurred", req.getRequestURI());
    }
}
