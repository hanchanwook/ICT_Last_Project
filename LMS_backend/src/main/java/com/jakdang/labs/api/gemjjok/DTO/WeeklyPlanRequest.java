package com.jakdang.labs.api.gemjjok.DTO;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

public class WeeklyPlanRequest {
    private Integer weekNumber;   // 필수
    private String weekTitle;     // 필수
    private String weekContent;   // 선택
    private String subjectId; // 단일 값으로 변경
    private String subDetailId; // 단일 값으로 변경
    
    // 프론트엔드에서 배열로 전송하는 경우를 위한 필드
    private List<String> subjectIds;
    private List<String> subDetailIds;
    
    // 기본 생성자
    public WeeklyPlanRequest() {}
    
    // weekNumber 유연한 설정을 위한 setter (Jackson이 사용할 메서드)
    @JsonSetter("weekNumber")
    public void setWeekNumber(Object weekNumber) {
        if (weekNumber == null) {
            this.weekNumber = null;
        } else if (weekNumber instanceof Integer) {
            this.weekNumber = (Integer) weekNumber;
        } else if (weekNumber instanceof String) {
            try {
                this.weekNumber = Integer.valueOf((String) weekNumber);
            } catch (NumberFormatException e) {
                this.weekNumber = null;
            }
        } else if (weekNumber instanceof Number) {
            this.weekNumber = ((Number) weekNumber).intValue();
        } else {
            this.weekNumber = null;
        }
    }
    
    // weekNumber 유효성 검사 및 기본값 설정
    public Integer getWeekNumber() {
        if (this.weekNumber == null || this.weekNumber <= 0) {
            return 1; // 기본값 반환
        }
        return this.weekNumber;
    }
    
    // 원본 weekNumber 값 반환 (null일 수 있음)
    public Integer getOriginalWeekNumber() {
        return this.weekNumber;
    }
    
    // weekTitle 유연한 설정을 위한 setter (Jackson이 사용할 메서드)
    @JsonSetter("weekTitle")
    public void setWeekTitle(Object weekTitle) {
        if (weekTitle == null) {
            this.weekTitle = null;
        } else if (weekTitle instanceof String) {
            this.weekTitle = (String) weekTitle;
        } else {
            this.weekTitle = weekTitle.toString();
        }
    }
    
    // weekTitle 유효성 검사 및 기본값 설정
    public String getWeekTitle() {
        if (this.weekTitle == null || this.weekTitle.trim().isEmpty()) {
            return null; // null 반환하여 검증에서 처리
        }
        return this.weekTitle.trim();
    }
    
    // 원본 weekTitle 값 반환 (null일 수 있음)
    public String getOriginalWeekTitle() {
        return this.weekTitle;
    }
    
    public String getWeekContent() {
        return this.weekContent;
    }
    
    public void setWeekContent(String weekContent) {
        this.weekContent = weekContent;
    }
    
    public String getSubjectId() {
        return this.subjectId;
    }
    
    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
    
    public String getSubDetailId() {
        return this.subDetailId;
    }
    
    public void setSubDetailId(String subDetailId) {
        this.subDetailId = subDetailId;
    }
    
    public List<String> getSubjectIds() {
        return this.subjectIds;
    }
    
    public void setSubjectIds(List<String> subjectIds) {
        this.subjectIds = subjectIds;
    }
    
    public List<String> getSubDetailIds() {
        return this.subDetailIds;
    }
    
    public void setSubDetailIds(List<String> subDetailIds) {
        this.subDetailIds = subDetailIds;
    }
} 