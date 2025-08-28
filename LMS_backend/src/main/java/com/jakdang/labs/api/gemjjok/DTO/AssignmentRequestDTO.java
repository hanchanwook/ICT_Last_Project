package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequestDTO {
    private String courseId;
    private String assignmentTitle;
    private String assignmentContent;
    private LocalDate dueDate;
    private Boolean fileRequired;
    private Boolean codeRequired;
    private String memberId; // 강사 ID
    
    // 루브릭 관련 필드 (프론트와 1:1로 맞춤)
    private List<RubricItemDTO> rubricitem;
    private String rubricTitle; // 옵션
} 