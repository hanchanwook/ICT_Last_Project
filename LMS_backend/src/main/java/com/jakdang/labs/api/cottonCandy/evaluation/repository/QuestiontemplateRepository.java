package com.jakdang.labs.api.cottonCandy.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.QuestionTemplateEntity;

@Repository
public interface QuestiontemplateRepository extends JpaRepository<QuestionTemplateEntity, String> {
    
    // 템플릿 등록시 템플릿 번호 최대값 조회 =
    @Query("SELECT MAX(q.questionTemplateNum) FROM QuestionTemplateEntity q WHERE q.educationId = ?1")
    Integer findMaxTemplateNum(String educationId);
    
    // 템플릿 목록 조회 educationId로 템플릿 목록 조회 =
    List<QuestionTemplateEntity> findAllByEducationId(String educationId);
    
    // evalQuestionId로 QuestionTemplate 조회 =
    List<QuestionTemplateEntity> findByEvalQuestionId(String evalQuestionId);

    // 평가 질문 사용 횟수 조회 =
    @Query("SELECT COUNT(q) FROM QuestionTemplateEntity q WHERE q.evalQuestionId = :evalQuestionId")
    int getUseEvalQuestion(String evalQuestionId);
    
    // questionTemplateNum으로 템플릿 조회 =
    List<QuestionTemplateEntity> findByQuestionTemplateNum(int questionTemplateNum);
}
