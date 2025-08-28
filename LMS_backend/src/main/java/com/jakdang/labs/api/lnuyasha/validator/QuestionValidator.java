package com.jakdang.labs.api.lnuyasha.validator;

import com.jakdang.labs.api.lnuyasha.dto.QuestionCreateRequestDTO;
import com.jakdang.labs.api.lnuyasha.dto.QuestionOptionDTO;
import com.jakdang.labs.api.lnuyasha.repository.KySubjectDetailRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.entity.SubjectDetailEntity;
import com.jakdang.labs.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Arrays;

/**
 * 문제 생성 요청 유효성 검사기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionValidator {
    
    private final KySubjectDetailRepository subjectDetailRepository;
    private final KyMemberRepository memberRepository;
    
    // 허용된 문제 유형
    private static final List<String> ALLOWED_QUESTION_TYPES = Arrays.asList("객관식", "서술형", "코드형");
    
    /**
     * 문제 생성 요청의 유효성을 검사
     * @param request 문제 생성 요청
     * @throws IllegalArgumentException 유효하지 않은 경우
     */
    public void validateQuestionCreateRequest(QuestionCreateRequestDTO request) {
        try {
            // 1. 필수 필드 검증
            validateRequiredFields(request);
            
            // 2. 문제 유형 검증
            validateQuestionType(request.getQuestionType());
            
            // 3. 세부과목명 검증
            validateSubDetailName(request.getSubDetailName());
            
            // 4. 강사 ID 검증
            validateInstructorId(request.getInstructorId());
            
            // 5. 조건부 필드 검증
            if ("객관식".equals(request.getQuestionType())) {
                validateObjectiveOptions(request.getOptions());
            }
        } catch (Exception e) {
            log.error("=== 문제 생성 요청 유효성 검사 실패 ===");
            log.error("오류 메시지: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 필수 필드 검증
     */
    private void validateRequiredFields(QuestionCreateRequestDTO request) {
        if (request.getQuestionText() == null || request.getQuestionText().trim().isEmpty()) {
            log.error("문제 내용이 누락되었습니다.");
            throw new IllegalArgumentException("문제 내용은 필수입니다. (questionText)");
        }
        
        if (request.getQuestionType() == null || request.getQuestionType().trim().isEmpty()) {
            log.error("문제 유형이 누락되었습니다.");
            throw new IllegalArgumentException("문제 유형은 필수입니다. (questionType)");
        }
        
        if (request.getSubDetailName() == null || request.getSubDetailName().trim().isEmpty()) {
            log.error("세부과목명이 누락되었습니다.");
            throw new IllegalArgumentException("세부과목명은 필수입니다. (subDetailName) - 프론트엔드에서 세부과목을 선택해주세요.");
        }
        
        // 객관식 문제의 경우에만 questionAnswer가 필수
        if ("객관식".equals(request.getQuestionType())) {
            if (request.getQuestionAnswer() == null || request.getQuestionAnswer().trim().isEmpty()) {
                log.error("객관식 문제의 정답이 누락되었습니다.");
                throw new IllegalArgumentException("객관식 문제의 정답은 필수입니다. (questionAnswer)");
            }
        }
        
        if (request.getInstructorId() == null || request.getInstructorId().trim().isEmpty()) {
            log.error("강사 ID가 누락되었습니다.");
            throw new IllegalArgumentException("강사 ID는 필수입니다. (instructorId)");
        }
    }
    
    /**
     * 문제 유형 검증
     */
    private void validateQuestionType(String questionType) {
        if (!ALLOWED_QUESTION_TYPES.contains(questionType)) {
            throw new IllegalArgumentException("문제 유형은 '객관식', '서술형', '코드형' 중 하나여야 합니다. 현재: " + questionType);
        }
    }
    
    /**
     * 세부과목명 검증
     */
    private void validateSubDetailName(String subDetailName) {
        // 세부과목명으로 세부과목 ID 조회
        List<SubjectDetailEntity> subDetails = subjectDetailRepository.findBySubDetailName(subDetailName);
        if (subDetails.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 세부과목명입니다: " + subDetailName);
        }
    }
    
    /**
     * 강사 ID 검증 (userId로 조회)
     */
    private void validateInstructorId(String instructorId) {
        // 1. 먼저 id 컬럼으로 조회 (userId)
        List<MemberEntity> members = memberRepository.findByIdColumn(instructorId);
        
        if (members.isEmpty()) {
            // 2. id로 조회 실패시 memberEmail로 조회 (하위 호환성)
            members = memberRepository.findByMemberEmail(instructorId);
        }
        
        if (members.isEmpty()) {
            log.error("userId 또는 memberEmail로 조회된 결과가 없음: {}", instructorId);
            throw new IllegalArgumentException("유효하지 않은 강사 ID입니다: " + instructorId);
        }
        
        MemberEntity memberEntity = members.get(0);
        
        if (!"강사".equals(memberEntity.getMemberRole()) && 
            !"TEACHER".equals(memberEntity.getMemberRole()) && 
            !"INSTRUCTOR".equals(memberEntity.getMemberRole()) &&
            !"ROLE_INSTRUCTOR".equals(memberEntity.getMemberRole())) {
            log.error("강사 권한이 아님: memberRole = {}", memberEntity.getMemberRole());
            throw new IllegalArgumentException("강사 권한이 없는 사용자입니다: " + instructorId);
        }
    }
    
    /**
     * 객관식 선택지 검증
     */
    private void validateObjectiveOptions(List<QuestionOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("객관식 문제는 선택지가 필수입니다.");
        }
        
        // 선택지 개수를 4개로 고정
        if (options.size() != 4) {
            throw new IllegalArgumentException("객관식 문제는 정확히 4개의 선택지가 필요합니다. 현재: " + options.size() + "개");
        }
        
        // 정답 개수 확인
        long correctCount = options.stream()
                .filter(option -> option.getOptIsCorrect() == 1)
                .count();
        
        if (correctCount == 0) {
            throw new IllegalArgumentException("객관식 문제는 정답이 하나 있어야 합니다.");
        }
        
        if (correctCount > 1) {
            throw new IllegalArgumentException("객관식 문제는 정답이 하나만 있어야 합니다. 현재: " + correctCount + "개");
        }
        
        // 선택지 내용 검증
        for (int i = 0; i < options.size(); i++) {
            QuestionOptionDTO option = options.get(i);
            if (option.getOptText() == null || option.getOptText().trim().isEmpty()) {
                throw new IllegalArgumentException("선택지 " + (i + 1) + "의 내용은 필수입니다.");
            }
        }
        

    }
} 