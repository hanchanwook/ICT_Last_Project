package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentEntity, String> {
    
    // 강사가 담당하는 모든 과제 조회
    List<AssignmentEntity> findByMemberIdOrderByCreatedAtDesc(String memberId);
    
    // 강사가 담당하는 활성 과제만 조회
    List<AssignmentEntity> findByMemberIdAndAssignmentActiveOrderByCreatedAtDesc(String memberId, Integer assignmentActive);
    
    // 특정 과정의 과제 조회
    List<AssignmentEntity> findByCourseIdOrderByCreatedAtDesc(String courseId);
    
    // 특정 과정의 활성 과제만 조회
    List<AssignmentEntity> findByCourseIdAndAssignmentActiveOrderByCreatedAtDesc(String courseId, Integer assignmentActive);
    
    // 과제 ID로 조회 (삭제되지 않은 것만)
    Optional<AssignmentEntity> findByAssignmentId(String assignmentId);
    
    // 과제 ID로 조회 (모든 상태)
    Optional<AssignmentEntity> findByAssignmentIdIgnoreCase(String assignmentId);
    
    // 제목으로 검색
    List<AssignmentEntity> findByAssignmentTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
    
    // 강사별 과제 통계 조회
    @Query("SELECT COUNT(a) FROM AssignmentEntity a WHERE a.memberId = :memberId")
    Long countByMemberId(@Param("memberId") String memberId);
    
    @Query("SELECT COUNT(a) FROM AssignmentEntity a WHERE a.memberId = :memberId AND a.assignmentActive = :assignmentActive")
    Long countByMemberIdAndAssignmentActive(@Param("memberId") String memberId, @Param("assignmentActive") Integer assignmentActive);

    List<AssignmentEntity> findByCourseIdInOrderByCreatedAtDesc(List<String> courseIds);

    // 진행중(마감일이 미래, 활성) 과제
    @Query("SELECT a FROM AssignmentEntity a WHERE a.memberId = :memberId AND a.assignmentActive = 0 AND a.dueDate > CURRENT_TIMESTAMP")
    List<AssignmentEntity> findActiveAssignmentsByMemberId(@Param("memberId") String memberId);

    // 마감됨(마감일이 과거 또는 비활성) 과제
    @Query("SELECT a FROM AssignmentEntity a WHERE a.memberId = :memberId AND (a.dueDate <= CURRENT_TIMESTAMP OR a.assignmentActive = 1)")
    List<AssignmentEntity> findCompletedAssignmentsByMemberId(@Param("memberId") String memberId);
} 