package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentListResponseDTO {
    private String assignmentId;
    private String courseId;
    private String courseName;
    private String assignmentTitle;
    private String assignmentContent;
    private LocalDate dueDate;
    private LocalDate createdAt;
    private Integer maxScore;
    private String assignmentType; // "INDIVIDUAL", "GROUP", "TEAM"
    private Integer submissionCount;
    private Integer totalStudents;
    private String status; // "ACTIVE", "INACTIVE", "DRAFT"
    private String memberId; // 강사 ID
} 