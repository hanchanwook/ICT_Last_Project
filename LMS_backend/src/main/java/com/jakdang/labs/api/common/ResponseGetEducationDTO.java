package com.jakdang.labs.api.common;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseGetEducationDTO {
    private String educationId;
    private String userId;
    
}
