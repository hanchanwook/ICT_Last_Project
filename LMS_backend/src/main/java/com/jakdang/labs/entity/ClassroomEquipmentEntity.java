package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "classroomequipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomEquipmentEntity {
    
    @Id
    @Column(name = "classEquipId")
    private String classEquipId; // UUID
    
    @Column(name = "classId", nullable = false, length = 100)
    private String classId; // 강의실 ID
    
    // JPA 관계 매핑: ClassroomEntity와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classId", insertable = false, updatable = false)
    private ClassroomEntity classroom; // 강의실 정보
    
    @Column(name = "equipName", nullable = false)
    private String equipName; // 장비명
    
    @Column(name = "equipModel", nullable = false)
    private String equipModel; // 장비 모델
    
    @Column(name = "equipNumber", nullable = false)
    private Integer equipNumber; // 장비 번호
    
    @Column(name = "equipDescription", columnDefinition = "TEXT")
    private String equipDescription; // 장비 설명 (구매처/구매링크)
    
    @Column(name = "equipRent", nullable = false)
    private Integer equipRent; // 대여료
    
    @Column(name = "equipActive", nullable = false)
    private Integer equipActive; // 활성화 상태
    
    @Column(name = "createdAt", nullable = false)
    private LocalDate createdAt; // 생성일
    
    @Column(name = "updatedAt")
    private LocalDate updatedAt; // 수정일
    
    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.classEquipId == null) {
            this.classEquipId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDate.now();
        }
    }
    
    @PreUpdate
    public void setUpdatedAt() {
        this.updatedAt = java.time.LocalDate.now();
    }
}
