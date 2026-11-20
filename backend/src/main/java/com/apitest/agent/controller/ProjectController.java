package com.apitest.agent.controller;

import com.apitest.agent.dto.*;
import com.apitest.agent.service.ProjectService;
import com.apitest.agent.service.TestRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Project management")
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final TestRunService testRunService;

    public ProjectController(ProjectService projectService, TestRunService testRunService) {
        this.projectService = projectService;
        this.testRunService = testRunService;
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @GetMapping
    @Operation(summary = "List all projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @PostMapping("/{id}/specifications")
    @Operation(summary = "Upload an OpenAPI specification")
    public ResponseEntity<SpecificationResponse> uploadSpecification(
            @PathVariable Long id,
            @Valid @RequestBody SpecificationUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.uploadSpecification(id, request));
    }

    @GetMapping("/{id}/specifications")
    @Operation(summary = "Get the latest specification for a project")
    public ResponseEntity<SpecificationResponse> getSpecification(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getSpecification(id));
    }

    @PostMapping("/{id}/generate-tests")
    @Operation(summary = "Generate AI test cases for a project")
    public ResponseEntity<TestRunResponse> generateTests(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) GenerateTestsRequest request) {
        if (request == null) request = new GenerateTestsRequest(null, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(testRunService.generateTests(id, request));
    }

    @PostMapping("/{id}/analyze")
    @Operation(summary = "Analyze the OpenAPI specification")
    public ResponseEntity<SpecificationResponse> analyzeSpecification(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getSpecification(id));
    }
}
