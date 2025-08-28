package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "assignmentsubmission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionEntity {
    @Id
    @Column(length = 100)
    private String submissionId;

    @Column(length = 100, nullable = false)
    private String assignmentId;

    @Column(length = 100, nullable = false)
    private String id; // 학생의 users.id

    @Column(length = 100)
    private String memberId; // 학생의 memberId

    @Lob
    private String answerText;

    private Timestamp submittedAt;

    private Integer score;

    @Lob
    private String feedback;

    private Integer submissionStatus;

    @Column(length = 10)
    private String submissionType;

    private Timestamp createdAt;
    private Timestamp updatedAt;
} 
