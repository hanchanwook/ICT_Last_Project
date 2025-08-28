package com.jakdang.labs.entity;

import java.util.UUID;

import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 세부 과목 정보 엔티티 */
@Entity
@Table(name = "subjectdetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDetailEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String subDetailId; // 과목 상세 ID

    @Column(length = 100, nullable = false)
    private String educationId; // 기관관 ID

    @Column(nullable = false)
    private String subDetailName; // 과목명

    @Column(length = 100, nullable = false)
    private String subDetailInfo; // 과목 정보

    @Column(nullable = false, columnDefinition = "int default 0")
    private int subDetailActive = 0; // 활성화 상태

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.subDetailId == null) {
            this.subDetailId = UUID.randomUUID().toString();
        }
    }
}
