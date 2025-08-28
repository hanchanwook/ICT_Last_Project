package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.api.gemjjok.entity.StudentSubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSubmissionFileRepository extends JpaRepository<StudentSubmissionFile, Long> {
    
    List<StudentSubmissionFile> findByAssignmentIdAndStudentIdAndIsActive(String assignmentId, String studentId, Integer isActive);
    
    List<StudentSubmissionFile> findByAssignmentIdAndIsActive(String assignmentId, Integer isActive);
    
    List<StudentSubmissionFile> findByStudentIdAndIsActive(String studentId, Integer isActive);
    
    StudentSubmissionFile findByFileKeyAndIsActive(String fileKey, Integer isActive);
    
    StudentSubmissionFile findByMaterialIdAndIsActive(String materialId, Integer isActive);
} 