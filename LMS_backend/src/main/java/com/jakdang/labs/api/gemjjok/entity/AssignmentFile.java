package com.jakdang.labs.api.gemjjok.entity;

import com.jakdang.labs.api.file.dto.FileEnum;
import com.jakdang.labs.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assignment_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentFile extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "assignment_id", length = 100, nullable = false)
    private String assignmentId;  // 과제 ID
    
    @Column(name = "material_id", length = 100, unique = true, nullable = false)
    private String materialId;    // 자료 ID (UUID)
    
    @Column(name = "file_key", length = 500, nullable = false)
    private String fileKey;       // S3 파일 키
    
    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;      // 파일명
    
    @Column(name = "title", length = 255)
    private String title;         // 자료 제목
    
    @Column(name = "file_size")
    private Long fileSize;        // 파일 크기
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 20, nullable = false)
    private FileEnum fileType;    // 파일 타입
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;  // 썸네일 URL
    
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;    // 업로더 ID
    
    @Column(name = "is_active", nullable = false)
    private Integer isActive;     // 활성 상태 (0: 활성, 1: 비활성)
} 