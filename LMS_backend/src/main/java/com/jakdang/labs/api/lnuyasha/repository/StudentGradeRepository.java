package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentGradeRepository extends JpaRepository<CourseEntity, String> {
    
    /**
     * userId로 학생의 수강 과정 정보 조회 (memberId, courseId)
     */
    @Query("SELECT m FROM MemberEntity m " +
           "WHERE m.id = :userId AND m.memberRole = 'ROLE_STUDENT' " +
           "AND m.memberExpired IS NULL")
    List<MemberEntity> findStudentCoursesByUserId(@Param("userId") String userId);
    
    /**
     * courseId로 과정 정보 조회
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.courseId = :courseId")
    CourseEntity findCourseByCourseId(@Param("courseId") String courseId);
    
    /**
     * courseId로 해당 과정의 시험 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    List<TemplateEntity> findTemplatesByCourseId(@Param("courseId") String courseId);
    
    /**
     * memberId와 templateId로 학생의 시험 성적 조회
     */
    @Query("SELECT ss FROM ScoreStudentEntity ss " +
           "WHERE ss.templateId = :templateId AND ss.memberId = :memberId")
    ScoreStudentEntity findScoreByTemplateAndMember(@Param("templateId") String templateId, @Param("memberId") String memberId);
    
    /**
     * templateId로 시험 만점 계산
     */
    @Query("SELECT SUM(tq.templateQuestionScore) FROM TemplateQuestionEntity tq " +
           "WHERE tq.templateId = :templateId")
    Double calculateMaxScoreByTemplateId(@Param("templateId") String templateId);
    
    /**
     * templateId로 객관식 만점 계산 (실제 구현에서는 문제 유형별로 분리 필요)
     */
    @Query("SELECT SUM(tq.templateQuestionScore) FROM TemplateQuestionEntity tq " +
           "WHERE tq.templateId = :templateId")
    Double calculateObjectiveMaxScore(@Param("templateId") String templateId);
    
    /**
     * templateId로 서술형 만점 계산 (실제 구현에서는 문제 유형별로 분리 필요)
     */
    @Query("SELECT SUM(tq.templateQuestionScore) FROM TemplateQuestionEntity tq " +
           "WHERE tq.templateId = :templateId")
    Double calculateSubjectiveMaxScore(@Param("templateId") String templateId);
    
    /**
     * memberId와 templateId로 학생의 객관식 점수 계산 (실제 구현에서는 문제 유형별로 분리 필요)
     */
    @Query("SELECT SUM(a.answerScore) FROM AnswerEntity a " +
           "JOIN TemplateQuestionEntity tq ON a.templateQuestion.templateQuestionId = tq.templateQuestionId " +
           "WHERE tq.templateId = :templateId AND a.memberId = :memberId")
    Double calculateObjectiveScore(@Param("templateId") String templateId, @Param("memberId") String memberId);
    
    /**
     * memberId와 templateId로 학생의 서술형 점수 계산 (실제 구현에서는 문제 유형별로 분리 필요)
     */
    @Query("SELECT SUM(a.answerScore) FROM AnswerEntity a " +
           "JOIN TemplateQuestionEntity tq ON a.templateQuestion.templateQuestionId = tq.templateQuestionId " +
           "WHERE tq.templateId = :templateId AND a.memberId = :memberId")
    Double calculateSubjectiveScore(@Param("templateId") String templateId, @Param("memberId") String memberId);
} 