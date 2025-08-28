package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "classroom")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomEntity {
    
    @Id
    @Column(name = "classId")
    private String classId; // UUID
    
    @Column(name = "classCode", nullable = false)
    private String classCode; // 강의실 코드
    
    @Column(name = "classNumber", nullable = false)
    private int classNumber; // 강의실 번호
    
    @Column(name = "classCapacity", nullable = false)
    private String classCapacity; // 수용 인원 (VARCHAR로 저장됨)
    
    @Column(name = "classActive", nullable = false)
    private int classActive; // 활성화 상태
    
    @Column(name = "classRent", nullable = false)
    private int classRent; // 대여료
    
    @Column(name = "classArea")
    private String classArea; // 면적
    
    @Column(name = "classPersonArea")
    private String classPersonArea; // 인당 면적
    
    @Column(name = "createdAt", nullable = false)
    private LocalDate createdAt; // 생성일
    
    @Column(name = "updatedAt")
    private LocalDate updatedAt; // 수정일
    
    @Column(name = "classMemo", columnDefinition = "TEXT")
    private String classMemo; // 메모
    
    @Column(name = "educationId", nullable = false)
    private String educationId; // 학원 ID

    @Column(name = "educationName", nullable = false)
    private String educationName; // 학원 이름
    
    // JPA 관계 매핑: ClassroomEquipmentEntity와의 관계
    @OneToMany(mappedBy = "classroom", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ClassroomEquipmentEntity> equipments; // 강의실 장비 목록
    
    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.classId == null) {
            this.classId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDate.now();
        }
    }
} 
