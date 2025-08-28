package com.jakdang.labs.api.cottonCandy.evaluation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.EvaluationQuestionEntity;

@Repository
public interface EvaluationQuestionRepository extends JpaRepository<EvaluationQuestionEntity, String> {
    
    // educationId와 evalQuestionActive로 평가 질문 조회
    List<EvaluationQuestionEntity> findByEducationIdAndEvalQuestionActive(String educationId, int evalQuestionActive);
    
    // evalQuestionId로 평가 질문 조회 =
    Optional<EvaluationQuestionEntity> findById(String evalQuestionId);
}
