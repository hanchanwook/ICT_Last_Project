package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qrcode")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCodeEntity {
    
    @Id
    @Column(name = "qrCodeId")
    private String qrCodeId; // UUID
    
    @Column(name = "classId", nullable = false, length = 100)
    private String classId; // 강의실 ID
    
    // JPA 관계 매핑: ClassroomEntity와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classId", insertable = false, updatable = false)
    private ClassroomEntity classroom; // 강의실 정보
    
    @Column(name = "qrUrl")
    private String qrUrl; // QR코드 URL
    
    @Column(name = "qrCodeActive")
    private int qrCodeActive; // 활성화 상태
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt; // 생성 시간
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt; // 수정 시간

    @Column(name = "courseId", nullable = false, length = 255)
    private String courseId; // 과정 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courseId", insertable = false, updatable = false)
    private CourseEntity course; // 과정 정보
    
    // UUID 자동 생성 및 시간 설정
    @PrePersist
    public void generateUUID() {
        if (this.qrCodeId == null) {
            this.qrCodeId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    
    // 수정 시간 자동 설정
    @PreUpdate
    public void setUpdateTime() {
        this.updatedAt = LocalDateTime.now();
    }

}
