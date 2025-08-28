package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.ScoreStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 학생 시험 점수 정보 조회를 위한 Repository
 */
@Repository
public interface ScoreStudentRepository extends JpaRepository<ScoreStudentEntity, String> {
    
    /**
     * 특정 시험의 모든 학생 점수 조회
     * @param templateId 시험 템플릿 ID
     * @return 해당 시험의 모든 학생 점수 목록
     */
    List<ScoreStudentEntity> findByTemplateId(String templateId);
    
    /**
     * 특정 학생의 모든 시험 점수 조회
     * @param memberId 학생 ID
     * @return 해당 학생의 모든 시험 점수 목록
     */
    List<ScoreStudentEntity> findByMemberId(String memberId);
    
    /**
     * 특정 시험의 특정 학생 점수 조회
     * @param templateId 시험 템플릿 ID
     * @param memberId 학생 ID
     * @return 해당 시험의 해당 학생 점수
     */
    ScoreStudentEntity findByTemplateIdAndMemberId(String templateId, String memberId);
    
    /**
     * 특정 시험의 평균 점수 조회
     * @param templateId 시험 템플릿 ID
     * @return 평균 점수
     */
    @Query("SELECT AVG(s.score) FROM ScoreStudentEntity s WHERE s.templateId = :templateId")
    Double findAverageScoreByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 특정 시험의 최고 점수 조회
     * @param templateId 시험 템플릿 ID
     * @return 최고 점수
     */
    @Query("SELECT MAX(s.score) FROM ScoreStudentEntity s WHERE s.templateId = :templateId")
    Integer findMaxScoreByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 특정 시험의 최저 점수 조회
     * @param templateId 시험 템플릿 ID
     * @return 최저 점수
     */
    @Query("SELECT MIN(s.score) FROM ScoreStudentEntity s WHERE s.templateId = :templateId")
    Integer findMinScoreByTemplateId(@Param("templateId") String templateId);
} 