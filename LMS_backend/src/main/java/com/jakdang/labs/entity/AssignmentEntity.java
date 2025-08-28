package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;

@Entity
@Table(name = "assignment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentEntity {
    @Id
    @Column(name = "assignmentId", length = 100, nullable = false)
    private String assignmentId;

    @Column(name = "courseId", length = 100, nullable = false)
    private String courseId;

    @Column(name = "memberId", length = 100, nullable = false)
    private String memberId; // 강사 ID

    @Column(name = "assignmentTitle", length = 100, nullable = false)
    private String assignmentTitle;

    @Column(name = "assignmentContent", columnDefinition = "TEXT")
    private String assignmentContent;

    @Column(name = "dueDate")
    private LocalDate dueDate;

    @Column(name = "fileRequired")
    private Boolean fileRequired;

    @Column(name = "codeRequired")
    private Boolean codeRequired;

    @Column(name = "isLocked")
    private Boolean isLocked;

    @Column(name = "assignmentActive")
    private Integer assignmentActive;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDate createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDate updatedAt;
} 
