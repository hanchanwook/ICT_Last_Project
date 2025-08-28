package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.api.gemjjok.entity.StudentSubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSubmissionFileViewRepository extends JpaRepository<StudentSubmissionFile, Long> {

    /**
     * 특정 과제와 학생의 제출 파일 목록 조회
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId AND s.isActive = 1 ORDER BY s.createdAt DESC")
    List<StudentSubmissionFile> findByAssignmentIdAndStudentId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);

    /**
     * 특정 과제의 모든 제출 파일 목록 조회 (관리자용)
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.isActive = 1 ORDER BY s.createdAt DESC")
    List<StudentSubmissionFile> findByAssignmentId(@Param("assignmentId") String assignmentId);

    /**
     * 특정 학생의 모든 제출 파일 목록 조회
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.studentId = :studentId AND s.isActive = 1 ORDER BY s.createdAt DESC")
    List<StudentSubmissionFile> findByStudentId(@Param("studentId") String studentId);

    /**
     * 특정 파일 ID로 제출 파일 조회
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.id = :fileId AND s.isActive = 1")
    Optional<StudentSubmissionFile> findByIdAndActive(@Param("fileId") Long fileId);

    /**
     * 특정 과제와 학생의 제출 파일 개수 조회
     */
    @Query("SELECT COUNT(s) FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId AND s.isActive = 1")
    Long countByAssignmentIdAndStudentId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);

    /**
     * 특정 과제의 총 파일 크기 조회
     */
    @Query("SELECT COALESCE(SUM(s.fileSize), 0) FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId AND s.isActive = 1")
    Long getTotalFileSizeByAssignmentIdAndStudentId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);

    /**
     * 특정 과제의 파일 타입별 개수 조회
     */
    @Query("SELECT s.contentType, COUNT(s) FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId AND s.isActive = 1 GROUP BY s.contentType")
    List<Object[]> getFileTypeCountByAssignmentIdAndStudentId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);

    /**
     * 특정 과제의 최근 제출 파일 조회 (최신 5개)
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId AND s.isActive = 1 ORDER BY s.createdAt DESC LIMIT 5")
    List<StudentSubmissionFile> findRecentFilesByAssignmentIdAndStudentId(@Param("assignmentId") String assignmentId, @Param("studentId") String studentId);

    /**
     * 파일 키로 제출 파일 조회
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.fileKey = :fileKey AND s.isActive = 1")
    Optional<StudentSubmissionFile> findByFileKey(@Param("fileKey") String fileKey);

    /**
     * 특정 과제와 학생의 특정 파일명으로 조회
     */
    @Query("SELECT s FROM StudentSubmissionFile s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId AND s.fileName = :fileName AND s.isActive = 1")
    Optional<StudentSubmissionFile> findByAssignmentIdAndStudentIdAndFileName(
        @Param("assignmentId") String assignmentId, 
        @Param("studentId") String studentId, 
        @Param("fileName") String fileName
    );
} 