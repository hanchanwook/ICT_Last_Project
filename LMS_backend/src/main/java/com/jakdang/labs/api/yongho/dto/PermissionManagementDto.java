package com.jakdang.labs.api.yongho.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 권한 그룹(permissionmanagement 테이블)을 전송하기 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionManagementDto {

    /** PK: 권한 그룹 ID */
    @NotNull
    Integer pmId;

    /** 권한명 */
    @NotBlank
    String permissionName;

    /** 권한 설명 */
    @NotBlank
    String permissionText;

    /** 권한 타입 */
    @NotBlank
    String type;

    /** 생성일시 */
    @NotNull
    LocalDate createdAt;

    /** 수정일시 (선택) */
    LocalDate updatedAt;

    /** 상위 권한 ID (선택) */
    Integer selfFk;
    
    // Manual getters to fix Lombok compilation issues
    public Integer getPmId() {
        return pmId;
    }
    
    public String getPermissionName() {
        return permissionName;
    }
    
    public String getPermissionText() {
        return permissionText;
    }
    
    public String getType() {
        return type;
    }
    
    public Integer getSelfFk() {
        return selfFk;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
}
