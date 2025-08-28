package com.jakdang.labs.api.lnuyasha.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.CourseEntity;

@Repository
public interface KyCourseRepository extends JpaRepository<CourseEntity, String> {
    
    // 특정 강사의 강의 목록 조회 (memberId로)
    @Query("SELECT c.courseId, m.memberName, c.courseCode, c.courseName, c.maxCapacity, c.minCapacity,  "+
           "cl.classNumber, c.courseStartDay, c.courseEndDay, c.courseDays, c.startTime, c.endTime, " +
           "s.subjectId, s.subjectName, s.subjectInfo " +
           "FROM CourseEntity c " +
           "LEFT JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "LEFT JOIN com.jakdang.labs.entity.SubjectEntity s ON s.subjectId = sg.subjectId " +
           "LEFT JOIN com.jakdang.labs.entity.ClassroomEntity cl ON c.classId = cl.classId " +
           "LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
           "WHERE c.courseActive = 0 AND c.memberId = :memberId")
    List<Object[]> findMyCoursesByMemberId(@Param("memberId") String memberId);
    
    // 특정 강사의 강의 목록 조회 (memberId와 educationId로 필터링)
    @Query("SELECT c.courseId, m.memberName, c.courseCode, c.courseName, c.maxCapacity, c.minCapacity,  "+
           "cl.classNumber, c.courseStartDay, c.courseEndDay, c.courseDays, c.startTime, c.endTime, " +
           "s.subjectId, s.subjectName, s.subjectInfo " +
           "FROM CourseEntity c " +
           "LEFT JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "LEFT JOIN com.jakdang.labs.entity.SubjectEntity s ON s.subjectId = sg.subjectId " +
           "LEFT JOIN com.jakdang.labs.entity.ClassroomEntity cl ON c.classId = cl.classId " +
           "LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
           "WHERE c.courseActive = 0 AND c.memberId = :memberId AND c.educationId = :educationId")
    List<Object[]> findMyCoursesByMemberIdAndEducationId(@Param("memberId") String memberId, @Param("educationId") String educationId);
    

    
    // 특정 강사의 강의 목록 조회 (userId로 - Course.memberId = Member.id)
    @Query("SELECT c.courseId, m.memberName, c.courseCode, c.courseName, c.maxCapacity, c.minCapacity,  "+
           "cl.classNumber, c.courseStartDay, c.courseEndDay, c.courseDays, c.startTime, c.endTime, " +
           "s.subjectId, s.subjectName, s.subjectInfo " +
           "FROM CourseEntity c " +
           "LEFT JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "LEFT JOIN com.jakdang.labs.entity.SubjectEntity s ON s.subjectId = sg.subjectId " +
           "LEFT JOIN com.jakdang.labs.entity.ClassroomEntity cl ON c.classId = cl.classId " +
           "LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.id " +
           "WHERE c.courseActive = 0 AND m.id = :userId")
    List<Object[]> findMyCoursesByUserId(@Param("userId") String userId);
    
    /**
     * educationId로 강의 목록 조회
     * @param educationId 학원 ID
     * @return 해당 학원의 강의 목록
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.educationId = :educationId AND c.courseActive = 0")
    List<CourseEntity> findByEducationId(@Param("educationId") String educationId);
    
    /**
     * educationId로 강의 목록 조회 (상세 정보 포함)
     * @param educationId 학원 ID
     * @return 해당 학원의 강의 목록 (강사, 교실, 과목 정보 포함)
     */
    @Query("SELECT c.courseId, m.memberName, c.courseCode, c.courseName, c.maxCapacity, c.minCapacity,  "+
           "cl.classNumber, c.courseStartDay, c.courseEndDay, c.courseDays, c.startTime, c.endTime, " +
           "s.subjectId, s.subjectName, s.subjectInfo " +
           "FROM CourseEntity c " +
           "LEFT JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "LEFT JOIN com.jakdang.labs.entity.SubjectEntity s ON s.subjectId = sg.subjectId " +
           "LEFT JOIN com.jakdang.labs.entity.ClassroomEntity cl ON c.classId = cl.classId " +
           "LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
           "WHERE c.courseActive = 0 AND c.educationId = :educationId")
    List<Object[]> findCoursesByEducationId(@Param("educationId") String educationId);
    
