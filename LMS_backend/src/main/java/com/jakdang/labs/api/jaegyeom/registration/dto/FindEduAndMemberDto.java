package com.jakdang.labs.api.jaegyeom.registration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FindEduAndMemberDto {
    private String educationId;
    private String educationName;
    private String memberEmail;
    private String memberName;
    private String businessNumber;
    private String description;
    private String memberAddress;
    private String educationAddress;
    private String educationDetailAddress;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
