package com.jakdang.labs.api.jaegyeom.registration.dto;

import java.time.LocalDateTime;

import com.jakdang.labs.entity.MemberEntity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeopleRegisterDto {
    private String memberId; // 회원 ID

    private String id; // 사용자 ID

    private String password; // 비밀번호

    private String courseId; // 강의 ID

    private String educationId; // 교육과정 ID

    private String memberName; // 회원명

    private String memberPhone; // 전화번호

    private String memberEmail; // 이메일

    private String profileFileId; // 프로필 이미지

    private String memberBirthday; // 생년월일

    private String memberAddress; // 주소

    private String memberRole; // 권한

    private LocalDateTime memberExpired; // 만료일

    private String courseName; // 강의명

    public MemberEntity toEntity() {
        return MemberEntity.builder()
            .memberId(this.memberId)
            .id(this.id)
            .password(this.password)
            .courseId(this.courseId)
            .educationId(this.educationId)
            .memberName(this.memberName)
            .memberPhone(this.memberPhone)
            .memberEmail(this.memberEmail)
            .profileFileId(this.profileFileId)
            .memberBirthday(this.memberBirthday)
            .memberAddress(this.memberAddress)
            .memberRole(this.memberRole)
            .memberExpired(this.memberExpired)
            .build();
    }
}