package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.AssignmentSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmissionEntity, String> {
    Optional<AssignmentSubmissionEntity> findByAssignmentIdAndId(String assignmentId, String id);
    List<AssignmentSubmissionEntity> findByIdIn(List<String> ids);
} 