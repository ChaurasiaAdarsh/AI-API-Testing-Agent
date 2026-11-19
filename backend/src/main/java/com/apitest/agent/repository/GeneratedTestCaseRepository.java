package com.apitest.agent.repository;

import com.apitest.agent.model.GeneratedTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeneratedTestCaseRepository extends JpaRepository<GeneratedTestCase, Long> {
    List<GeneratedTestCase> findByTestRunId(Long testRunId);
    long countByTestRunId(Long testRunId);
}
