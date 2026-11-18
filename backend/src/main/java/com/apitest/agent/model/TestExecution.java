package com.apitest.agent.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions")
@Data
@NoArgsConstructor
public class TestExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name")
    private String testName;

    @Column(length = 10)
    private String method;

    private String endpoint;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "expected_status")
    private Integer expectedStatus;

    @Column(name = "actual_status")
    private Integer actualStatus;

    @Column(name = "actual_response_body", columnDefinition = "TEXT")
    private String actualResponseBody;

    @Column(name = "actual_response_headers", columnDefinition = "TEXT")
    private String actualResponseHeaders;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Enumerated(EnumType.STRING)
    private TestExecutionStatus status = TestExecutionStatus.PENDING;

    @Column(name = "contract_violation")
    private Boolean contractViolation = false;

    @Column(name = "contract_violation_details", columnDefinition = "TEXT")
    private String contractViolationDetails;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "test_case_id")
    private GeneratedTestCase testCase;

    @OneToOne(mappedBy = "testExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FailureAnalysis failureAnalysis;
}
