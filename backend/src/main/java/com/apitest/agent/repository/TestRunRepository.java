package com.apitest.agent.repository;

import com.apitest.agent.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
