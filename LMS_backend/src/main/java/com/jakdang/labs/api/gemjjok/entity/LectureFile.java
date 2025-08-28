package com.jakdang.labs.api.gemjjok.entity;

import com.jakdang.labs.api.file.dto.FileEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lecture_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureFile {

    @PrePersist
    protected void onCreate() {
        if (this.materialId == null) {
            this.materialId = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecture_id", nullable = false, length = 100)
    private String lectureId;

    @Column(name = "course_id", nullable = false, length = 100)
    private String courseId;

    @Column(name = "material_id", nullable = false, length = 100)
    private String materialId;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 100)
    private FileEnum fileType;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(name = "uploaded_by", length = 100, nullable = false)
    private String uploadedBy;

    @Column(name = "is_active", nullable = false, columnDefinition = "int default 1")
    private Integer isActive;

    @Column(name = "plan_id", length = 100)
    private String planId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 파일 확장자 추출
    public String getFileExtension() {
        if (this.fileName == null || this.fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = this.fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < this.fileName.length() - 1) {
            return this.fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }

    // 파일 확장자가 이미지인지 확인
    public boolean isImageFile() {
        String extension = getFileExtension();
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("gif") || 
               extension.equals("bmp") || extension.equals("webp");
    }

    // 파일 확장자가 비디오인지 확인
    public boolean isVideoFile() {
        String extension = getFileExtension();
        return extension.equals("mp4") || extension.equals("avi") || 
               extension.equals("mov") || extension.equals("wmv") || 
               extension.equals("flv") || extension.equals("webm");
    }
} 