package com.jakdang.labs.api.lnuyasha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.SubjectEntity;

import java.util.List;

@Repository
public interface LectureHistoryRepository extends JpaRepository<CourseEntity, String> {
    
    /**
     * 강사가 담당한 모든 강의 조회
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.memberId = :instructorId ORDER BY c.createdAt DESC")
    List<CourseEntity> findByInstructorId(@Param("instructorId") String instructorId);
    

    
    /**
     * 강사가 담당한 완료된 강의 수 조회 (날짜 기준)
     */
    @Query("SELECT COUNT(c) FROM CourseEntity c WHERE c.memberId = :instructorId AND c.courseEndDay < CURRENT_DATE")
    Long countCompletedLecturesByInstructorId(@Param("instructorId") String instructorId);
    
    /**
     * 강사가 담당한 총 강의 수 조회
     */
    @Query("SELECT COUNT(c) FROM CourseEntity c WHERE c.memberId = :instructorId")
    Long countTotalLecturesByInstructorId(@Param("instructorId") String instructorId);
    
    /**
     * 과정별 수강생 수 조회 (memberExpired가 null인 학생만)
     */
    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' AND m.memberExpired IS NULL")
    int countStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 완료 수강생 수 조회 (memberExpired IS NULL이면서 수업이 완료된 학생)
     */
    @Query("SELECT COUNT(m) FROM MemberEntity m " +
           "JOIN CourseEntity c ON m.courseId = c.courseId " +
           "WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' " +
           "AND m.memberExpired IS NULL AND c.courseEndDay < CURRENT_DATE")
    int countCompletedStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 평균 성적 조회 (ScoreStudentEntity 사용)
     */
    @Query("SELECT AVG(ss.score) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    Double calculateAverageScoreByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 총점 계산 (ScoreStudentEntity 사용)
     */
    @Query("SELECT SUM(ss.score) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    Double calculateTotalScoreByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 합격자 수 조회 (ScoreStudentEntity 사용)
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND ss.score >= 60")
    int countPassedStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 총 시험 수 조회 (TemplateEntity 사용)
     */
    @Query("SELECT COUNT(t) FROM TemplateEntity t " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    int countTotalExamsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 완료된 시험 수 조회 (TemplateEntity 사용)
     */
    @Query("SELECT COUNT(t) FROM TemplateEntity t " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND t.templateActive = 1")
    int countCompletedExamsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 90점 이상 학생 수 조회
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND ss.score >= 90")
    int countOutstandingStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 80-89점 학생 수 조회
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND ss.score >= 80 AND ss.score < 90")
    int countExcellentStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 70-79점 학생 수 조회
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND ss.score >= 70 AND ss.score < 80")
    int countGoodStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 60-69점 학생 수 조회
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND ss.score >= 60 AND ss.score < 70")
    int countAverageStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 60점 미만 학생 수 조회
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND ss.score < 60")
    int countNeedsImprovementStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 시험을 보지 않은 학생 수 조회 (0점 처리, memberExpired가 null인 학생만)
     */
    @Query("SELECT COUNT(m) FROM MemberEntity m " +
           "WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' AND m.memberExpired IS NULL " +
           "AND m.memberId NOT IN (SELECT DISTINCT ss.memberId FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId))")
    int countStudentsWithoutScoreByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 시험을 본 학생 수 조회 (memberExpired가 null인 학생만)
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "JOIN MemberEntity m ON ss.memberId = m.memberId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND m.memberExpired IS NULL")
    int countStudentsWithScoreByCourseId(@Param("courseId") String courseId);
    
    /**
     * 강사별 전체 평균 성적 계산
     */
    @Query("SELECT AVG(ss.score) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.memberId = :instructorId")
    Double calculateOverallAverageScoreByInstructorId(@Param("instructorId") String instructorId);
    
    /**
     * 강사별 총 수강생 수 계산 (memberExpired가 null인 학생만)
     */
    @Query("SELECT COUNT(DISTINCT m.memberId) FROM MemberEntity m " +
           "JOIN CourseEntity c ON m.courseId = c.courseId " +
           "WHERE c.memberId = :instructorId AND m.memberRole = 'ROLE_STUDENT' AND m.memberExpired IS NULL")
    int countTotalStudentsByInstructorId(@Param("instructorId") String instructorId);
    
    /**
     * 과정별 시험 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    List<TemplateEntity> findTemplatesByCourseId(@Param("courseId") String courseId);
    
    /**
     * 시험별 평균 점수 계산
     */
    @Query("SELECT AVG(ss.score) FROM ScoreStudentEntity ss WHERE ss.templateId = :templateId")
    Double calculateAverageScoreByTemplateId(@Param("templateId") String templateId);

    /**
     * 시험별 합격률 계산 (60점 이상)
     */
    @Query("SELECT (COUNT(CASE WHEN ss.score >= 60 THEN 1 END) * 100.0 / COUNT(ss)) " +
           "FROM ScoreStudentEntity ss WHERE ss.templateId = :templateId")
    Double calculatePassRateByTemplateId(@Param("templateId") String templateId);

    /**
     * 시험별 참여자 수 계산
     */
    @Query("SELECT COUNT(ss) FROM ScoreStudentEntity ss WHERE ss.templateId = :templateId")
    Integer countParticipantsByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 시험별 합격자 수 계산
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss WHERE ss.templateId = :templateId AND ss.score >= 60")
    int countPassedStudentsByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 시험별 전체 응시자 수 계산
     */
    @Query("SELECT COUNT(DISTINCT ss.memberId) FROM ScoreStudentEntity ss WHERE ss.templateId = :templateId")
    int countTotalStudentsByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 시험별 학생 점수 목록 조회
     */
    @Query("SELECT ss FROM ScoreStudentEntity ss WHERE ss.templateId = :templateId")
    List<ScoreStudentEntity> findScoreStudentsByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 과정별 학생 목록 조회 (memberExpired가 null인 학생만)
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' AND m.memberExpired IS NULL")
    List<MemberEntity> findStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 학생별 과정 평균 점수 계산
     */
    @Query("SELECT AVG(ss.score) FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE ss.memberId = :memberId AND t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    Double calculateAverageScoreByStudentAndCourse(@Param("memberId") String memberId, @Param("courseId") String courseId);
    
    /**
     * 과정별 과목 목록 조회
     */
    @Query("SELECT s FROM SubjectEntity s " +
           "WHERE s.subjectId IN (SELECT sg.subjectId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    List<SubjectEntity> findSubjectsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과목별 시험 목록 조회
     */
    @Query("SELECT t FROM TemplateEntity t " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.subjectId = :subjectId)")
    List<TemplateEntity> findTemplatesBySubjectId(@Param("subjectId") String subjectId);
    
    /**
     * 과정별 활성화된 템플릿 목록 조회 (isActive = 0인 것만)
     */
    @Query("SELECT t FROM TemplateEntity t " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "AND t.templateActive = 0")
    List<TemplateEntity> findActiveTemplatesByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 중도탈락 학생 수 조회
     */
    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' AND m.memberExpired IS NOT NULL")
    int countDropoutStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 합격률 계산
     */
    @Query("SELECT CASE WHEN COUNT(DISTINCT ss.memberId) > 0 THEN " +
           "(COUNT(DISTINCT CASE WHEN ss.score >= 60 THEN ss.memberId END) * 100.0 / COUNT(DISTINCT ss.memberId)) " +
           "ELSE 0 END FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId)")
    Integer calculatePassRateByCourseId(@Param("courseId") String courseId);
    
    /**
     * 과정별 학생별 평균 점수 조회 (성적 분포 계산용)
     */
    @Query("SELECT ss.memberId, AVG(ss.score) as averageScore " +
           "FROM ScoreStudentEntity ss " +
           "JOIN TemplateEntity t ON ss.templateId = t.templateId " +
           "WHERE t.subGroupId IN (SELECT sg.subGroupId FROM SubGroupEntity sg WHERE sg.courseId = :courseId) " +
           "GROUP BY ss.memberId")
    List<Object[]> findStudentAverageScoresByCourseId(@Param("courseId") String courseId);
} 