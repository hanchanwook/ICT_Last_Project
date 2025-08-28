package com.jakdang.labs.api.lnuyasha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 통합된 과정 정보 DTO
 * StudentCourseDTO, MyCourseDTO, StaffCourseDTO, InstructorCourseDTO, CourseDetailDTO를 통합
 * 사용자 역할에 따라 필요한 필드만 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDTO {
    
    // 기본 과정 정보
    private String courseId;              // 과정 ID
    private String courseName;            // 과정명
    private String courseCode;            // 과정코드
    private String subGroupId;            // 서브그룹 ID (CourseDetailDTO)
    private String status;                // 과정 상태 (진행중, 완료, 예정)
    
    // 기간 정보
    private LocalDate courseStartDay;     // 과정 시작일
    private LocalDate courseEndDay;       // 과정 종료일
    private LocalTime startTime;          // 시작 시간 (StaffCourseDTO)
    private LocalTime endTime;            // 종료 시간 (StaffCourseDTO)
    private String courseDays;            // 수업 요일 (StaffCourseDTO)
    
    // 강사 정보 (StaffCourseDTO)
    private String memberId;              // 강사 ID
    private String memberName;            // 강사 이름
    
    // 수강생 정보
    private Integer studentCount;         // 수강생 수 (StaffCourseDTO)
    private Integer totalStudents;        // 전체 학생 수 (InstructorCourseDTO)
    private Integer completedStudents;    // 완료된 학생 수 (InstructorCourseDTO)
    private Integer dropoutStudents;      // 중도 탈락 학생 수 (InstructorCourseDTO)
    
    // 시험 정보
    private Integer totalExams;           // 전체 시험 수 (StudentCourseDTO)
    private Integer completedExams;       // 완료된 시험 수 (StudentCourseDTO)
    private Integer examCount;            // 시험 개수 (StaffCourseDTO)
    
    // 점수 정보
    private Integer totalScore;           // 총점 (StudentCourseDTO)
    private Double averageScore;          // 평균 점수 (모든 DTO)
    private String grade;                 // 등급 (StudentCourseDTO)
    private Integer passRate;             // 합격률 (InstructorCourseDTO)
    
    // 기타 정보
    private String educationId;           // 교육기관 ID (StaffCourseDTO)
    private Integer maxCapacity;          // 최대 인원 (StaffCourseDTO)
    private Integer minCapacity;          // 최소 인원 (StaffCourseDTO)
    private String classId;               // 강의실 ID (StaffCourseDTO)
    private String classNumber;           // 강의실 번호 (StaffCourseDTO)
    private Integer courseActive;         // 과정 활성화 상태 (StaffCourseDTO)
    private LocalDateTime createdAt;      // 생성일시 (StudentCourseDTO)
    
    // 연결된 데이터
    private List<CourseSubjectDTO> subjects;  // 과정에 연결된 과목 목록 (MyCourseDTO)
    private List<ExamDTO> exams;              // 해당 과정의 시험 목록 (StudentCourseDTO)
    
    // 호환성을 위한 별칭 필드들
    private String courseStartDayStr;     // 문자열 형태 시작일 (InstructorCourseDTO)
    private String courseEndDayStr;       // 문자열 형태 종료일 (InstructorCourseDTO)
} 