    /**
     * 간단한 과정 조회 (디버깅용)
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId AND c.educationId = :educationId AND c.courseActive = 0")
    List<CourseEntity> findSimpleCoursesByMemberAndEducation(@Param("memberId") String memberId, @Param("educationId") String educationId);
    
    /**
     * 디버깅용 - 전체 과정 조회
     */
    @Query("SELECT c FROM CourseEntity c")
    List<CourseEntity> findAllCourses();
    
    /**
     * 디버깅용 - 특정 memberId의 모든 과정 조회 (courseActive 무관)
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId")
    List<CourseEntity> findAllCoursesByMemberId(@Param("memberId") String memberId);
    
    /**
     * 디버깅용 - 특정 educationId의 모든 과정 조회 (courseActive 무관)
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.educationId = :educationId")
    List<CourseEntity> findAllCoursesByEducationId(@Param("educationId") String educationId);
    
    /**
     * 특정 교육 ID와 활성 상태로 과정 조회
     * @param educationId 교육 ID
     * @param courseActive 과정 활성 상태
     * @return 과정 목록
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.educationId = :educationId AND c.courseActive = :courseActive")
    List<Object[]> findByEducationIdAndCourseActive(@Param("educationId") String educationId, @Param("courseActive") int courseActive);
    
    /**
     * 학생이 수강 중인 과정 목록 조회
     * @param memberId 학생 ID
     * @param courseActive 과정 활성 상태
     * @return 과정 목록
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId AND c.courseActive = :courseActive")
    List<CourseEntity> findByMemberIdAndCourseActive(@Param("memberId") String memberId, @Param("courseActive") int courseActive);
    
    /**
     * 학생이 수강 중인 과정의 소그룹 ID 목록 조회
     * @param memberId 학생 ID
     * @return 소그룹 ID 목록
     */
    @Query("SELECT DISTINCT sg.subGroupId FROM CourseEntity c " +
           "JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "JOIN com.jakdang.labs.entity.MemberEntity m ON m.courseId = c.courseId " +
           "WHERE m.memberId = :memberId AND c.courseActive = 0")
    List<String> findSubGroupIdsByMemberId(@Param("memberId") String memberId);
    
    /**
     * 학생이 수강 중인 과정 목록 조회 (학생 memberId로)
     * @param memberId 학생 memberId
     * @return 학생이 수강 중인 과정 목록
     */
    @Query("SELECT DISTINCT c FROM CourseEntity c " +
           "JOIN com.jakdang.labs.entity.MemberEntity m ON m.courseId = c.courseId " +
           "WHERE m.memberId = :memberId AND c.courseActive = 0")
    List<CourseEntity> findCoursesByStudentMemberId(@Param("memberId") String memberId);
    
    /**
     * 사용자 ID와 member 테이블의 id가 동일한 과정 목록 조회 (여러 과정 지원)
     * @param userId 사용자 ID (member.id와 동일)
     * @return 해당 사용자가 수강 중인 과정 목록
     */
    @Query("SELECT DISTINCT c FROM CourseEntity c " +
           "JOIN com.jakdang.labs.entity.MemberEntity m ON c.courseId = m.courseId " +
           "WHERE m.id = :userId AND c.courseActive = 0")
    List<CourseEntity> findCoursesByUserId(@Param("userId") String userId);
    
    /**
     * 과정 ID 목록으로 소그룹 ID 목록 조회
     * @param courseIds 과정 ID 목록
     * @return 소그룹 ID 목록
     */
    @Query("SELECT DISTINCT sg.subGroupId FROM CourseEntity c " +
           "JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "WHERE c.courseId IN :courseIds AND c.courseActive = 0")
    List<String> findSubGroupIdsByCourseIds(@Param("courseIds") List<String> courseIds);
    
    /**
     * 강사가 담당하는 과정 목록 조회 (생성일시 내림차순 정렬)
     * @param memberId 강사 ID
     * @return 과정 목록
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId ORDER BY c.createdAt DESC")
    List<CourseEntity> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") String memberId);
} 