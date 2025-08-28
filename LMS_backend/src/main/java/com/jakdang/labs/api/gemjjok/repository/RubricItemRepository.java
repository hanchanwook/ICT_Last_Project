package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.RubricItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RubricItemRepository extends JpaRepository<RubricItemEntity, String> {
    
    // 과제 ID로 모든 아이템 조회 (순서대로)
    List<RubricItemEntity> findByAssignmentIdOrderByItemOrderAsc(String assignmentId);
    
    // 과제 ID로 아이템 개수 조회
    long countByAssignmentId(String assignmentId);
    
    // 과제 ID로 모든 아이템 삭제
    void deleteByAssignmentId(String assignmentId);
    
    // 과제 ID로 루브릭 존재 여부 확인
    boolean existsByAssignmentId(String assignmentId);
} 