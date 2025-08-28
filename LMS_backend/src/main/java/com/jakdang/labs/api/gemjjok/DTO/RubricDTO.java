package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricDTO {
    private String assignmentId;
    private String rubricTitle;
    private Integer totalScore;
    private List<RubricItemDTO> rubricitem;
} 