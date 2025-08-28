package com.jakdang.labs.api.cottonCandy.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.TemplateGroupEntity;

@Repository
public interface TemplateGroupRepository extends JpaRepository<TemplateGroupEntity, String> {

    // courseId로 템플릿 그룹 조회 (모든 평가 질문 포함) =
    @Query("SELECT tg, qt, eq" + 
    " FROM TemplateGroupEntity tg " + 
    " INNER JOIN com.jakdang.labs.entity.QuestionTemplateEntity qt ON " +
    "   qt.questionTemplateNum = tg.questionTemplateNum " +
    "   AND qt.educationId = (SELECT c.educationId FROM CourseEntity c WHERE c.courseId = tg.courseId) " +
    " INNER JOIN com.jakdang.labs.entity.EvaluationQuestionEntity eq ON qt.evalQuestionId = eq.evalQuestionId " + 
    " WHERE tg.courseId = :courseId " +
    " ORDER BY qt.questionTemplateNum, eq.evalQuestionId")
    List<Object[]> findByCourseId(String courseId);
    
    // courseId로 템플릿 그룹만 조회 =
    @Query("SELECT tg FROM TemplateGroupEntity tg WHERE tg.courseId = :courseId")
    List<TemplateGroupEntity> findByCourseIdSimple(@Param("courseId") String courseId);
    

    
    // templateGroupId로 템플릿 그룹 조회
    TemplateGroupEntity findByTemplateGroupId(String templateGroupId);
    
    // templateGroupId로 템플릿 그룹과 질문들을 조회 (평가 항목 조회용)
    @Query("SELECT tg, qt, eq" + 
    " FROM TemplateGroupEntity tg " + 
    " INNER JOIN com.jakdang.labs.entity.QuestionTemplateEntity qt ON " +
    "   qt.questionTemplateNum = tg.questionTemplateNum " +
    "   AND qt.educationId = (SELECT c.educationId FROM CourseEntity c WHERE c.courseId = tg.courseId) " +
    " INNER JOIN com.jakdang.labs.entity.EvaluationQuestionEntity eq ON qt.evalQuestionId = eq.evalQuestionId " + 
    " WHERE tg.templateGroupId = :templateGroupId " +
    " ORDER BY qt.questionTemplateNum, eq.evalQuestionId")
    List<Object[]> findByTemplateGroupIdWithQuestions(@Param("templateGroupId") String templateGroupId);
}
