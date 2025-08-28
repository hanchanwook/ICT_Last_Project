package com.jakdang.labs.api.auth.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Valid
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnsSignInRequest {
    @Builder.Default
    private String type = "local";
    private String accessToken;
    private UserLoginRequest userInfo;
    private String userAgent;
}
