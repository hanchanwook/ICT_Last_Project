package com.jakdang.labs.api.gemjjok.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_submission_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubmissionFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "assignment_id", nullable = false)
    private String assignmentId;
    
    @Column(name = "student_id", nullable = false)
    private String studentId; // 학생의 memberId
    
    @Column(name = "file_key", nullable = false)
    private String fileKey; // 파일 서비스의 고유 키
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "content_type")
    private String contentType;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "material_id")
    private String materialId; // 파일 서비스의 materialId
    
    @Column(name = "is_active")
    @Builder.Default
    private Integer isActive = 1;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "uploaded_by")
    private String uploadedBy;
} 