package com.jakdang.labs.api.cottonCandy.course.dto;

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
public class RequestSubjectDTO {
    private String subjectId;
    private String subjectName;
    private String subjectInfo;
    private String educationId;
    
    private String subDetailId;
    private String userId;

  
}
