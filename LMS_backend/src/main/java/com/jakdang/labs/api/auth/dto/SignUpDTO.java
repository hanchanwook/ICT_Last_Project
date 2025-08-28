package com.jakdang.labs.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpDTO {
    private String id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String birth;
    @Builder.Default
    private String provider = "local";
    private String role; // 역할 추가

}