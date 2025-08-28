package com.jakdang.labs.api.chanwook.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jakdang.labs.entity.MemberEntity;

@Repository
public interface MemberManagementRepository extends JpaRepository<MemberEntity, String> {
    
    // 학생 권한으로 조회 (memberRole = "ROLE_STUDENT")
    List<MemberEntity> findByMemberRole(String memberRole);
    
    // 학생 이름으로 검색
    List<MemberEntity> findByMemberNameContainingAndMemberRole(String memberName, String memberRole);
    
    // 학생 이메일로 검색
    List<MemberEntity> findByMemberEmailContainingAndMemberRole(String memberEmail, String memberRole);
    
    // 학생 전화번호로 검색
    List<MemberEntity> findByMemberPhoneContainingAndMemberRole(String memberPhone, String memberRole);
    
    // 과정별 학생 조회
    List<MemberEntity> findByCourseIdAndMemberRole(String courseId, String memberRole);
    
    // 특정 학생 상세 조회 (과정명 포함)
    @Query("SELECT m, c.courseName FROM MemberEntity m " +
           "LEFT JOIN com.jakdang.labs.entity.CourseEntity c ON m.courseId = c.courseId " +
           "WHERE m.id = :userId AND m.memberRole = :memberRole")
    Optional<Object[]> findByUserIdAndMemberRoleWithCourse(@Param("userId") String userId, @Param("memberRole") String memberRole);
    
    // 특정 학생 상세 조회 (단순 조회)
    Optional<MemberEntity> findByIdAndMemberRole(String userId, String memberRole);
    
    // 학생 존재 여부 확인
    boolean existsByIdAndMemberRole(String userId, String memberRole);
    
    // 활성 학생 조회 (과정명 포함)
    @Query("SELECT m, c.courseName FROM MemberEntity m " +
           "LEFT JOIN com.jakdang.labs.entity.CourseEntity c ON m.courseId = c.courseId " +
           "WHERE m.memberRole = 'ROLE_STUDENT' AND " +
           "(m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP)")
    List<Object[]> findActiveMembersWithCourseName();
    
    // 활성 회원 조회 (모든 역할, 과정명 포함)
    @Query("SELECT m, c.courseName FROM MemberEntity m " +
           "LEFT JOIN com.jakdang.labs.entity.CourseEntity c ON m.courseId = c.courseId " +
           "WHERE (m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP)")
    List<Object[]> findActiveAllMembersWithCourseName();
    
    // 활성 학생 조회 (과정명 포함, educationId 조건 추가)
    @Query("SELECT m, c.courseName FROM MemberEntity m " +
           "LEFT JOIN com.jakdang.labs.entity.CourseEntity c ON m.courseId = c.courseId " +
           "WHERE m.memberRole = 'ROLE_STUDENT' AND " +
           "(m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP) AND " +
           "m.educationId = :educationId")
    List<Object[]> findActiveMembersWithCourseNameByEducationId(@Param("educationId") String educationId);
    
    // 활성 회원 조회 (모든 역할, 과정명 포함, educationId 조건 추가)
    @Query("SELECT m, c.courseName FROM MemberEntity m " +
           "LEFT JOIN com.jakdang.labs.entity.CourseEntity c ON m.courseId = c.courseId " +
           "WHERE (m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP) AND " +
           "m.educationId = :educationId")
    List<Object[]> findActiveAllMembersWithCourseNameByEducationId(@Param("educationId") String educationId);
    
    // 과정명 조회 (활성화된 과정만)
    @Query("SELECT c.courseName FROM com.jakdang.labs.entity.CourseEntity c WHERE c.courseId = :courseId AND c.courseActive = 0")
    Optional<String> findCourseNameByCourseId(@Param("courseId") String courseId);
    
    // 과정 정보 조회 (과정명, 최대 수용 인원 포함)
    @Query("SELECT c.courseName, c.maxCapacity FROM com.jakdang.labs.entity.CourseEntity c WHERE c.courseId = :courseId AND c.courseActive = 0")
    Optional<Object[]> findCourseInfoByCourseId(@Param("courseId") String courseId);
    
    // 특정 학생의 특정 과정 조회 (비활성화용)
    Optional<MemberEntity> findByIdAndCourseIdAndMemberRole(String userId, String courseId, String memberRole);
    
    // 강사의 memberId로 담당하는 과정 조회
    @Query("SELECT c.courseId as courseId, c.courseName as courseName FROM com.jakdang.labs.entity.CourseEntity c " +
           "WHERE c.memberId = :memberId AND c.courseActive = 0")
    List<Map<String, Object>> findCoursesByMemberId(@Param("memberId") String memberId);
    
    // 모든 과정 조회 (educationId 포함)
    @Query("SELECT c.courseId as courseId, c.courseName as courseName, c.educationId as educationId " +
           "FROM com.jakdang.labs.entity.CourseEntity c WHERE c.courseActive = 0")
    List<Map<String, Object>> findAllCourses();
    
    // 특정 과정의 현재 등록된 학생 수 조회
    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT'")
    Long countStudentsByCourseId(@Param("courseId") String courseId);
} 