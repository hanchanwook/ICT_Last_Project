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

/** 과목 + 과목 상세 그룹 엔티티 */
@Entity
@Table(name = "subdetailgroup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubDetailGroupEntity extends BaseEntity {

    @Id
    @Column(length = 100)
    private String subDetailGroupId; // 과목 상세 그룹 ID

    @Column(length = 100, nullable = false)
    private String subDetailId; // 과목 상세 ID

    @Column(length = 100, nullable = false)
    private String subjectId; // 과목 ID

    // UUID 자동 생성 및 BaseEntity의 onCreate 호출
    @PrePersist
    public void prePersist() {
        // BaseEntity의 onCreate 호출
        super.onCreate();
        
        // UUID 생성
        if (this.subDetailGroupId == null) {
            this.subDetailGroupId = UUID.randomUUID().toString();
        }
    }
}


