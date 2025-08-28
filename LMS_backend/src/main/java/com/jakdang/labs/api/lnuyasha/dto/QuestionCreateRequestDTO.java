package com.jakdang.labs.api.lnuyasha.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 문제 생성 요청 DTO
 * 새 문제 생성 시 필요한 정보를 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionCreateRequestDTO {
    
    // 필수 필드
    private String questionText;       // 문제 내용 (문자열)
    private String questionType;       // 문제 유형 ("객관식", "서술형", "코드형")
    private String subDetailName;      // 세부과목명 (문자열)
    private String questionAnswer;     // 정답 (문자열)
    
    // 선택 필드
    private String explanation;        // 문제 해설 (문자열, 빈 문자열 가능)
    private Integer questionActive;    // 문제 활성화 상태 (0: 활성, 1: 비활성)
    
    // 조건부 필드 (객관식 문제일 때만)
            private List<QuestionOptionDTO> options;  // 객관식 선택지 목록 (별도 테이블로 관리)
    
    // 자동 추가 필드 (프론트엔드에서 자동 추가)
    private String instructorId;       // 현재 로그인한 강사 ID
    
    // 기존 필드 (하위 호환성)
    private String subDetailId;        // 세부과목 ID (subDetailName으로 대체 예정)
    private String educationId;        // 학원 ID
    private String memberId;           // 등록한 강사 ID
    private String codeLanguage;       // 코드 언어 (코드형인 경우)
} 