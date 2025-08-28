package com.jakdang.labs.api.jaegyeom.registration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EduRegisterDto {
    private EducationData education;
    private MemberData member;
    private UserData user;

    @Data
    public static class EducationData {
        private String educationName;
        private String businessNumber;
        private String educationAddress;
        private String description;
        private String educationDetailAddress;
    }

    @Data
    public static class MemberData {
        private String memberName;
        private String memberPhone;
        private String memberEmail;
        private String password;
        private String memberBirthday;
        private String memberRole;
        private String courseId;
        private String educationId;
        private String profileFileId;
        private String memberAddress;
    }

    @Data
    public static class UserData {
        private String name;
        private String phone;
        private String email;
        private String password;
    }
}
