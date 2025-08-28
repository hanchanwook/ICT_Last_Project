package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 과정에 연결된 과목 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSubjectDTO {
    private String subjectId;      // 과목 고유 ID
    private String subjectName;    // 과목명
    private String subjectInfo;    // 과목 설명
    private int subjectActive;     // 과목 활성화 상태 (0: 활성, 1: 비활성)
    private List<CourseSubDetailDTO> subDetails; // 세부과목 목록
} 