package com.jakdang.labs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "dailyattendance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEntity {
    
    @Id
    @Column(name = "attendanceId")
    private String attendanceId; // UUID
    
    @Column(name = "userId", nullable = false, length = 100)
    private String userId; // 유저 ID
    
    @Column(name = "lectureDate", nullable = false)
    private LocalDate lectureDate; // 수업 일자
    
    @Column(name = "lectureStartTime")
    private LocalDateTime lectureStartTime; // 수업 시작시간
    
    @Column(name = "lectureEndTime")
    private LocalDateTime lectureEndTime; // 수업 종료시간
    
    @Column(name = "attendanceStatus")
    private String attendanceStatus; // 출석, 지각, 결석, 공결
    
    @Column(name = "checkIn")
    private LocalDateTime checkIn; // 입실 시간
    
    @Column(name = "checkOut")
    private LocalDateTime checkOut; // 퇴실 시간
    
    @Column(name = "officialReason")
    private String officialReason; // 공결 사유
    
    @Column(name = "note")
    private String note; // 특이사항

    @Column(name = "createdAt")
    private LocalDateTime createdAt; // 생성 시간
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt; // 수정 시간

    @Column(name = "classId", nullable = true, length = 255)
    private String classId; // 강의실 ID

    @Column(name = "courseId", nullable = true, length = 255)
    private String courseId; // 과정 ID

    @Column(name = "memberId", nullable = true, length = 255)
    private String memberId; // 멤버 ID

    
    // UUID 자동 생성 및 시간 설정
    @PrePersist
    public void prePersist() {
        if (this.attendanceId == null) {
            this.attendanceId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        // updatedAt은 NULL 허용이므로 조건부로만 설정
        this.updatedAt = LocalDateTime.now();
    }
    
    // 수정 시간 자동 설정
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

} 