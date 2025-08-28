package com.jakdang.labs.api.cottonCandy.evaluation.dto;

import java.time.LocalDate;
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
public class ResponseStudentCourseDTO {

    // 과정 정보
    private String courseId;
    private String courseCode; // 과정 코드 
    private String courseName; // 과정 이름
    private String memberId;
    private String memberName; // 강사 이름 
    private String educationId;
    private int maxCapacity; // 최대인원
    private int studentCount; // 해당 과정를 듣는 학생 수
    private LocalDate courseStartDay; // 과정 시작 날짜
    private LocalDate courseEndDay; // 과정 종료 날짜
    private String courseDays; // 과정 요일
    private String startTime; // 과정 시작 시간
    private String endTime; // 과정 종료 시간

    private boolean response; // 평가 답변 여부 (강의 레벨)
    
    private List<TemplateItem> templateList; // 템플릿 리스트
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateItem {
            private boolean response; // 평가 답변 여부
            private String templateGroupId; // 템플릿 그룹 ID
            private LocalDate openDate; // 평가 시작일
            private LocalDate closeDate; // 평가 종료일
            private int questionTemplateNum; // 템플릿 번호
            private String questionTemplateName; // 템플릿 이름
        }

    
    

    

}
