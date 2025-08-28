package com.jakdang.labs.api.jaegyeom.popup.entity;

import com.jakdang.labs.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import java.time.LocalDateTime;

@Entity
@Table(name = "popup")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupEntity extends BaseEntity {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    // === 작성자 & 수정자 ===
    private String createdBy;

    private String updatedBy;

    // === 추가: 교육기관 ID ===
    @Column(nullable = false)
    private String educationId;

    @PrePersist
    public void generateUUID() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
