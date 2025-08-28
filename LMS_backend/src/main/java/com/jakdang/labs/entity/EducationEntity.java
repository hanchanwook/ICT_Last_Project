package com.jakdang.labs.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.jakdang.labs.global.BaseEntity;

@Entity
@Table(name = "education")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EducationEntity extends BaseEntity {
    @Id
    @Column(name = "educationId")
    private String educationId;

    private String educationName;

    private String businessNumber;

    private String educationAddress;

    private String description;

    private String educationDetailAddress;

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.educationId == null) {
            this.educationId = UUID.randomUUID().toString();
        }
    }
}
