package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.TemplateQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 템플릿 문제 정보 조회를 위한 Repository
 */
@Repository
public interface TemplateQuestionRepository extends JpaRepository<TemplateQuestionEntity, String> {
    
    /**
     * 템플릿 문제 ID로 조회
     * @param templateQuestionId 템플릿 문제 ID
     * @return 템플릿 문제 정보
     */
    Optional<TemplateQuestionEntity> findByTemplateQuestionId(String templateQuestionId);
    
    /**
     * 템플릿 ID로 문제 목록 조회
     * @param templateId 템플릿 ID
     * @return 해당 템플릿의 문제 목록
     */
    List<TemplateQuestionEntity> findByTemplateId(String templateId);
} 
 