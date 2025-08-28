package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.WeeklyPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlanEntity, String> {
    
    // planId로 주차별 계획 목록 조회 (주차 순으로 정렬)
    List<WeeklyPlanEntity> findByPlanIdOrderByWeekNumberAsc(String planId);
    
    // planId로 주차별 계획 삭제
    @Modifying
    @Query("DELETE FROM WeeklyPlanEntity w WHERE w.planId = :planId")
    void deleteByPlanId(@Param("planId") String planId);
    
    // planId와 weekNumber로 중복 확인
    boolean existsByPlanIdAndWeekNumber(String planId, Integer weekNumber);
    
    // planId로 주차별 계획 개수 조회
    long countByPlanId(String planId);
} 