package com.apitest.agent.repository;

import com.apitest.agent.model.FailureAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FailureAnalysisRepository extends JpaRepository<FailureAnalysis, Long> {
    Optional<FailureAnalysis> findByTestExecutionId(Long testExecutionId);
}
