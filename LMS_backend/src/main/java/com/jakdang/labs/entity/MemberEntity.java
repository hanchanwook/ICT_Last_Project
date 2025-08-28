package com.jakdang.labs.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 회원 정보 엔티티 */
@Entity
@Table(name="member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberEntity extends BaseEntity {
    
    @Id
    @Column(length = 100)
    private String memberId; // 회원 ID

    @Column(length = 100)
    private String id; // 사용자 ID

    @Column(length = 255)
    private String password; // 비밀번호

    @Column(length = 100)
    private String courseId; // 강의 ID

    @Column(length = 100)
    private String educationId; // 교육과정 ID

    @Column(length = 50, nullable = false)
    private String memberName; // 회원명

    @Column(length = 20)
    private String memberPhone; // 전화번호

    @Column(length = 100, nullable = false)
    private String memberEmail; // 이메일

    @Column(length = 100)
    private String profileFileId; // 프로필 이미지

    @Column(length = 20)
    private String memberBirthday; // 생년월일

    @Column(length = 200)
    private String memberAddress; // 주소

    @Column(length = 20, nullable = false)
    private String memberRole; // 권한

    @Column
    private LocalDateTime memberExpired; // 만료일

    // UUID 자동 생성
    @PrePersist
    public void generateUUID() {
        if (this.memberId == null) {
            this.memberId = UUID.randomUUID().toString();
        }
    }
    
    // Manual getters to fix Lombok compilation issues
    public String getMemberId() {
        return memberId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getMemberName() {
        return memberName;
    }
    
    public String getMemberEmail() {
        return memberEmail;
    }
    
    public String getMemberPhone() {
        return memberPhone;
    }
    
    public String getMemberRole() {
        return memberRole;
    }
    
    public String getCourseId() {
        return courseId;
    }
    
    public String getEducationId() {
        return educationId;
    }
    
    public String getProfileFileId() {
        return profileFileId;
    }
    
    public String getMemberBirthday() {
        return memberBirthday;
    }
    
    public String getMemberAddress() {
        return memberAddress;
    }
    
    public LocalDateTime getMemberExpired() {
        return memberExpired;
    }
}