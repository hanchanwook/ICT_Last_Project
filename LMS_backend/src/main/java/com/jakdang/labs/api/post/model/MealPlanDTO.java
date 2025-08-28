package com.jakdang.labs.api.post.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class MealPlanDTO extends PostDTO {
    private String mealDate; // 식단표 날짜
    private String mealTime;    // 식단 시간 (아침, 점심, 저녁 등)
}
