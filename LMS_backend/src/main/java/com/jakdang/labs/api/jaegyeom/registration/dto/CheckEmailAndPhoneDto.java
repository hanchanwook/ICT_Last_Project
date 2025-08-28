package com.jakdang.labs.api.jaegyeom.registration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckEmailAndPhoneDto {
    private String memberEmail;
    private String memberPhone;
}
