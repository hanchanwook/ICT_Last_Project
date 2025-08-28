package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDetailResponseDTO {
    private String assignmentId;
    private String courseId;
    private String courseName;
    private String courseCode;
    private LocalDate courseStartDay;
    private LocalDate courseEndDay;
    private String assignmentTitle;
    private String assignmentContent;
    private LocalDate dueDate;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private Integer maxScore;
    private String assignmentType; // "INDIVIDUAL", "GROUP", "TEAM"
    private String status; // "ACTIVE", "INACTIVE", "DRAFT"
    private String memberId; // 강사 ID
    private String instructorName; // 강사 이름
    private Boolean fileRequired;
    private Boolean codeRequired;
    private Integer assignmentActive;
    private List<AssignmentFileDTO> attachments; // 첨부파일
    private List<AssignmentSubmissionDTO> submissions; // 제출 현황
    private Integer submissionCount;
    private Integer totalStudents;
    private Double averageScore;
    private String instructions;
    private String evaluationCriteria;
    // 프론트엔드 요구에 맞춰 소문자 rubricitem
    private List<RubricItemDTO> rubricitem;
    
    // 루브릭 관련 필드
    private Boolean hasRubric; // 루브릭 설정 여부
    private RubricDTO rubric; // 루브릭 정보
} 