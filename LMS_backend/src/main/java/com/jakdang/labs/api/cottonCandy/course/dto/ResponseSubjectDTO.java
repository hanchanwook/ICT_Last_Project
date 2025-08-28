package com.jakdang.labs.api.cottonCandy.course.dto;

import java.util.List;

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
public class ResponseSubjectDTO {
    private String subjectId;
    private String subjectName;
    private String subjectInfo;
    private String createdAt;

    private int useSubject;
    // 상세과목 정보 리스트
    private List<SubDetailList> subDetails;

    // 중첩 DTO: 상세과목 하나를 표현
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubDetailList {
        private String subDetailId;
        private String subDetailName;
        private String subDetailInfo;
    }
}
