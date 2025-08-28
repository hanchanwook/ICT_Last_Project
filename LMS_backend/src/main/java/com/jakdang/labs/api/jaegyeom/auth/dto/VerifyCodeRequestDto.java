package com.jakdang.labs.api.jaegyeom.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyCodeRequestDto {
    private String email;
    private String code;
}