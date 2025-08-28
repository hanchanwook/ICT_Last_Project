package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseListRepository extends JpaRepository<CourseEntity, String> {
    // 사용하지 않는 메서드들 - users.id → email → member → courseId → course 체인으로 대체됨
    // @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId")
    // List<CourseEntity> findByMemberId(@Param("memberId") String memberId);
    
    // @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId AND c.courseActive = 1")
    // List<CourseEntity> findActiveCoursesByMemberId(@Param("memberId") String memberId);
    
    Optional<CourseEntity> findByCourseIdAndCourseActive(String courseId, int courseActive);
    
    @Query("SELECT c FROM CourseEntity c WHERE c.courseActive = 1")
    List<CourseEntity> findAllActiveCourses();
    
    // users.id로 email을 찾고, member.memberEmail로 학생 row들을 찾고, 각 row의 courseId로 course 테이블에서 강의 정보를 모두 조회
    @Query("SELECT c FROM CourseEntity c WHERE c.courseId IN (SELECT m.courseId FROM MemberEntity m WHERE m.memberEmail = (SELECT u.email FROM com.jakdang.labs.api.auth.entity.UserEntity u WHERE u.id = :userId) AND m.memberRole = 'ROLE_STUDENT' AND m.courseId IS NOT NULL)")
    List<CourseEntity> findCoursesByStudentId(@Param("userId") String userId);

    // 강사 memberId로 강의 목록 조회
    List<CourseEntity> findByMemberId(String memberId);

    // 학생 강의 목록 조회 (강의실 정보와 자료 개수 포함) - 임시로 기존 방식 사용
    // @Query("SELECT c, cl.classCode, COUNT(lf.id) as materialsCount " +
    //        "FROM CourseEntity c " +
    //        "LEFT JOIN com.jakdang.labs.entity.ClassroomEntity cl ON c.classId = cl.classId " +
    //        "LEFT JOIN com.jakdang.labs.api.gemjjok.entity.LectureFile lf ON c.courseId = lf.courseId AND lf.isActive = 0 " +
    //        "WHERE c.courseId IN (SELECT m.courseId FROM MemberEntity m WHERE m.memberEmail = (SELECT u.email FROM UserEntity u WHERE u.id = :userId) AND m.memberRole = 'ROLE_STUDENT' AND m.courseId IS NOT NULL) " +
    //        "GROUP BY c, cl.classCode")
    // List<Object[]> findCoursesWithClassroomAndMaterialsByStudentId(@Param("userId") String userId);

    // 사용하지 않는 메서드들 - users.id → email → member → courseId → course 체인으로 대체됨
    // @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId")
    // List<CourseEntity> findByMemberId(@Param("memberId") String memberId);
    // @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :memberId AND c.courseActive = 1")
    // List<CourseEntity> findActiveCoursesByMemberId(@Param("memberId") String memberId);
    // @Query("SELECT c FROM CourseEntity c WHERE c.courseId = :courseId AND c.memberId = :memberId")
    // List<CourseEntity> findByCourseIdAndMemberId(@Param("courseId") String courseId, @Param("memberId") String memberId);
    
    // Native Query로 실제 SQL 실행 결과 확인 (테스트용)
    @Query(value = "SELECT * FROM course WHERE memberId = :memberId", nativeQuery = true)
    List<CourseEntity> findByMemberIdNative(@Param("memberId") String memberId);
    
    // Native Query로 memberId 존재 여부 확인 (테스트용)
    @Query(value = "SELECT COUNT(*) FROM course WHERE memberId = :memberId", nativeQuery = true)
    Long countByMemberIdNative(@Param("memberId") String memberId);

    // courseId로 Native Query로 강의 정보 조회 (숫자/문자열 모두 처리)
    @Query(value = "SELECT * FROM course WHERE courseId = :courseId", nativeQuery = true)
    List<CourseEntity> findByCourseIdNative(@Param("courseId") String courseId);
} 