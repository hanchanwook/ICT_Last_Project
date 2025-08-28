package com.jakdang.labs.entity;

import java.util.UUID;

import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 과목 엔티티 */
@Entity
@Table(name = "subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String subjectId; // 과목 ID

    @Column(length = 50, nullable = false)
    private String subjectName; // 과목명
 
    @Column(length = 100)
    private String subjectInfo; // 과목 정보

    @Column(nullable = false, columnDefinition = "int default 0")
    private int subjectActive; // 활성화 상태

    @Column(length = 100, nullable = false)
    private String educationId; // 교육 ID

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.subjectId == null) {
            this.subjectId = UUID.randomUUID().toString();
        }
    }
}

