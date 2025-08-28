package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.ExamGradingSubmitRequestDTO;
import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.repository.AnswerRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.ScoreStudentRepository;
import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시험 채점 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExamGradingService {
    
    private final AnswerRepository answerRepository;
    private final TemplateRepository templateRepository;
    private final KyMemberRepository memberRepository;
    private final ScoreStudentRepository scoreStudentRepository;
    
    /**
     * 채점 완료 처리
     */
    public AnswerDTO submitGrading(ExamGradingSubmitRequestDTO request) {
        
        // 1. 필수 필드 검증
        validateRequest(request);
        
        // 2. 학생 정보 확인
        validateStudent(request.getMemberId());
        
        // 3. 시험 템플릿 확인
        validateTemplate(request.getTemplateId());
        
        // 4. 기존 답안 조회
        List<AnswerEntity> existingAnswers = answerRepository.findByTemplateIdAndMemberId(
                request.getTemplateId(), request.getMemberId());
        
        if (existingAnswers.isEmpty()) {
            throw new IllegalArgumentException("해당 학생의 답안을 찾을 수 없습니다.");
        }
        
        // 5. 이미 채점 완료되었는지 확인 (scorestudent 테이블에 레코드가 있으면 중복 채점)
        ScoreStudentEntity existingScore = scoreStudentRepository.findByTemplateIdAndMemberId(
                request.getTemplateId(), request.getMemberId());
        
        if (existingScore != null) {
            log.warn("이미 채점 완료된 시험: memberId={}, templateId={}, isChecked={}", 
                    request.getMemberId(), request.getTemplateId(), existingScore.getIsChecked());
            throw new IllegalStateException("이미 채점이 완료된 시험입니다.");
        }
        
        // 6. 각 문제별 답안 업데이트
        for (AnswerDTO questionDetail : request.getQuestionDetails()) {
            updateAnswerForQuestion(existingAnswers, questionDetail);
        }
        
        // 7. 전체 답안에 총점 및 피드백 업데이트
        updateOverallGrading(existingAnswers, request);
        
        // 8. scorestudent 테이블에 새 레코드 생성
        ScoreStudentEntity savedScore = updateScoreStudentTable(request);
        
        // 9. 결과 반환
        return AnswerDTO.builder()
                .scoreStudentId(savedScore.getScoreStudentId())
                .memberId(savedScore.getMemberId())
                .templateId(savedScore.getTemplateId())
                .score(savedScore.getScore())
                .isChecked(savedScore.getIsChecked())
                .totalComment(savedScore.getTotalComment())
                .build();
    }
    
    /**
     * 요청 데이터 검증
     */
    private void validateRequest(ExamGradingSubmitRequestDTO request) {
        if (request.getMemberId() == null || request.getMemberId().trim().isEmpty()) {
            throw new IllegalArgumentException("학생 ID는 필수입니다.");
        }
        
        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            throw new IllegalArgumentException("시험 템플릿 ID는 필수입니다.");
        }
        
        if (request.getScore() == null) {
            throw new IllegalArgumentException("총점은 필수입니다.");
        }
        
        if (request.getIsChecked() == null) {
            throw new IllegalArgumentException("채점 완료 여부는 필수입니다.");
        }
        
        if (request.getQuestionDetails() == null || request.getQuestionDetails().isEmpty()) {
            throw new IllegalArgumentException("문제별 상세 정보는 필수입니다.");
        }
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
     * 특정 문제의 답안 업데이트
     */
    private void updateAnswerForQuestion(List<AnswerEntity> answers, AnswerDTO questionDetail) {
        // 해당 문제의 답안 찾기
        Optional<AnswerEntity> targetAnswer = answers.stream()
                .filter(answer -> answer.getTemplateQuestion() != null && 
                        questionDetail.getQuestionId().equals(answer.getTemplateQuestion().getQuestionId()))
                .findFirst();
        
        if (targetAnswer.isPresent()) {
            AnswerEntity answer = targetAnswer.get();
            
            // 점수 업데이트
            if (questionDetail.getScore() != null) {
                answer.setAnswerScore(questionDetail.getScore());
            }
            
            // 코멘트 업데이트
            if (questionDetail.getComment() != null) {
                answer.setTeacherComment(questionDetail.getComment());
            }
            
            // 채점 완료 시간 설정
            answer.setAnswerGradedAt(LocalDateTime.now());
            answer.setAnswerGradeUpdatedAt(LocalDateTime.now());
            
            // 정답 여부 설정 (필요한 경우)
            // Note: AnswerEntity에 isCorrect 필드가 있다면 설정
            
            answerRepository.save(answer);
            

        } else {
            log.warn("해당 문제의 답안을 찾을 수 없음: questionId={}", questionDetail.getQuestionId());
        }
    }
    
    /**
     * 전체 답안에 총점 및 피드백 업데이트
     */
    private void updateOverallGrading(List<AnswerEntity> answers, ExamGradingSubmitRequestDTO request) {
        // 첫 번째 답안에 전체 피드백 저장 (또는 별도 테이블에 저장)
        if (!answers.isEmpty() && request.getOverallFeedback() != null) {
            AnswerEntity firstAnswer = answers.get(0);
            // Note: AnswerEntity에 overallFeedback 필드가 있다면 설정
            // firstAnswer.setOverallFeedback(request.getOverallFeedback());
            answerRepository.save(firstAnswer);
        }
        

    }
    
    /**
     * scorestudent 테이블 업데이트
     */
    private ScoreStudentEntity updateScoreStudentTable(ExamGradingSubmitRequestDTO request) {
        try {
            // 새 레코드 생성 (중복 체크는 이미 submitGrading에서 수행됨)
            ScoreStudentEntity newScore = ScoreStudentEntity.builder()
                    .score(request.getScore())
                    .isChecked(1) // 채점 완료, 학생 확인 가능
                    .totalComment(request.getOverallFeedback()) // overallFeedback을 totalComment에 저장
                    .memberId(request.getMemberId())
                    .templateId(request.getTemplateId())
                    .graded(0) // 채점 완료 (0: 채점 완료, 1: 미채점)
                    .build();
            
            ScoreStudentEntity resultScore = scoreStudentRepository.save(newScore);
            return resultScore;
            
        } catch (Exception e) {
            log.error("=== scorestudent 테이블 업데이트 중 오류 발생 ===");
            log.error("오류 상세: memberId={}, templateId={}, error={}", 
                    request.getMemberId(), request.getTemplateId(), e.getMessage(), e);
            throw new RuntimeException("scorestudent 테이블 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 