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
public class AssignmentSubmissionDTO {
    private String submissionId;
    private String assignmentId;
    private String id;
    private String memberId;
    private String answerText;
    private java.sql.Timestamp submittedAt;
    private Integer score;
    private String feedback;
    private Integer submissionStatus;
    private String submissionType;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;
} 