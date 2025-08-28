package com.jakdang.labs.api.chanwook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jakdang.labs.entity.CourseEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface InstructorCourseRepository extends JpaRepository<CourseEntity, String> {
    
    // 강사가 담당하는 과정 목록 조회 (Entity 반환)
    List<CourseEntity> findByMemberId(String memberId);
    
    // 강사가 담당하는 과정 목록 조회 (Map 반환)
    @Query("SELECT c.courseId as courseId, c.courseName as courseName " +
           "FROM CourseEntity c " +
           "WHERE c.memberId = :memberId AND c.courseActive = 0")
    List<Map<String, Object>> findCoursesByMemberId(@Param("memberId") String memberId);
    
    // 과정별 학생 수 조회
    @Query("SELECT COUNT(m.id) as studentCount " +
           "FROM MemberEntity m " +
           "WHERE m.educationId = :educationId AND m.memberRole = 'ROLE_STUDENT'")
    Long countStudentsByCourseId(@Param("educationId") String educationId);
    
    // 과정별 출석률 조회
    @Query("SELECT AVG(CASE WHEN da.attendanceStatus = '출석' THEN 1.0 ELSE 0.0 END) * 100 as attendanceRate " +
           "FROM AttendanceEntity da " +
           "JOIN MemberEntity m ON da.userId = m.id " +
           "WHERE da.courseId IN (SELECT c.courseId FROM CourseEntity c WHERE c.educationId = :educationId)")
    Double getAttendanceRateByCourseId(@Param("educationId") String educationId);
    
  
    // 강의실 ID로 과정 조회
    List<CourseEntity> findByClassId(String classId);
} 