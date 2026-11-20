package com.apitest.agent.service;

import com.apitest.agent.dto.*;
import com.apitest.agent.exception.InvalidSpecificationException;
import com.apitest.agent.exception.ProjectNotFoundException;
import com.apitest.agent.model.*;
import com.apitest.agent.parser.OpenApiParserService;
import com.apitest.agent.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApiSpecificationRepository specRepository;
    private final OpenApiParserService parserService;

    public ProjectService(ProjectRepository projectRepository,
                          ApiSpecificationRepository specRepository,
                          OpenApiParserService parserService) {
        this.projectRepository = projectRepository;
        this.specRepository = specRepository;
        this.parserService = parserService;
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setTargetBaseUrl(request.targetBaseUrl());
        project = projectRepository.save(project);
        log.info("Created project: {} (id={})", project.getName(), project.getId());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id) {
        return toResponse(findProject(id));
    }

    public SpecificationResponse uploadSpecification(Long projectId, SpecificationUploadRequest request) {
        Project project = findProject(projectId);

        ApiContractDto contract = parserService.parse(request.content());
        String format = parserService.detectFormat(request.content());

        ApiSpecification spec = new ApiSpecification();
        spec.setTitle(contract.title());
        spec.setVersion(contract.version());
        spec.setFormat(format);
        spec.setRawContent(request.content());
        spec.setProject(project);
        spec = specRepository.save(spec);

        log.info("Uploaded specification for project {}: {} v{}", projectId, spec.getTitle(), spec.getVersion());
        return toSpecResponse(spec);
    }

    @Transactional(readOnly = true)
    public SpecificationResponse getSpecification(Long projectId) {
        findProject(projectId);
        return specRepository.findTopByProjectIdOrderByUploadedAtDesc(projectId)
                .map(this::toSpecResponse)
                .orElseThrow(() -> new InvalidSpecificationException("No specification found for project " + projectId));
    }

    public Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with id: " + id));
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(), p.getTargetBaseUrl(),
                p.getSpecifications() != null ? p.getSpecifications().size() : 0,
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    private SpecificationResponse toSpecResponse(ApiSpecification s) {
        return new SpecificationResponse(s.getId(), s.getTitle(), s.getVersion(), s.getFormat(),
                s.getUploadedAt(), s.getProject().getId());
    }
}
