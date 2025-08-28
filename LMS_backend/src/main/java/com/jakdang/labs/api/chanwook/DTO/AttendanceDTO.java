package com.jakdang.labs.api.chanwook.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDTO {
    
    private String attendanceId; // UUID
    private String userId; // 유저 ID
    private String courseId; // 과정 ID
    private LocalDate lectureDate; // 수업 일자
    private LocalDateTime lectureStartTime; // 수업 시작시간
    private LocalDateTime lectureEndTime; // 수업 종료시간
    private String attendanceStatus; // 출석, 지각, 결석, 공결
    private LocalDateTime checkIn; // 입실 시간
    private LocalDateTime checkOut; // 퇴실 시간
    private String officialReason; // 공결 사유
    private String note; // 특이사항
    private String memberEmail; // 학생 이메일
    private String memberName; // 학생 이름
    private String courseName; // 과정명
    private String classCode; // 강의실 코드
    
    // ========== 관리자용 편의 메서드들 ==========
    
    // 출석 상태 검증
    public boolean isPresent() {
        return "출석".equals(this.attendanceStatus);
    }
    
    public boolean isLate() {
        return "지각".equals(this.attendanceStatus);
    }
    
    public boolean isAbsent() {
        return "결석".equals(this.attendanceStatus);
    }
    
    public boolean isOfficialAbsence() {
        return "공결".equals(this.attendanceStatus);
    }
    
    public boolean isAttended() {
        return isPresent() || isLate();
    }
    
    // 수업 시간 계산
    public Duration getActualAttendanceTime() {
        if (checkIn != null && checkOut != null) {
            return Duration.between(checkIn, checkOut);
        }
        return null;
    }
    
    public String getAttendanceTimeFormatted() {
        Duration duration = getActualAttendanceTime();
        if (duration != null) {
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            return String.format("%d시간 %d분", hours, minutes);
        }
        return "미기록";
    }
    
    // 지각 시간 계산
    public Duration getLateTime() {
        if (checkIn != null && lectureStartTime != null) {
            if (checkIn.isAfter(lectureStartTime)) {
                return Duration.between(lectureStartTime, checkIn);
            }
        }
        return Duration.ZERO;
    }
    
    public String getLateTimeFormatted() {
        Duration lateTime = getLateTime();
        if (lateTime != null && !lateTime.isZero()) {
            long minutes = lateTime.toMinutes();
            return String.format("%d분 지각", minutes);
        }
        return "정시 출석";
    }
    
    // 출석 상태 업데이트 (관리자용)
    public void updateStatusWithReason(String newStatus, String reason) {
        String oldStatus = this.attendanceStatus;
        this.attendanceStatus = newStatus;
        
        // 변경 이력을 notes에 추가
        String updateNote = String.format(" | 상태변경: %s→%s (사유: %s, %s)", 
            oldStatus, newStatus, reason, 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
        
        this.note = (this.note != null ? this.note : "") + updateNote;
    }
    
    // 수동 출석 표시 추가
    public void markAsManualEntry(String inputBy) {
        String manualNote = String.format(" | 수동입력: %s (%s)", 
            inputBy, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
        
        this.note = (this.note != null ? this.note : "") + manualNote;
    }
    
    // 공결 처리
    public void markAsOfficialAbsence(String reason) {
        this.attendanceStatus = "공결";
        this.officialReason = reason;
        
        String officialNote = String.format(" | 공결처리: %s (%s)", 
            reason, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
        
        this.note = (this.note != null ? this.note : "") + officialNote;
    }
    
    // 출석 요약 정보 (관리자 대시보드용)
    public String getAttendanceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("상태: ").append(attendanceStatus);
        
        if (checkIn != null) {
            summary.append(" | 입실: ").append(checkIn.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        
        if (checkOut != null) {
            summary.append(" | 퇴실: ").append(checkOut.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        
        if (isLate() && !getLateTime().isZero()) {
            summary.append(" | ").append(getLateTimeFormatted());
        }
        
        return summary.toString();
    }
    
    // 경고 표시 여부 (관리자용)
    public boolean needsAttention() {
        // 지각이 30분 이상이거나 결석인 경우
        return isAbsent() || (isLate() && getLateTime().toMinutes() >= 30);
    }
    
    // 출석 점수 계산 (관리자용 - 출석률 계산에 사용)
    public double getAttendanceScore() {
        switch (attendanceStatus) {
            case "출석": return 1.0;
            case "지각": return 0.8;
            case "공결": return 1.0;
            case "결석": return 0.0;
            default: return 0.0;
        }
    }
    
    // 강의실 정보 추출 (notes에서)
    public String getClassroomInfo() {
        if (note != null && note.contains("강의실: ")) {
            int start = note.indexOf("강의실: ") + 5;
            int end = note.indexOf(",", start);
            if (end == -1) end = note.indexOf(" |", start);
            if (end == -1) end = note.length();
            return note.substring(start, end).trim();
        }
        return "미기록";
    }
    
    // 유효성 검증
    public boolean isValidAttendance() {
        return userId != null && 
               lectureDate != null && 
               attendanceStatus != null &&
               (checkIn != null || isAbsent());
    }
} 