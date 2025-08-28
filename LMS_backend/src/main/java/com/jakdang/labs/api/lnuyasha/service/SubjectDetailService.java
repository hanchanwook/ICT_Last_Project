package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.SubjectDetailDTO;
import com.jakdang.labs.api.lnuyasha.dto.SubjectGroupDTO;
import com.jakdang.labs.api.lnuyasha.dto.SubDetailWithQuestionsDTO;
import com.jakdang.labs.api.lnuyasha.repository.KySubjectDetailRepository;
import com.jakdang.labs.api.lnuyasha.repository.QuestionRepository;
import com.jakdang.labs.entity.SubjectDetailEntity;
import com.jakdang.labs.entity.QuestionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectDetailService {
    
    private final KySubjectDetailRepository subjectDetailRepository;
    private final QuestionRepository questionRepository;
    
    /**
     * 세부과목 목록 조회
     * @param educationId 학원 ID
     * @return 해당 학원의 세부과목 목록
     */
    public List<SubjectDetailDTO> getSubDetailList(String educationId) {
        log.info("[SUBJECT_DETAIL] Request to get sub detail list: educationId = {}", educationId);
        
        try {
            List<SubjectDetailEntity> subDetails = subjectDetailRepository.findByEducationId(educationId);
            
            List<SubjectDetailDTO> subDetailList = new ArrayList<>();
            
            for (SubjectDetailEntity entity : subDetails) {
                SubjectDetailDTO dto = SubjectDetailDTO.builder()
                    .subDetailName(entity.getSubDetailName())
                    .subDetailId(entity.getSubDetailId())
                    .subDetailActive(entity.getSubDetailActive())
                    .build();
                
                subDetailList.add(dto);
            }
            
            log.info("[SUBJECT_DETAIL] Sub detail list retrieval completed: {} items", subDetailList.size());
            return subDetailList;
            
        } catch (Exception e) {
            log.error("[SUBJECT_DETAIL] Error occurred while retrieving sub detail list: {}", e.getMessage(), e);
            throw new RuntimeException("Error occurred while retrieving sub detail list.", e);
        }
    }
    
    /**
     * 세부과목 목록과 문제 목록 함께 조회
     * @param educationId 학원 ID
     * @return 해당 학원의 세부과목 목록과 각 세부과목의 문제 목록
     */
    public List<SubDetailWithQuestionsDTO> getSubDetailListWithQuestions(String educationId) {
        log.info("[SUBJECT_DETAIL] Request to get sub detail list with questions: educationId = {}", educationId);
        
        try {
            List<SubjectDetailEntity> subDetails = subjectDetailRepository.findByEducationId(educationId);
            
            List<SubDetailWithQuestionsDTO> subDetailList = new ArrayList<>();
            
            for (SubjectDetailEntity entity : subDetails) {
                // 해당 세부과목의 문제 목록 조회
                List<QuestionEntity> questions = questionRepository.findBySubDetailId(entity.getSubDetailId());
                
                // 문제 목록을 DTO로 변환
                List<SubDetailWithQuestionsDTO.QuestionInfoDTO> questionDTOs = questions.stream()
                    .map(question -> {
                        // 강사 이름 조회 (간단한 구현)
                        String memberName = "강사"; // 실제로는 member 테이블에서 조회해야 함
                        
                        return SubDetailWithQuestionsDTO.QuestionInfoDTO.builder()
                            .questionId(question.getQuestionId())
                            .questionText(question.getQuestionText())
                            .questionType(question.getQuestionType())
                            .questionAnswer(question.getQuestionAnswer())
                            .explanation(question.getExplanation())
                            .codeLanguage(question.getCodeLanguage())
                            .memberId(question.getMemberId())
                            .memberName(memberName)
                            .questionActive(question.getQuestionActive())
                            .createdAt(question.getCreatedAt() != null ? question.getCreatedAt().toString() : null)
                            .build();
                    })
                    .collect(Collectors.toList());
                
                SubDetailWithQuestionsDTO dto = SubDetailWithQuestionsDTO.builder()
                    .subDetailId(entity.getSubDetailId())
                    .subDetailName(entity.getSubDetailName())
                    .subDetailInfo(entity.getSubDetailInfo())
                    .subDetailActive(entity.getSubDetailActive())
                    .subjectId(null) // SubjectDetailEntity에 subjectId 필드가 없음
                    .subjectName("과목") // 기본값
                    .educationId(entity.getEducationId())
                    .questions(questionDTOs)
                    .questionCount(questionDTOs.size())
                    .build();
                
                subDetailList.add(dto);
            }
            
            log.info("[SUBJECT_DETAIL] Sub detail list with questions retrieval completed: {} items", subDetailList.size());
            return subDetailList;
            
        } catch (Exception e) {
            log.error("[SUBJECT_DETAIL] Error occurred while retrieving sub detail list with questions: {}", e.getMessage(), e);
            throw new RuntimeException("Error occurred while retrieving sub detail list with questions.", e);
        }
    }
    
    /**
     * 과목별 세부과목 목록 조회 (그룹화된 구조)
     * @param educationId 학원 ID
     * @return 해당 학원의 과목별로 그룹화된 세부과목 정보
     */
    public Map<String, SubjectGroupDTO> getSubjectList(String educationId) {
        log.info("[SUBJECT_DETAIL] Request to get subject list: educationId = {}", educationId);
        
        try {
            List<SubjectDetailEntity> subDetails = subjectDetailRepository.findByEducationId(educationId);
            
            // 세부과목명의 첫 번째 단어를 과목명으로 사용하여 그룹화
            Map<String, List<SubjectDetailEntity>> groupedBySubject = subDetails.stream()
                .collect(Collectors.groupingBy(entity -> {
                    String subDetailName = entity.getSubDetailName();
                    if (subDetailName != null && !subDetailName.trim().isEmpty()) {
                        String[] words = subDetailName.split("\\s+");
                        return words[0]; // 첫 번째 단어를 과목명으로 사용
                    }
                    return "기타";
                }));
            
            Map<String, SubjectGroupDTO> result = new HashMap<>();
            
            for (Map.Entry<String, List<SubjectDetailEntity>> entry : groupedBySubject.entrySet()) {
                String subjectName = entry.getKey();
                List<SubjectDetailEntity> subDetailList = entry.getValue();
                
                // 세부과목 목록을 DTO로 변환
                List<SubjectGroupDTO.SubDetailDTO> subDetailDTOs = subDetailList.stream()
                    .map(entity -> SubjectGroupDTO.SubDetailDTO.builder()
                        .subDetailName(entity.getSubDetailName())
                        .subDetailId(entity.getSubDetailId())
                        .subDetailInfo(entity.getSubDetailInfo())
                        .subDetailActive(entity.getSubDetailActive())
                        .build())
                    .collect(Collectors.toList());
                
                SubjectGroupDTO groupDTO = SubjectGroupDTO.builder()
                    .mainSubject(subjectName)
                    .subjectId(subjectName) // 과목명을 ID로 사용
                    .subjectInfo("") // 기본값
                    .subDetails(subDetailDTOs)
                    .build();
                
                result.put(subjectName, groupDTO);
            }
            
            log.info("[SUBJECT_DETAIL] Subject list retrieval completed: {} subjects", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("[SUBJECT_DETAIL] Error occurred while retrieving subject list: {}", e.getMessage(), e);
            throw new RuntimeException("Error occurred while retrieving subject list.", e);
        }
    }
} 