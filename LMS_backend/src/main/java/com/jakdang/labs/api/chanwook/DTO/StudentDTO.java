package com.jakdang.labs.api.chanwook.DTO;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDTO {
    
    private String userId;        // 회원 ID
    private String id;              // 사용자 ID
    private String courseId;        // 강의 ID
    private String educationId;     // 교육과정 ID
    private String memberName;      // 회원명
    private String memberPhone;     // 전화번호
    private String memberEmail;     // 이메일
    private String profileFileId;   // 프로필 이미지
    private String memberBirthday;  // 생년월일
    private String memberAddress;   // 주소
    private String memberRole;      // 권한
    private LocalDate memberExpired; // 만료일
    
    // 추가 필드 (프론트엔드 요구사항)
    private String courseName;      // 과정명
    private String status;          // 상태 (재학중, 퇴학 등)
    private String emergencyContact; // 비상연락처
    private LocalDate enrollmentDate; // 입학일
    private LocalDate updatedAt; // 수정일
} 