package com.jakdang.labs.api.post.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MealPlanCreateDTO extends PostCreateDTO {
    private String mealDate;
    private String mealTime;
}
