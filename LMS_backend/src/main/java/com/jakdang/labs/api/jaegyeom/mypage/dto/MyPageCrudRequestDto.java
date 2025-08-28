package com.jakdang.labs.api.jaegyeom.mypage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyPageCrudRequestDto {
    private String name;
    private String email;
    private String phone;
    private String password;        // 새 비밀번호

    @JsonProperty("address")
    private String memberAddress;   // member 전용
}
