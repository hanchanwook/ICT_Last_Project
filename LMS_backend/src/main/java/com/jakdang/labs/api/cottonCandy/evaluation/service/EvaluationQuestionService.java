package com.jakdang.labs.api.cottonCandy.evaluation.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestEvaluationQuestionDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseEvaluationQuestionDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.EvaluationQuestionRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.QuestiontemplateRepository;
import com.jakdang.labs.entity.EvaluationQuestionEntity;
import com.jakdang.labs.entity.QuestionTemplateEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationQuestionService {
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final QuestiontemplateRepository questionTemplateRepository;
    
    // 평가 질문 리스트 조회 =
    public List<ResponseEvaluationQuestionDTO> getEvaluationList(String educationId) {
        List<EvaluationQuestionEntity> evaluationQuestionList = evaluationQuestionRepository.findByEducationIdAndEvalQuestionActive(educationId, 0);
        List<ResponseEvaluationQuestionDTO> responseEvaluationQuestionList = new ArrayList<>();
        
        
        for (EvaluationQuestionEntity evaluationQuestion : evaluationQuestionList) {
            ResponseEvaluationQuestionDTO responseEvaluationQuestion = ResponseEvaluationQuestionDTO.builder()
            .evalQuestionId(evaluationQuestion.getEvalQuestionId())
            .evalQuestionType(evaluationQuestion.getEvalQuestionType())
            .evalQuestionText(evaluationQuestion.getEvalQuestionText())
            .createdAt(evaluationQuestion.getCreatedAt())
            .build();
            responseEvaluationQuestion.setUseEvalQuestion(questionTemplateRepository.getUseEvalQuestion(evaluationQuestion.getEvalQuestionId()));
            
            // 템플릿이 존재하는 경우에만 templateCreatedAt 설정
            List<QuestionTemplateEntity> templates = questionTemplateRepository.findByEvalQuestionId(evaluationQuestion.getEvalQuestionId());
            if (!templates.isEmpty()) {
                responseEvaluationQuestion.setTemplateCreatedAt(templates.get(0).getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate());
            }
            
            responseEvaluationQuestionList.add(responseEvaluationQuestion);
        }
        return responseEvaluationQuestionList;
    }

    // 평가 질문 등록 =
    public ResponseEvaluationQuestionDTO createEvaluationQuestion(RequestEvaluationQuestionDTO dto, String educationId) {
        EvaluationQuestionEntity evaluationQuestion = EvaluationQuestionEntity.builder()
            .educationId(educationId) 
            .evalQuestionType(dto.getEvalQuestionType())
            .evalQuestionText(dto.getEvalQuestionText())
            .build();
        
        EvaluationQuestionEntity savedEvaluationQuestion = evaluationQuestionRepository.save(evaluationQuestion);
        
        return ResponseEvaluationQuestionDTO.builder()
            .evalQuestionId(savedEvaluationQuestion.getEvalQuestionId())
            .evalQuestionType(savedEvaluationQuestion.getEvalQuestionType())
            .evalQuestionText(savedEvaluationQuestion.getEvalQuestionText())
            .build();
    }

    // 평가 질문 삭제 =
    public void deleteEvaluationQuestion(String id) {
        EvaluationQuestionEntity existingEvaluationQuestion = evaluationQuestionRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("평가 질문을 찾을 수 없습니다. ID: " + id));
        
        existingEvaluationQuestion.setEvalQuestionActive(1);    
        evaluationQuestionRepository.save(existingEvaluationQuestion);
    }

 
}