package com.jakdang.labs.api.cottonCandy.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.entity.EvaluationResponseEntity;

public interface StudentEvaluationRepository extends JpaRepository<EvaluationResponseEntity, String> {
    
    // templateGroupId, evalQuestionId, memberId로 답변 조회
    List<EvaluationResponseEntity> findByTemplateGroupIdAndEvalQuestionIdAndMemberId(String templateGroupId, String evalQuestionId, String memberId);
    
    // templateGroupId와 memberId로 답변 조회 (모든 답변)
    List<EvaluationResponseEntity> findByTemplateGroupIdAndMemberId(String templateGroupId, String memberId);
    
    // templateGroupId로 모든 답변 조회
    List<EvaluationResponseEntity> findByTemplateGroupId(String templateGroupId);
}
