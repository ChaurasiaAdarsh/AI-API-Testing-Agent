package com.apitest.agent.repository;

import com.apitest.agent.model.TestExecution;
import com.apitest.agent.model.TestExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findByTestRunId(Long testRunId);
    List<TestExecution> findByTestRunIdAndContractViolationTrue(Long testRunId);
    List<TestExecution> findByTestRunIdAndStatusNot(Long testRunId, TestExecutionStatus status);
    long countByTestRunIdAndStatus(Long testRunId, TestExecutionStatus status);
}
