package com.jakdang.labs.api.cottonCandy.course.dto;
import java.time.LocalDate;

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
public class ResponseStudentsListDTO {
    private String memberId;
    private String memberName;
    private String memberEmail;
    private String memberPhone;
    private LocalDate createdAt;
}
