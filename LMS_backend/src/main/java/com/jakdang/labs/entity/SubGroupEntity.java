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

/** 과목 + 과정 그룹 엔티티 */
@Entity
@Table(name = "subgroup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubGroupEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String subGroupId; // 과목 그룹 ID

    @Column(length = 100, nullable = false)
    private String courseId; // 강의 ID

    @Column(length = 100, nullable = false)
    private String subjectId; // 과목 ID

    @Column(nullable = false)
    private int subjectTime ; // 과목 시간

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.subGroupId == null) {
            this.subGroupId = UUID.randomUUID().toString();
        }
    }
}

