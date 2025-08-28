package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 채점 완료 API 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamGradingSubmitRequestDTO {
    private String memberId;                    // 학생 ID (필수)
    private String templateId;                  // 시험 템플릿 ID (필수)
    private Integer score;                      // 총점 (필수)
    private Integer isChecked;                  // 채점 완료 여부 (1: 완료, 0: 미완료) - 실제로는 사용되지 않음
    private List<AnswerDTO> questionDetails;  // 각 문제별 상세 정보 (필수)
    private String overallFeedback;             // 전체 피드백 (선택)
} 