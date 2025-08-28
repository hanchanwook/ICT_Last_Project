package com.jakdang.labs.api.gemjjok.util;

import com.jakdang.labs.api.gemjjok.DTO.WeeklyPlanRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class WeeklyPlanValidator {
    
    public void validateWeeklyPlanRequest(WeeklyPlanRequest request) {
        if (request.getWeekNumber() == null || request.getWeekNumber() <= 0) {
            throw new IllegalArgumentException("주차 번호는 1 이상이어야 합니다.");
        }
        
        if (!StringUtils.hasText(request.getWeekTitle())) {
            throw new IllegalArgumentException("주차 제목은 필수입니다.");
        }
        
        if (!StringUtils.hasText(request.getWeekContent())) {
            throw new IllegalArgumentException("주차 내용은 필수입니다.");
        }
        
        // 배열 데이터 검증
        if (request.getSubjectId() != null && !StringUtils.hasText(request.getSubjectId())) {
            request.setSubjectId(null);
        }
        
        if (request.getSubDetailId() != null && !StringUtils.hasText(request.getSubDetailId())) {
            request.setSubDetailId(null);
        }
    }
    
    public void validateWeeklyPlanRequests(List<WeeklyPlanRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("주차별 계획 데이터가 없습니다.");
        }
        
        // 중복 주차 번호 검사
        List<Integer> weekNumbers = requests.stream()
                .map(WeeklyPlanRequest::getWeekNumber)
                .toList();
        
        if (weekNumbers.size() != weekNumbers.stream().distinct().count()) {
            throw new IllegalArgumentException("중복된 주차 번호가 있습니다.");
        }
        
        // 각 요청 검증
        for (WeeklyPlanRequest request : requests) {
            validateWeeklyPlanRequest(request);
        }
    }
} 