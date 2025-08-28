package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jakdang.labs.entity.CourseEntity;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, String>{
    
//     //List<CourseEntity> findByCourseActive(int courseActive);
//     @Query("SELECT c, m.memberName FROM CourseEntity c " +
//     " LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
//     " WHERE c.educationId = :educationId AND c.courseActive = :courseActive")
//     List<Object[]> findByEducationIdAndCourseActive(String educationId, int courseActive);

    // 과정 평가용 강의 리스트 조회 (TemplateGroup 정보 포함)
    @Query("SELECT c, m.memberName, tg.openDate, tg.closeDate FROM CourseEntity c " +
    " LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
    " LEFT JOIN com.jakdang.labs.entity.TemplateGroupEntity tg ON c.courseId = tg.courseId " +
    " WHERE c.educationId = :educationId AND c.courseActive = :courseActive")
    List<Object[]> findByEducationIdAndCourseActiveWithTemplateGroup(String educationId, int courseActive);

    @Query("SELECT c.courseId, m.memberName, c.courseCode, c.courseName, c.maxCapacity, c.minCapacity,  "+
           "cl.classNumber,c.classId, c.courseStartDay, c.courseEndDay, c.courseDays, c.startTime, c.endTime, c.createdAt, " +
           "s.subjectId, s.subjectName, s.subjectInfo, sg.subjectTime " +
           "FROM CourseEntity c " +
           "LEFT JOIN com.jakdang.labs.entity.SubGroupEntity sg ON c.courseId = sg.courseId " +
           "LEFT JOIN com.jakdang.labs.entity.SubjectEntity s ON s.subjectId = sg.subjectId " +
           "LEFT JOIN com.jakdang.labs.entity.ClassroomEntity cl ON c.classId = cl.classId " +
           "LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
           "WHERE c.courseActive = 0 and c.educationId = :educationId")
    List<Object[]> findCourseWithSubGroup(@Param("educationId") String educationId);

    // 강의실ID, 기간이 겹치는 예약된 강의 목록 조회
    @Query("SELECT c FROM CourseEntity c WHERE c.classId = :classId AND c.courseActive = 0 AND ((c.courseStartDay <= :endDate AND c.courseEndDay >= :startDate))")
    List<CourseEntity> findReservedCoursesByClassIdAndPeriod(@Param("classId") String classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 학생이 듣는 강의 리스트
    @Query("SELECT c, m.memberName, tg.templateGroupId, qt.questionTemplateName, qt.questionTemplateNum, tg.openDate, tg.closeDate, mem2.memberId " +
    " FROM CourseEntity c " +
    " LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
    " LEFT JOIN com.jakdang.labs.entity.TemplateGroupEntity tg ON c.courseId = tg.courseId " +
    " LEFT JOIN com.jakdang.labs.entity.QuestionTemplateEntity qt ON qt.questionTemplateNum = tg.questionTemplateNum AND qt.educationId = c.educationId " +
    " LEFT JOIN com.jakdang.labs.entity.MemberEntity mem2 ON mem2.memberEmail = (SELECT u.email FROM com.jakdang.labs.api.auth.entity.UserEntity u WHERE u.id = :userId) AND mem2.memberRole = 'ROLE_STUDENT' AND mem2.courseId = c.courseId " +
    " WHERE c.courseId IN (SELECT mem3.courseId FROM MemberEntity mem3 WHERE mem3.memberEmail = (SELECT u2.email FROM com.jakdang.labs.api.auth.entity.UserEntity u2 WHERE u2.id = :userId) " +
    " AND mem3.memberRole = 'ROLE_STUDENT' AND mem3.courseId IS NOT NULL) AND c.courseActive = 0 AND c.educationId = :educationId")
    List<Object[]> findByEducationIdAndCourseActiveAndUserId(@Param("educationId") String educationId, @Param("userId") String userId);

    // 강사가 담당하는 강의 리스트 (강사 이름 포함)
    @Query("SELECT c, m.memberName, tg.openDate, tg.closeDate FROM CourseEntity c " +
           "LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
           "LEFT JOIN com.jakdang.labs.entity.TemplateGroupEntity tg ON c.courseId = tg.courseId " +
           "WHERE c.memberId = :memberId AND c.courseActive = 0")
    List<Object[]> findByMemberIdWithInstructorName(@Param("memberId") String memberId);

    // courseId 목록으로 강의 정보 조회 (평가용)
    @Query("SELECT c, m.memberName, tg.templateGroupId, qt.questionTemplateName, qt.questionTemplateNum, tg.openDate, tg.closeDate, mem2.memberId " +
    " FROM CourseEntity c " +
    " LEFT JOIN com.jakdang.labs.entity.MemberEntity m ON c.memberId = m.memberId " +
    " LEFT JOIN com.jakdang.labs.entity.TemplateGroupEntity tg ON c.courseId = tg.courseId " +
    " LEFT JOIN com.jakdang.labs.entity.QuestionTemplateEntity qt ON qt.questionTemplateNum = tg.questionTemplateNum AND qt.educationId = c.educationId " +
    " LEFT JOIN com.jakdang.labs.entity.MemberEntity mem2 ON mem2.memberEmail = (SELECT u.email FROM UserEntity u WHERE u.id = :userId) AND mem2.memberRole = 'ROLE_STUDENT' AND mem2.courseId = c.courseId " +
    " WHERE c.courseId IN :courseIds AND c.courseActive = 0 AND c.educationId = :educationId")
    List<Object[]> findByCourseIdsAndEducationId(@Param("courseIds") List<String> courseIds, @Param("educationId") String educationId, @Param("userId") String userId);


}
