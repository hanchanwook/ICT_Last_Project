package com.jakdang.labs.api.jaegyeom.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAndEduDto {

    private String name;
    private String email;
    private String role;
    private String educationId;
    private String educationName;
    private String phone;
    
}
