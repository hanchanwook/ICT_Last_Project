package com.jakdang.labs.api.cottonCandy.course.dto;

import com.jakdang.labs.entity.SubjectDetailEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseSubDetailDTO {
    private String subDetailId;
    private String subDetailName;
    private String subDetailInfo;
    private String createdAt;

    public static ResponseSubDetailDTO fromEntity(SubjectDetailEntity entity) {
        return ResponseSubDetailDTO.builder()
                .subDetailId(entity.getSubDetailId())
                .subDetailName(entity.getSubDetailName())
                .subDetailInfo(entity.getSubDetailInfo())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build();
    }
}
