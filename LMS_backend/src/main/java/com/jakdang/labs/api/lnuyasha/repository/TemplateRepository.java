package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, String> {
    
    /**
     * 강사별 시험 템플릿 목록 조회 (모든 상태)
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.memberId = :memberId")
    List<TemplateEntity> findByMemberId(@Param("memberId") String memberId);
    
    /**
     * 강사별 시험 템플릿 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.memberId = :memberId AND t.templateActive = 0")
    List<TemplateEntity> findByMemberIdAndActive(@Param("memberId") String memberId);
    
    /**
     * 교육기관별 시험 템플릿 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.educationId = :educationId AND t.templateActive = 0")
    List<TemplateEntity> findByEducationIdAndActive(@Param("educationId") String educationId);
    
    /**
     * 강사별 시험 템플릿 목록 조회 (교육기관 필터링 포함)
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.memberId = :memberId AND t.educationId = :educationId AND t.templateActive = 0")
    List<TemplateEntity> findByMemberIdAndEducationIdAndActive(@Param("memberId") String memberId, @Param("educationId") String educationId);
    
    /**
     * 특정 소그룹의 활성화된 시험 템플릿 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.subGroupId IN :subGroupIds AND t.templateActive = :templateActive")
    List<TemplateEntity> findBySubGroupIdInAndTemplateActive(@Param("subGroupIds") List<String> subGroupIds, @Param("templateActive") int templateActive);
    
    /**
     * 단일 소그룹의 활성화된 시험 템플릿 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.subGroupId = :subGroupId AND t.templateActive = :templateActive")
    List<TemplateEntity> findBySubGroupIdAndTemplateActive(@Param("subGroupId") String subGroupId, @Param("templateActive") int templateActive);
    
    /**
     * 시험 템플릿 ID로 조회
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.templateId = :templateId")
    java.util.Optional<TemplateEntity> findByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 종료 시간이 된 활성화된 시험 템플릿 목록 조회 (자동 종료 스케줄러용)
     */
    @Query("SELECT t FROM TemplateEntity t WHERE t.templateActive = :templateActive AND t.templateClose <= :now")
    List<TemplateEntity> findByTemplateActiveAndTemplateCloseBefore(@Param("templateActive") int templateActive, @Param("now") LocalDateTime now);
} 