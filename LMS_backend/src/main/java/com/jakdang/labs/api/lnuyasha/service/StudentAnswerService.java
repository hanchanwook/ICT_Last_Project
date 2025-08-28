package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.repository.AnswerRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateQuestionRepository;
import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.TemplateQuestionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 학생 답안 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAnswerService {
    
    private final AnswerRepository answerRepository;
    private final TemplateRepository templateRepository;
    private final KyMemberRepository memberRepository;
    private final TemplateQuestionRepository templateQuestionRepository;
    
    /**
     * 학생 답안 조회
     */
    public AnswerDTO getStudentAnswers(String templateId, String memberId) {
        log.info("학생 답안 조회 요청: templateId={}, memberId={}", templateId, memberId);
        
        // 1. 학생 정보 확인
        validateStudent(memberId);
        
        // 2. 시험 템플릿 확인
        validateTemplate(templateId);
        
        // 3. 학생 답안 조회
        List<AnswerEntity> answers = answerRepository.findByTemplateIdAndMemberId(templateId, memberId);
        
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("해당 학생의 답안을 찾을 수 없습니다.");
        }
        
        // 4. 시험의 전체 문제 수 조회
        int totalQuestions = getTotalQuestions(templateId);
        
        // 5. 답안 데이터 구성
        Map<String, String> answerMap = new HashMap<>();
        LocalDateTime submittedAt = null;
        int answeredCount = 0;
        
        for (AnswerEntity answer : answers) {
            if (answer.getTemplateQuestion() != null) {
                String questionId = answer.getTemplateQuestion().getQuestionId();
                String answerText = answer.getAnswerText();
                
                if (answerText != null && !answerText.trim().isEmpty()) {
                    answerMap.put(questionId, answerText);
                    answeredCount++;
                }
                
                // 제출 시간은 가장 늦은 답안의 생성 시간으로 설정
                if (submittedAt == null || 
                    (answer.getCreatedAt() != null && answer.getCreatedAt().isAfter(submittedAt))) {
                    submittedAt = answer.getCreatedAt();
                }
            }
        }
        
        // 6. 소요 시간 계산 (시험 시작 시간과 제출 시간의 차이)
        Integer timeSpent = calculateTimeSpent(templateId, submittedAt);
        
        // 7. 응답 DTO 생성
        AnswerDTO response = AnswerDTO.builder()
                .memberId(memberId)
                .templateId(templateId)
                .submittedAt(submittedAt)
                .answers(answerMap)
                .timeSpent(timeSpent)
                .answeredQuestions(answeredCount)
                .totalQuestions(totalQuestions)
                .build();
        
        log.info("학생 답안 조회 완료: answeredQuestions={}, totalQuestions={}", 
                answeredCount, totalQuestions);
        
        return response;
    }
    
    /**
     * 학생 정보 확인
     */
    private void validateStudent(String memberId) {
        List<MemberEntity> students = memberRepository.findByMemberId(memberId);
        if (students.isEmpty()) {
            throw new IllegalArgumentException("해당 학생을 찾을 수 없습니다: " + memberId);
        }
    }
    
    /**
     * 시험 템플릿 확인
     */
    private void validateTemplate(String templateId) {
        Optional<TemplateEntity> template = templateRepository.findById(templateId);
        if (template.isEmpty()) {
            throw new IllegalArgumentException("해당 시험 템플릿을 찾을 수 없습니다: " + templateId);
        }
    }
    
    /**
     * 시험의 전체 문제 수 조회
     */
    private int getTotalQuestions(String templateId) {
        List<TemplateQuestionEntity> questions = templateQuestionRepository.findByTemplateId(templateId);
        return questions.size();
    }
    
    /**
     * 소요 시간 계산 (분 단위)
     */
    private Integer calculateTimeSpent(String templateId, LocalDateTime submittedAt) {
        if (submittedAt == null) {
            return null;
        }
        
        // 시험 시작 시간 조회 (templateOpen 사용)
        Optional<TemplateEntity> template = templateRepository.findById(templateId);
        if (template.isPresent() && template.get().getTemplateOpen() != null) {
            LocalDateTime examStartTime = template.get().getTemplateOpen();
            
            // 소요 시간 계산 (분 단위)
            Duration duration = Duration.between(examStartTime, submittedAt);
            return (int) duration.toMinutes();
    }

        return null;
    }
} 