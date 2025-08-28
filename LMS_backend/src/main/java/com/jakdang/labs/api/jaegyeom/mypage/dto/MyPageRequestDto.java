package com.jakdang.labs.api.jaegyeom.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyPageRequestDto {
    private String name;
    private String email;
    private String userId;
    private String educationId;
}