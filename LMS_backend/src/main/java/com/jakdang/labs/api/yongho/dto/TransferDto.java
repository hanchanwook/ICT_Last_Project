package com.jakdang.labs.api.yongho.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * transfer 테이블 매핑용 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferDto {

    /** PK: 매핑 ID */
    @NotBlank
    String tId;

    /** FK: 권한 그룹 ID (permissionmanagement.pmId) */
    @NotNull
    Integer pmId;

    /** FK: 사용자 ID (users.id 혹은 memberId) */
    @NotBlank
    String memberId;

    /** 매핑 생성일시 */
    @NotNull
    Instant createdAt;
    
    // Manual getters to fix Lombok compilation issues
    public String getTId() {
        return tId;
    }
    
    public Integer getPmId() {
        return pmId;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}

