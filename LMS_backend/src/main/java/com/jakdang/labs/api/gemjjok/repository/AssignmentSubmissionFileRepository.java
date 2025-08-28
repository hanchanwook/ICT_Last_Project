package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.AssignmentSubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionFileRepository extends JpaRepository<AssignmentSubmissionFile, String> {

    // 과제 ID와 학생 ID로 제출 파일 목록 조회
    @Query("SELECT f FROM AssignmentSubmissionFile f WHERE f.assignmentId = :assignmentId AND f.studentId = :studentId AND f.isActive = true ORDER BY f.createdAt DESC")
    List<AssignmentSubmissionFile> findByAssignmentIdAndStudentId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);

    // 제출 ID로 파일 목록 조회
    @Query("SELECT f FROM AssignmentSubmissionFile f WHERE f.submissionId = :submissionId AND f.isActive = true ORDER BY f.createdAt DESC")
    List<AssignmentSubmissionFile> findBySubmissionId(@Param("submissionId") String submissionId);

    // 파일 키로 파일 조회
    Optional<AssignmentSubmissionFile> findByFileKey(String fileKey);

    // 외부 파일 ID로 파일 조회
    Optional<AssignmentSubmissionFile> findById(String externalFileId);
    
    // 과제 ID와 학생 ID로 submissionId가 null인 파일 목록 조회
    @Query("SELECT f FROM AssignmentSubmissionFile f WHERE f.assignmentId = :assignmentId AND f.studentId = :studentId AND f.submissionId IS NULL AND f.isActive = true ORDER BY f.createdAt DESC")
    List<AssignmentSubmissionFile> findByAssignmentIdAndStudentIdAndSubmissionIdIsNull(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);
    
    // assignmentId, studentId, submissionId로 파일 목록 조회
    @Query("SELECT f FROM AssignmentSubmissionFile f WHERE f.assignmentId = :assignmentId AND f.studentId = :studentId AND f.submissionId = :submissionId AND f.isActive = true ORDER BY f.createdAt DESC")
    List<AssignmentSubmissionFile> findByAssignmentIdAndStudentIdAndSubmissionId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId, @Param("submissionId") String submissionId);
    
    // 과제 ID로 모든 학생의 제출 파일 목록 조회 (강사용)
    @Query("SELECT f FROM AssignmentSubmissionFile f WHERE f.assignmentId = :assignmentId AND f.isActive = true ORDER BY f.studentId, f.createdAt DESC")
    List<AssignmentSubmissionFile> findByAssignmentId(@Param("assignmentId") String assignmentId);
} 