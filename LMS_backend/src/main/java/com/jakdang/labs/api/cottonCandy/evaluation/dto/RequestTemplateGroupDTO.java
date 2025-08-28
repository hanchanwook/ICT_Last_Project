package com.jakdang.labs.api.cottonCandy.evaluation.dto;


import java.time.LocalDate;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestTemplateGroupDTO {
    private String templateGroupId;
    private String courseId;
    private LocalDate openDate;
    private LocalDate closeDate;
    private int questionTemplateNum;
    private String userId;  // 프론트엔드에서 보내는 userId 추가

}
