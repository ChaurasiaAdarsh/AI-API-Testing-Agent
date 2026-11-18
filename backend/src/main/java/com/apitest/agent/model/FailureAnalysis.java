package com.apitest.agent.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "failure_analyses")
@Data
@NoArgsConstructor
public class FailureAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "likely_cause", columnDefinition = "TEXT")
    private String likelyCause;

    @Column(length = 20)
    private String severity;

    @Column(name = "contract_violation")
    private Boolean contractViolation = false;

    @Column(name = "is_expected_failure")
    private Boolean isExpectedFailure = false;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @OneToOne
    @JoinColumn(name = "test_execution_id")
    private TestExecution testExecution;

    @PrePersist
    void prePersist() {
        analyzedAt = LocalDateTime.now();
    }
}
