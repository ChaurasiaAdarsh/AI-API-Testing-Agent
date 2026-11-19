package com.apitest.agent.repository;

import com.apitest.agent.model.ApiSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApiSpecificationRepository extends JpaRepository<ApiSpecification, Long> {
    Optional<ApiSpecification> findTopByProjectIdOrderByUploadedAtDesc(Long projectId);
}
