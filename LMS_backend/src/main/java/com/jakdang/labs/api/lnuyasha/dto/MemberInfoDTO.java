package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 통합된 회원 정보 DTO
 * MemberInfoDTO, InstructorInfoDTO, CourseStudentDTO를 통합
 * 사용자 역할에 따라 필요한 필드만 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfoDTO {
    
    // 기본 회원 정보
    private String memberId;      // 회원 UUID
    private String educationId;   // 학원 ID
    private String memberName;    // 회원명 (강사명/학생명)
    private String memberRole;    // 권한 (STUDENT, INSTRUCTOR, ADMIN 등)
    private String memberEmail;   // 이메일
    private String id;            // member 테이블의 사용자 ID (로그인)
    
    // 강사 관련 정보 (InstructorInfoDTO에서 통합)
    private Integer questionCount;     // 등록한 문제 수 (강사인 경우)
    private String department;         // 소속 부서/학원
    private String profileImage;       // 프로필 이미지 URL
    private String lastActiveDate;     // 마지막 활동일
    
    // 학생 관련 정보 (CourseStudentDTO에서 통합)
    private String courseId;           // 과정 ID (학생인 경우)
    private LocalDateTime memberExpired; // 만료 여부 (null이면 활성)
    
    // 호환성을 위한 별칭 필드들
    private String studentName;        // 학생명 (memberName과 동일, 호환성)
    private String email;              // 이메일 (memberEmail과 동일, 호환성)
    private String instructorName;     // 강사명 (memberName과 동일, 호환성)
    private String instructorEmail;    // 강사 이메일 (memberEmail과 동일, 호환성)
    
    // Repository에서 사용하는 생성자
    public MemberInfoDTO(String memberId, String educationId, String memberName, String memberRole, String memberEmail, String id) {
        this.memberId = memberId;
        this.educationId = educationId;
        this.memberName = memberName;
        this.memberRole = memberRole;
        this.memberEmail = memberEmail;
        this.id = id;
        
        // 호환성을 위한 별칭 필드 설정
        this.studentName = memberName;
        this.email = memberEmail;
        this.instructorName = memberName;
        this.instructorEmail = memberEmail;
    }
    
    @Override
    public String toString() {
        return "MemberInfoDTO{" +
                "memberId='" + memberId + '\'' +
                ", educationId='" + educationId + '\'' +
                ", memberName='" + memberName + '\'' +
                ", memberRole='" + memberRole + '\'' +
                ", memberEmail='" + memberEmail + '\'' +
                ", id='" + id + '\'' +
                ", questionCount=" + questionCount +
                ", department='" + department + '\'' +
                ", profileImage='" + profileImage + '\'' +
                ", lastActiveDate='" + lastActiveDate + '\'' +
                ", courseId='" + courseId + '\'' +
                ", memberExpired=" + memberExpired +
                '}';
    }
} 

