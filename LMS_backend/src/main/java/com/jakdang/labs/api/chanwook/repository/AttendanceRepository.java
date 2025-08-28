package com.jakdang.labs.api.chanwook.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.AttendanceEntity;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, String> {
    
    // AttendanceService에서 사용하는 메서드들
    List<AttendanceEntity> findByUserId(String userId);
    List<AttendanceEntity> findByUserIdAndLectureDate(String userId, LocalDate lectureDate);
    
    // member 테이블과 조인하여 이메일 포함한 전체 출석 기록 조회
    @Query("SELECT DISTINCT a, m.memberEmail, m.memberName, c.courseName FROM AttendanceEntity a " +
           "LEFT JOIN MemberEntity m ON a.userId = m.id " +
           "LEFT JOIN CourseEntity c ON a.courseId = c.courseId " +
           "ORDER BY a.lectureDate DESC, a.checkIn DESC")
    List<Object[]> findAllWithMemberAndCourseInfo();
    
    // member 테이블과 조인하여 이메일 포함한 전체 출석 기록 조회 (educationId 조건 추가)
    @Query("SELECT DISTINCT a, m.memberEmail, m.memberName, c.courseName FROM AttendanceEntity a " +
           "LEFT JOIN MemberEntity m ON a.userId = m.id " +
           "LEFT JOIN CourseEntity c ON a.courseId = c.courseId " +
           "WHERE m.educationId = :educationId " +
           "ORDER BY a.lectureDate DESC, a.checkIn DESC")
    List<Object[]> findAllWithMemberAndCourseInfoByEducationId(@Param("educationId") String educationId);
    
    // 특정 사용자의 출석 기록을 조인 쿼리로 조회 (courseName, memberName, classCode 포함) - 중복 제거 및 memberId 필터링 추가
    @Query("SELECT DISTINCT a, m.memberEmail, m.memberName, c.courseName, cl.classCode FROM AttendanceEntity a " +
           "JOIN MemberEntity m ON a.userId = m.id AND a.memberId = m.memberId " +
           "JOIN CourseEntity c ON a.courseId = c.courseId " +
           "LEFT JOIN ClassroomEntity cl ON a.classId = cl.classId " +
           "WHERE a.userId = :userId " +
           "ORDER BY a.lectureDate DESC, a.checkIn DESC")
    List<Object[]> findByUserIdWithMemberAndCourseInfo(@Param("userId") String userId);
    
    // 사용자의 classId 조회 (최근 출석 기록에서)
    @Query("SELECT a.classId FROM AttendanceEntity a " +
           "WHERE a.userId = :userId " +
           "ORDER BY a.lectureDate DESC, a.checkIn DESC " +
           "LIMIT 1")
    String findLatestClassIdByUserId(@Param("userId") String userId);
    
    // 특정 사용자의 특정 날짜, 특정 과정 출석 기록 조회
    AttendanceEntity findByUserIdAndLectureDateAndCourseId(String userId, LocalDate lectureDate, String courseId);
    
    // memberId와 courseId로 특정 날짜 출석 기록 조회
    List<AttendanceEntity> findByMemberIdAndCourseIdAndLectureDate(String memberId, String courseId, LocalDate lectureDate);
    
    // userId와 courseId로 출석 기록 조회
    List<AttendanceEntity> findByUserIdAndCourseId(String userId, String courseId);
    


}
