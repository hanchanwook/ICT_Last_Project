package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.api.gemjjok.entity.AssignmentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentFileRepository extends JpaRepository<AssignmentFile, Long> {
    
    /**
     * 과제 ID와 활성 상태로 자료 목록 조회
     */
    List<AssignmentFile> findByAssignmentIdAndIsActive(String assignmentId, Integer isActive);
    
    /**
     * materialId로 자료 조회
     */
    Optional<AssignmentFile> findByMaterialId(String materialId);
    
    /**
     * fileKey로 자료 조회
     */
    Optional<AssignmentFile> findByFileKey(String fileKey);
    
    /**
     * 과제 ID로 모든 자료 조회 (활성 상태 무관)
     */
    List<AssignmentFile> findByAssignmentId(String assignmentId);
    
    /**
     * 업로더 ID로 자료 목록 조회
     */
    List<AssignmentFile> findByUploadedBy(String uploadedBy);
    
    /**
     * 활성 상태로 자료 목록 조회
     */
    List<AssignmentFile> findByIsActive(Integer isActive);
} 