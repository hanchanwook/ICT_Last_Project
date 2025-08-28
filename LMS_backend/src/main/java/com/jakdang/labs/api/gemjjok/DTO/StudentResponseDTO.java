package com.jakdang.labs.api.gemjjok.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDTO {
    private String studentId;
    private String studentName;
    private String studentCode;
    private String email;
    private String phone;
} 