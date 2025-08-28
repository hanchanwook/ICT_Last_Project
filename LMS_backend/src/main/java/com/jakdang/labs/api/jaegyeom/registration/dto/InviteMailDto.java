package com.jakdang.labs.api.jaegyeom.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InviteMailDto {
    private String email;
    private String name;
    private String educationId;
}
