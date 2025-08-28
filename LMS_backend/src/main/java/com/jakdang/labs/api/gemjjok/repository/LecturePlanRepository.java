package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.LecturePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturePlanRepository extends JpaRepository<LecturePlanEntity, String> {
    
    // 활성 상태인 강의 계획서 목록 조회
    List<LecturePlanEntity> findByPlanActiveOrderByCreatedAtDesc(Integer planActive);
    
    // 특정 과정의 강의 계획서 목록 조회
    List<LecturePlanEntity> findByCourseIdAndPlanActiveOrderByCreatedAtDesc(String courseId, Integer planActive);
    
    // 강의 계획서 ID로 조회 (삭제되지 않은 것만)
    Optional<LecturePlanEntity> findByPlanIdAndPlanActive(String planId, Integer planActive);
    
    // 강의 계획서 ID와 활성 상태로 존재 여부 확인
    boolean existsByPlanIdAndPlanActive(String planId, Integer planActive);
    
    // 과정 ID와 제목으로 중복 확인
    boolean existsByCourseIdAndPlanTitleAndPlanActive(String courseId, String planTitle, Integer planActive);
    
    // 강의 계획서 제목으로 검색
    @Query("SELECT l FROM LecturePlanEntity l WHERE l.planTitle LIKE %:keyword% AND l.planActive = :planActive")
    List<LecturePlanEntity> findByPlanTitleContainingAndPlanActive(@Param("keyword") String keyword, @Param("planActive") Integer planActive);
    
    // 과정 ID로 강의 계획서 개수 조회
    long countByCourseIdAndPlanActive(String courseId, Integer planActive);
} 