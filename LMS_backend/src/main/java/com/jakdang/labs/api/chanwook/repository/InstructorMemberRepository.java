package com.jakdang.labs.api.chanwook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.SubGroupEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface InstructorMemberRepository extends JpaRepository<MemberEntity, String> {
    
    // 강사가 담당하는 과정별 학생 목록 조회 (한 userId에 여러 memberId가 있는 경우 처리 + classCode, classId, classNumber 포함)
    @Query("SELECT c.courseId as courseId, c.courseName as courseName, " +
           "c.courseStartDay as courseStartDay, c.courseEndDay as courseEndDay, " +
           "c.courseDays as courseDays, c.startTime as startTime, c.endTime as endTime, " +
           "c.classId as classId, cl.classCode as classCode, cl.classNumber as classNumber, " +
           "COUNT(DISTINCT m.id) as totalStudents, " +
           "SUM(CASE WHEN da.attendanceStatus = '출석' THEN 1 ELSE 0 END) as presentCount, " +
           "SUM(CASE WHEN da.attendanceStatus = '결석' THEN 1 ELSE 0 END) as absentCount, " +
           "SUM(CASE WHEN da.attendanceStatus = '지각' THEN 1 ELSE 0 END) as lateCount " +
           "FROM CourseEntity c " +
           "LEFT JOIN ClassroomEntity cl ON c.classId = cl.classId " +
           "LEFT JOIN MemberEntity m ON c.courseId = m.courseId AND m.memberRole = 'ROLE_STUDENT' " +
           "LEFT JOIN AttendanceEntity da ON m.id = da.userId AND da.courseId = c.courseId AND da.lectureDate = :date " +
           "WHERE c.memberId IN (SELECT mem.memberId FROM MemberEntity mem WHERE mem.id = :userId) AND c.courseActive = 0 " +
           "GROUP BY c.courseId, c.courseName, c.courseStartDay, c.courseEndDay, c.courseDays, c.startTime, c.endTime, c.classId, cl.classCode, cl.classNumber")
    List<Map<String, Object>> findCourseAttendanceByUserId(
        @Param("userId") String userId, 
        @Param("date") LocalDate date);
    
    // 특정 과정의 출석 기록 조회
    @Query("SELECT m.id as userId, m.memberName as memberName, " +
           "da.attendanceStatus as status, " +
           "da.checkIn as checkInTime, da.checkOut as checkOutTime " +
           "FROM MemberEntity m " +
           "JOIN CourseEntity c ON m.courseId = c.courseId " +
           "LEFT JOIN AttendanceEntity da ON m.id = da.userId AND da.courseId = c.courseId AND da.lectureDate = :date " +
           "WHERE c.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' " +
           "ORDER BY m.memberName")
    List<Map<String, Object>> findAttendanceByCourseId(
        @Param("courseId") String courseId, 
        @Param("date") LocalDate date);
    
    // 학생 출석 이력 조회 (권한 확인 포함 + courseId 매칭 + memberId 필터링)
    @Query("SELECT da.lectureDate as date, c.courseName, da.courseId as courseId, " +
           "da.attendanceStatus as status, " +
           "da.checkIn as checkInTime, da.checkOut as checkOutTime " +
           "FROM AttendanceEntity da " +
           "JOIN MemberEntity m ON da.userId = m.id AND da.memberId = m.memberId " +
           "JOIN CourseEntity c ON da.courseId = c.courseId " +
           "WHERE m.id = :userId " +
           "AND da.courseId = m.courseId " +
           "AND c.memberId = (SELECT mem.memberId FROM MemberEntity mem WHERE mem.id = :instructorId) " +
           "ORDER BY da.lectureDate DESC")
    List<Map<String, Object>> findStudentAttendanceHistory(
        @Param("userId") String userId, 
        @Param("instructorId") String instructorId);
    
    // 학생 출석 이력 조회 (권한 확인 없음 - courseId 매칭 포함 + memberId 필터링)
    @Query("SELECT da.lectureDate as date, c.courseName, da.courseId as courseId, " +
           "da.attendanceStatus as status, " +
           "da.checkIn as checkInTime, da.checkOut as checkOutTime " +
           "FROM AttendanceEntity da " +
           "JOIN MemberEntity m ON da.userId = m.id AND da.memberId = m.memberId " +
           "JOIN CourseEntity c ON da.courseId = c.courseId " +
           "WHERE m.id = :userId " +
           "AND da.courseId = m.courseId " +
           "ORDER BY da.lectureDate DESC")
    List<Map<String, Object>> findStudentAttendanceHistoryWithoutAuth(@Param("userId") String userId);
    
    // 출석 기록 검색 (엑셀 내보내기용) - 한 userId에 여러 memberId가 있는 경우 처리
    @Query("SELECT m.id as userId, m.memberName as memberName, " +
           "da.courseId as courseId, c.courseName as courseName, da.lectureDate as date, da.attendanceStatus as status, " +
           "da.checkIn as checkInTime, da.checkOut as checkOutTime " +
           "FROM AttendanceEntity da " +
           "JOIN MemberEntity m ON da.userId = m.id " +
           "LEFT JOIN CourseEntity c ON da.courseId = c.courseId " +
           "WHERE c.memberId IN (SELECT mem.memberId FROM MemberEntity mem WHERE mem.id = :userId) " +
           "AND (:courseId IS NULL OR da.courseId = :courseId) " +
           "AND (:date IS NULL OR da.lectureDate = :date) " +
           "AND (:status IS NULL OR da.attendanceStatus = :status) " +
           "AND (:studentName IS NULL OR m.memberName LIKE %:studentName%) " +
           "ORDER BY da.lectureDate DESC, m.memberName")
    List<Map<String, Object>> searchAttendanceRecords(
        @Param("userId") String userId,
        @Param("courseId") String courseId,
        @Param("date") LocalDate date,
        @Param("status") String status,
        @Param("studentName") String studentName);
    
    // 강사별 총 학생 수 조회 (한 userId에 여러 memberId가 있는 경우 처리)
    @Query("SELECT COUNT(DISTINCT m.id) as totalStudents " +
           "FROM MemberEntity m " +
           "JOIN CourseEntity c ON m.educationId = c.educationId " +
           "WHERE c.memberId IN (SELECT mem.memberId FROM MemberEntity mem WHERE mem.id = :userId) AND m.memberRole = 'ROLE_STUDENT'")
    Long countStudentsByUserId(@Param("userId") String userId);
    
    // 학생 상세 정보 조회 (memberId 필터링 포함)
    @Query("SELECT m.id as userId, m.memberName, c.courseName, " +
           "AVG(CASE WHEN da.attendanceStatus = '출석' THEN 1.0 ELSE 0.0 END) * 100 as attendanceRate " +
           "FROM MemberEntity m " +
           "JOIN CourseEntity c ON m.courseId = c.courseId " +
           "LEFT JOIN AttendanceEntity da ON m.id = da.userId AND da.courseId = c.courseId AND da.memberId = m.memberId " +
           "WHERE m.id = :userId " +
           "GROUP BY m.id, m.memberName, c.courseName")
    List<Map<String, Object>> findStudentDetailById(@Param("userId") String userId);

    
    // 과정 ID 리스트로 학생 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.courseId IN :courseIds AND m.memberRole = :memberRole")
    List<MemberEntity> findByCourseIdInAndMemberRole(@Param("courseIds") List<String> courseIds, @Param("memberRole") String memberRole);

    // 사용자명으로 회원 조회
    Optional<MemberEntity> findByMemberName(String memberName);
    
    // 로그인용 ID로 회원 조회 (중복불가)
    Optional<MemberEntity> findById(String id);
    
    // memberId로 회원 조회
    Optional<MemberEntity> findByMemberId(String memberId);
    
    // id 필드로 회원 조회 (users 테이블의 id와 매칭)
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :id")
    List<MemberEntity> findByIdField(@Param("id") String id);
    
    // 이메일로 회원 조회
    Optional<MemberEntity> findByMemberEmail(String memberEmail);


    // id 컬럼(고유키)로 회원 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :id")
    Optional<MemberEntity> findByIdColumn(@Param("id") String id);

    // ===== 성적 관련 메서드들 =====
    
    // memberId로 ScoreStudentEntity 조회
    @Query("SELECT ss FROM ScoreStudentEntity ss WHERE ss.memberId = :memberId")
    List<ScoreStudentEntity> findScoreStudentsByMemberId(@Param("memberId") String memberId);
    
    // templateId로 TemplateEntity 조회
    @Query("SELECT t FROM TemplateEntity t WHERE t.templateId = :templateId")
    Optional<TemplateEntity> findTemplateById(@Param("templateId") String templateId);
    

    
    // memberId로 TemplateEntity 목록 조회
    @Query("SELECT t FROM TemplateEntity t WHERE t.memberId = :memberId")
    List<TemplateEntity> findTemplatesByMemberId(@Param("memberId") String memberId);
    
    // subGroupId로 SubGroupEntity 조회
    @Query("SELECT sg FROM SubGroupEntity sg WHERE sg.subGroupId = :subGroupId")
    Optional<SubGroupEntity> findSubGroupById(@Param("subGroupId") String subGroupId);
    
    // courseId로 SubGroupEntity 목록 조회
    @Query("SELECT sg FROM SubGroupEntity sg WHERE sg.courseId = :courseId")
    List<SubGroupEntity> findSubGroupsByCourseId(@Param("courseId") String courseId);
    
    // userId로 MemberEntity의 memberId, courseId 조회
    @Query("SELECT m.memberId, m.courseId FROM MemberEntity m WHERE m.id = :userId")
    Object[] findMemberInfoByUserId(@Param("userId") String userId);
    
    // courseId로 CourseEntity의 classId 조회
    @Query("SELECT c.classId FROM CourseEntity c WHERE c.courseId = :courseId")
    String findClassIdByCourseId(@Param("courseId") String courseId);

} 