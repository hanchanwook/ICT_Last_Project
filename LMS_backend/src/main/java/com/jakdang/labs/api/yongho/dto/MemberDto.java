package com.jakdang.labs.api.yongho.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * member 테이블 매핑용 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDto {

    /**
     * PK: 회원 ID (NOT NULL)
     */
    @NotBlank
    String memberId;

    /**
     * 사용자 ID (NOT NULL)
     */
    @NotBlank
    String id;

    /**
     * 강의 ID (NULL 허용)
     */
    String courseId;

    /**
     * 교육과정 ID (NOT NULL)
     */
    @NotBlank
    String educationId;

    /**
     * 회원명 (NOT NULL)
     */
    @NotBlank
    String memberName;

    /**
     * 전화번호 (NOT NULL)
     */
    @NotBlank
    String memberPhone;

    /**
     * 이메일 (NOT NULL)
     */
    @NotBlank
    @Email
    String memberEmail;

    /**
     * 프로필 이미지 (NOT NULL)
     */
    @NotBlank
    String profileFileId;

    /**
     * 생년월일 (NOT NULL)
     */
    @NotBlank
    String memberBirthday;

    /**
     * 주소 (NOT NULL)
     */
    @NotBlank
    String memberAddress;

    /**
     * 권한 (NOT NULL)
     */
    @NotBlank
    String memberRole;

    /**
     * 계정 생성일시 (TIMESTAMP)
     */
    LocalDate createdAt;

    /**
     * 계정 수정일시 (TIMESTAMP, NULL 허용)
     */
    LocalDate updatedAt;

    /**
     * 만료일 (TIMESTAMP, NULL 허용)
     */
    LocalDateTime memberExpired;
}
