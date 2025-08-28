package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentStatsResponseDTO {
    private Integer totalAssignments;
    private Integer activeAssignments;
    private Integer completedAssignments;
    private Integer draftAssignments;
    private Integer totalSubmissions;
    private Integer gradedSubmissions;
    private Integer pendingSubmissions;
    private Double averageScore;
    private Double submissionRate;
} 