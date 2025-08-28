package com.jakdang.labs.api.chanwook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.QrCodeEntity;
import com.jakdang.labs.entity.AttendanceEntity;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.ClassroomEntity;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCodeEntity, String> {

    // 강의실별 QR코드 조회
    List<QrCodeEntity> findByClassId(String classId);
    
    // 강의실별 활성화된 QR코드 조회 (qrCodeActive = 0)
    List<QrCodeEntity> findByClassIdAndQrCodeActive(String classId, int qrCodeActive);
    
    // 강의실 정보와 함께 QR코드 조회 (JOIN FETCH)
    @Query("SELECT q FROM QrCodeEntity q JOIN FETCH q.classroom WHERE q.classId = :classId")
    List<QrCodeEntity> findByClassIdWithClassroom(@Param("classId") String classId);
    
    // 특정 강의실의 활성화된 QR 코드 조회
    @Query("SELECT q FROM QrCodeEntity q WHERE q.classId = :classroomId AND q.qrCodeActive = 1")
    List<QrCodeEntity> findActiveQrCodesByClassroomId(@Param("classroomId") String classroomId);
    
    // 특정 강의실의 QR 출석 현황 조회 (QR 코드 + 출석 기록)
    @Query("SELECT a FROM AttendanceEntity a " +
           "WHERE a.classId = :classroomId " +
           "ORDER BY a.lectureDate DESC, a.checkIn DESC")
    List<AttendanceEntity> findAttendanceByClassroomId(@Param("classroomId") String classroomId);
    
    // 특정 강의실의 출석 기록 조회 (Member 정보 포함)
    @Query("SELECT a, m.memberEmail, m.memberName FROM AttendanceEntity a " +
           "LEFT JOIN MemberEntity m ON a.userId = m.id " +
           "WHERE a.classId = :classroomId " +
           "ORDER BY a.lectureDate DESC, a.checkIn DESC")
    List<Object[]> findAttendanceWithMemberInfo(@Param("classroomId") String classroomId);
    
    // 강의실 ID로 QR 코드 삭제
    void deleteByClassId(String classId);
    
    // 강의실 ID로 과정 조회 (CourseEntity용)
    @Query("SELECT c FROM CourseEntity c WHERE c.classId = :classId")
    List<CourseEntity> findCoursesByClassId(@Param("classId") String classId);
    
    // 강의실 전체 목록 조회
    @Query("SELECT c FROM ClassroomEntity c ORDER BY c.classCode")
    List<ClassroomEntity> findAllClassrooms();
    
    // 과정 전체 목록 조회
    @Query("SELECT c FROM CourseEntity c ORDER BY c.courseName")
    List<CourseEntity> findAllCourses();
    
    // 강사 정보 조회 (memberId로)
    @Query("SELECT m.id, m.memberName, m.memberEmail FROM MemberEntity m WHERE m.memberId = :memberId")
    List<Object[]> findInstructorByMemberId(@Param("memberId") String memberId);
    
    // userId로 MemberEntity 조회 (QR 출석체크용 - 모든 필요한 정보 포함)
    @Query("SELECT m.memberRole, m.memberId, m.courseId, c.classId FROM MemberEntity m " +
           "LEFT JOIN CourseEntity c ON m.courseId = c.courseId " +
           "WHERE m.id = :userId")
    List<Object[]> findMemberById(@Param("userId") String userId);
    
    // userId로 학생의 모든 수강 과정 조회 (QR 출석체크용 - 여러 과정 지원)
    @Query("SELECT m.memberRole, m.memberId, m.courseId, c.classId FROM MemberEntity m " +
           "LEFT JOIN CourseEntity c ON m.courseId = c.courseId " +
           "WHERE m.id = :userId AND m.memberRole = 'ROLE_STUDENT'")
    List<Object[]> findStudentCoursesById(@Param("userId") String userId);
    
    // 강사별 강의실 조회 (userId로 강사가 담당하는 과정의 강의실만 조회)
    @Query("SELECT DISTINCT c FROM ClassroomEntity c " +
           "JOIN CourseEntity course ON c.classId = course.classId " +
           "JOIN MemberEntity m ON course.memberId = m.memberId " +
           "WHERE m.id = :userId AND course.courseActive = 0 " +
           "ORDER BY c.classCode")
    List<ClassroomEntity> findClassroomsByInstructorId(@Param("userId") String userId);
    
    // sessionId(QR 코드 ID)로 courseId와 classId 조회
    @Query("SELECT q.courseId, q.classId FROM QrCodeEntity q WHERE q.qrCodeId = :sessionId")
    Object[] findCourseAndClassBySessionId(@Param("sessionId") String sessionId);
    
    // 디버깅용: QR 코드 정보 전체 조회
    @Query("SELECT q FROM QrCodeEntity q WHERE q.qrCodeId = :sessionId")
    QrCodeEntity findQrCodeBySessionId(@Param("sessionId") String sessionId);
}
