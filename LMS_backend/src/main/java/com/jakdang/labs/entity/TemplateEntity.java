package com.jakdang.labs.entity;

import java.time.LocalDateTime;

import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID 생성
    @Column(name = "templateId", columnDefinition = "VARCHAR(100)")
    private String templateId;  // 템플릿 UUID
    
    @Column(name = "templateNumber", columnDefinition = "int")
    private int templateNumber; // 문제수

    @Column(name = "templateName", columnDefinition = "VARCHAR(100)")
    private String templateName; // 시험명
    
    @Column(name = "templateTime", columnDefinition = "int")
    private int templateTime; // 시험시간

    @Column(name = "templateOpen", columnDefinition = "TIMESTAMP")
    private LocalDateTime templateOpen; // 시험 시작 시간

    @Column(name = "templateClose", columnDefinition = "TIMESTAMP")
    private LocalDateTime templateClose; // 시험 종료 시간

    @Column(name = "templateActive", columnDefinition = "int default 0")
    private int templateActive; // 삭제 여부 (0: 미삭제, 1: 삭제 등)
    
    @Column(name = "memberId", columnDefinition = "VARCHAR(100)")
    private String memberId; // 등록한 멤버 UUID (강사ID)

    @Column(name = "subGroupId", columnDefinition = "VARCHAR(100)")
    private String subGroupId; // 과정 + 과목 UUID

    @Column(name = "educationId", columnDefinition = "VARCHAR(100)")
    private String educationId; // 학원 UUId
    
    @Version
    @Column(name = "version", columnDefinition = "int default 0")
    private Integer version; // 낙관적 락을 위한 버전 필드
}