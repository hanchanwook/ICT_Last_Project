package com.jakdang.labs.api.jaegyeom.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCheckRequestDto {
    private String name;
    private String email;
}
