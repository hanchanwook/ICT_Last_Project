package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.TemplateQuestionEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.QuestionEntity;
import com.jakdang.labs.entity.QuestionOptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentExamRepository extends JpaRepository<AnswerEntity, String> {
    
    /**
     * 학생의 특정 시험 답안 조회
     */
    @Query("SELECT a FROM AnswerEntity a " +
           "JOIN TemplateQuestionEntity tq ON a.templateQuestion.templateQuestionId = tq.templateQuestionId " +
           "WHERE tq.templateId = :examId AND a.memberId = :studentId")
    List<AnswerEntity> findStudentAnswersByExamId(@Param("examId") String examId, @Param("studentId") String studentId);
    
    /**
     * 시험의 문제 목록 조회
     */
    @Query("SELECT tq FROM TemplateQuestionEntity tq WHERE tq.templateId = :examId ORDER BY tq.templateQuestionId")
    List<TemplateQuestionEntity> findQuestionsByExamId(@Param("examId") String examId);
    
    /**
     * 문제의 선택지 목록 조회
     */
    @Query("SELECT qo FROM QuestionOptEntity qo WHERE qo.question.questionId = :questionId ORDER BY qo.optId")
    List<QuestionOptEntity> findOptionsByQuestionId(@Param("questionId") String questionId);
    
    /**
     * 학생의 시험 성적 조회
     */
    @Query("SELECT ss FROM ScoreStudentEntity ss WHERE ss.templateId = :examId AND ss.memberId = :studentId")
    ScoreStudentEntity findStudentScoreByExamId(@Param("examId") String examId, @Param("studentId") String studentId);
    
    /**
     * 문제 상세 정보 조회
     */
    @Query("SELECT q FROM QuestionEntity q WHERE q.questionId = :questionId")
    QuestionEntity findQuestionById(@Param("questionId") String questionId);
    
    /**
     * 템플릿의 문제 목록 조회 (시험 시작용)
     */
    @Query("SELECT tq FROM TemplateQuestionEntity tq WHERE tq.templateId = :templateId ORDER BY tq.templateQuestionId")
    List<TemplateQuestionEntity> findTemplateQuestionsByTemplateId(@Param("templateId") String templateId);
    
    /**
     * 학생의 특정 템플릿 답안 조회
     */
    @Query("SELECT a FROM AnswerEntity a " +
           "JOIN TemplateQuestionEntity tq ON a.templateQuestion.templateQuestionId = tq.templateQuestionId " +
           "WHERE tq.templateId = :templateId AND a.memberId = :studentId")
    List<AnswerEntity> findStudentAnswersByTemplateId(@Param("templateId") String templateId, @Param("studentId") String studentId);
    
    /**
     * 학생의 특정 문제 답안 조회
     */
    @Query("SELECT a FROM AnswerEntity a " +
           "WHERE a.templateQuestion.templateQuestionId = :templateQuestionId AND a.memberId = :studentId")
    AnswerEntity findStudentAnswerByTemplateQuestionAndMember(@Param("templateQuestionId") String templateQuestionId, @Param("studentId") String studentId);
} 