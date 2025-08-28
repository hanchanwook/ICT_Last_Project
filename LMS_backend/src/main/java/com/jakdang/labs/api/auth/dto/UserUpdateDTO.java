package com.jakdang.labs.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateDTO  {

    private String name;
    private String nickname;
    private String email;
    private String image;
    private String bio;

    // 필요시 필드 추가
    private Boolean activated;
    private String role;

}
