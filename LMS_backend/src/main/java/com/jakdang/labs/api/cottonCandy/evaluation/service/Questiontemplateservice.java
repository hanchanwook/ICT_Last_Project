package com.jakdang.labs.api.cottonCandy.evaluation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestQuestiontemplateDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseQuestiontemplateDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.QuestiontemplateRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.EvaluationQuestionRepository;
import com.jakdang.labs.entity.EvaluationQuestionEntity;
import com.jakdang.labs.entity.QuestionTemplateEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Questiontemplateservice {
    private final QuestiontemplateRepository questiontemplateRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;

    // 과정 평가 템플릿 생성 =
    @Transactional
    public ResponseQuestiontemplateDTO createQuestiontemplate(RequestQuestiontemplateDTO dto, String educationId) {
        // 1. 템플릿 번호 자동 증가 (가장 큰 번호 + 1)
        Integer maxNum = questiontemplateRepository.findMaxTemplateNum(educationId);
        int newTemplateNum = (maxNum == null ? 1 : maxNum + 1);


        // 3. 항목 리스트 저장
        List<QuestionTemplateEntity> savedEntities = new ArrayList<>();
        
        for (int i = 0; i < dto.getQuestionList().size(); i++) {
            RequestQuestiontemplateDTO.QuestionItem item = dto.getQuestionList().get(i);
            
            QuestionTemplateEntity entity = QuestionTemplateEntity.builder()
                .questionTemplateName(dto.getQuestionTemplateName())
                .questionTemplateNum(newTemplateNum)
                .questionNum(item.getQuestionNum())
                .evalQuestionId(item.getEvalQuestionId())
                .templateActive(0)
                .educationId(educationId)
                .build();
            
            QuestionTemplateEntity savedEntity = questiontemplateRepository.save(entity);
            savedEntities.add(savedEntity);
        }

        // 4. ResponseQuestiontemplateDTO 생성 및 반환
        List<ResponseQuestiontemplateDTO.QuestionItem> questionList = savedEntities.stream()
            .map(entity -> {
                // 평가 질문 정보 조회
                EvaluationQuestionEntity eval = evaluationQuestionRepository
                    .findById(entity.getEvalQuestionId()).orElse(null);
                
                return ResponseQuestiontemplateDTO.QuestionItem.builder()
                    .questionNum(entity.getQuestionNum())
                    .evalQuestionId(entity.getEvalQuestionId())
                    .evalQuestionText(eval != null ? eval.getEvalQuestionText() : null)
                    .evalQuestionType(eval != null ? eval.getEvalQuestionType() : 0)
                    .createdAt(eval != null && eval.getCreatedAt() != null ? 
                             eval.getCreatedAt().toString() : null)
                    .build();
            })
            .collect(Collectors.toList());

        return ResponseQuestiontemplateDTO.builder()
            .questionTemplateName(dto.getQuestionTemplateName())
            .questionTemplateNum(newTemplateNum)
            .createdAt(savedEntities.get(0).getCreatedAt() != null ? 
                     savedEntities.get(0).getCreatedAt().toString() : null)
            .questionList(questionList)
            .build();
    }


    // 템플릿 리스트 조회 (템플릿별로 그룹화) =
    public List<ResponseQuestiontemplateDTO> getQuestiontemplateList(String educationId) {
        // 1. 활성화된 템플릿 데이터만 조회 (templateActive = 0)
        List<QuestionTemplateEntity> allTemplates = questiontemplateRepository.findAllByEducationId(educationId)
            .stream()
            .filter(template -> template.getTemplateActive() == 0)
            .collect(Collectors.toList());
        
        // 2. 템플릿별로 그룹화 (questionTemplateNum으로 그룹화)
        Map<Integer, List<QuestionTemplateEntity>> groupedTemplates = allTemplates.stream()
            .collect(Collectors.groupingBy(QuestionTemplateEntity::getQuestionTemplateNum));
        
        // 3. 각 그룹을 ResponseQuestiontemplateDTO로 변환
        List<ResponseQuestiontemplateDTO> result = groupedTemplates.entrySet().stream()
            .map(entry -> {
                Integer templateNum = entry.getKey();
                List<QuestionTemplateEntity> templateGroup = entry.getValue();
                QuestionTemplateEntity firstEntity = templateGroup.get(0);
                
                // 4. 각 템플릿 그룹의 항목들을 QuestionItem으로 변환
                List<ResponseQuestiontemplateDTO.QuestionItem> questionList = templateGroup.stream()
                    .map(entity -> {
                        // 평가 질문 정보 조회
                        EvaluationQuestionEntity eval = evaluationQuestionRepository
                            .findById(entity.getEvalQuestionId()).orElse(null);
                        
                        return ResponseQuestiontemplateDTO.QuestionItem.builder()
                            .questionNum(entity.getQuestionNum())
                            .evalQuestionId(entity.getEvalQuestionId())
                            .evalQuestionText(eval != null ? eval.getEvalQuestionText() : null)
                            .evalQuestionType(eval != null ? eval.getEvalQuestionType() : 0)
                            .createdAt(eval != null && eval.getCreatedAt() != null ? 
                                     eval.getCreatedAt().toString() : null)
                            .build();
                    })
                    .sorted((a, b) -> Integer.compare(a.getQuestionNum(), b.getQuestionNum())) // questionNum으로 정렬
                    .collect(Collectors.toList());
                
                // 5. ResponseQuestiontemplateDTO 생성
                return ResponseQuestiontemplateDTO.builder()
                    .questionTemplateName(firstEntity.getQuestionTemplateName())
                    .questionTemplateNum(templateNum)
                    .createdAt(firstEntity.getCreatedAt() != null ? 
                             firstEntity.getCreatedAt().toString() : null)
                    .questionList(questionList)
                    .build();
            })
            .sorted((a, b) -> {
                // templateNum으로 정렬, 같으면 생성일로 정렬
                int numCompare = Integer.compare(a.getQuestionTemplateNum(), b.getQuestionTemplateNum());
                if (numCompare != 0) return numCompare;
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            })
            .collect(Collectors.toList());
        
        return result;
    }

    // 템플릿 수정 (기존 템플릿 비활성화 후 새로 생성) =
    @Transactional
    public ResponseQuestiontemplateDTO updateQuestiontemplate(RequestQuestiontemplateDTO dto, String educationId) {
        // 1. 기존 템플릿 조회 (templateNum으로만 검색)
        List<QuestionTemplateEntity> existingTemplates = questiontemplateRepository.findAllByEducationId(educationId)
            .stream()
            .filter(template -> template.getQuestionTemplateNum() == dto.getQuestionTemplateNum() && 
                              template.getTemplateActive() == 0)
            .collect(Collectors.toList());
        
        if (existingTemplates.isEmpty()) {
            throw new RuntimeException("수정할 템플릿을 찾을 수 없습니다.");
        }
        
        // 2. 기존 템플릿 항목들을 비활성화 (templateActive = 1)
        for (QuestionTemplateEntity existingTemplate : existingTemplates) {
            existingTemplate.setTemplateActive(1);
            questiontemplateRepository.save(existingTemplate);
        }
        
        // 3. 새로운 템플릿 생성 (기존 createQuestiontemplate 메서드 호출)
        ResponseQuestiontemplateDTO newTemplate = createQuestiontemplate(dto, educationId);
        
        return newTemplate;
    }

    // 템플릿 삭제 (논리적 삭제 - 비활성화)
    @Transactional
    public void deleteQuestiontemplate(String educationId, int questionTemplateNum) {
        // 해당 templateNum의 모든 활성 템플릿을 비활성화
        List<QuestionTemplateEntity> templatesToDelete = questiontemplateRepository.findAllByEducationId(educationId)
            .stream()
            .filter(template -> template.getQuestionTemplateNum() == questionTemplateNum && 
                              template.getTemplateActive() == 0)
            .collect(Collectors.toList());
        
        if (templatesToDelete.isEmpty()) {
            throw new RuntimeException("삭제할 템플릿을 찾을 수 없습니다.");
        }
        
        for (QuestionTemplateEntity template : templatesToDelete) {
            template.setTemplateActive(1); // 비활성화
            questiontemplateRepository.save(template);
        }
    }
}
