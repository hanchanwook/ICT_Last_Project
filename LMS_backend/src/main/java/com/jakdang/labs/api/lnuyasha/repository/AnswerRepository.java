package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.AnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

/**
 * 학생 답안 정보 조회를 위한 Repository
 */
@Repository
public interface AnswerRepository extends JpaRepository<AnswerEntity, String> {
    
    /**
     * memberId로 답안 목록 조회
     * @param memberId 회원 ID
     * @return 해당 회원의 답안 목록
     */
    List<AnswerEntity> findByMemberId(String memberId);
    
    /**
     * memberId와 answerActive로 답안 목록 조회
     * @param memberId 회원 ID
     * @param answerActive 활성 상태
     * @return 해당 회원의 활성화된 답안 목록
     */
    List<AnswerEntity> findByMemberIdAndAnswerActive(String memberId, int answerActive);
    
    /**
     * 시험 템플릿 ID와 회원 ID로 답안 목록 조회
     * @param templateId 시험 템플릿 ID
     * @param memberId 회원 ID
     * @return 해당 시험의 답안 목록
     */
    @Query("SELECT a FROM AnswerEntity a WHERE a.templateQuestion.templateId = :templateId AND a.memberId = :memberId")
    List<AnswerEntity> findByTemplateIdAndMemberId(@Param("templateId") String templateId, @Param("memberId") String memberId);
    
    /**
     * 시험 템플릿 ID로 모든 답안 목록 조회
     * @param templateId 시험 템플릿 ID
     * @return 해당 시험의 모든 답안 목록
     */
    @Query("SELECT a FROM AnswerEntity a WHERE a.templateQuestion.templateId = :templateId")
    List<AnswerEntity> findByTemplateId(@Param("templateId") String templateId);
    
    /**
     * templateQuestionId와 memberId로 답안 조회
     * @param templateQuestionId 템플릿 질문 ID
     * @param memberId 회원 ID
     * @return 해당 답안
     */
    @Query("SELECT a FROM AnswerEntity a WHERE a.templateQuestion.templateQuestionId = :templateQuestionId AND a.memberId = :memberId")
    AnswerEntity findByTemplateQuestionIdAndMemberId(@Param("templateQuestionId") String templateQuestionId, @Param("memberId") String memberId);
    
    /**
     * 템플릿 문제 ID로 답안 목록 조회
     * @param templateQuestionId 템플릿 문제 ID
     * @return 해당 문제의 모든 답안 목록
     */
    @Query("SELECT a FROM AnswerEntity a WHERE a.templateQuestion.templateQuestionId = :templateQuestionId")
    List<AnswerEntity> findByTemplateQuestionId(@Param("templateQuestionId") String templateQuestionId);

} 