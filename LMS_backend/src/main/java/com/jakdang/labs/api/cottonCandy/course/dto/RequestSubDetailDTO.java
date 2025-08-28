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
public class RequestSubDetailDTO {
    private String subDetailId;
    private String subDetailName;
    private String subDetailInfo;
    private String userId;

    
    
    
}
