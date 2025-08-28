package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.QuestionOptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptRepository extends JpaRepository<QuestionOptEntity, String> {
    
    /**
     * 특정 문제의 보기 목록 조회
     * @param questionId 문제 ID
     * @return 해당 문제의 보기 목록
     */
    @Query("SELECT qo FROM QuestionOptEntity qo WHERE qo.question.questionId = :questionId ORDER BY qo.optId")
    List<QuestionOptEntity> findByQuestionId(@Param("questionId") String questionId);
    
    /**
     * 특정 문제의 보기 개수 조회
     * @param questionId 문제 ID
     * @return 해당 문제의 보기 개수
     */
    @Query("SELECT COUNT(qo) FROM QuestionOptEntity qo WHERE qo.question.questionId = :questionId")
    long countByQuestionId(@Param("questionId") String questionId);
    
    /**
     * 특정 문제의 정답 보기 조회
     * @param questionId 문제 ID
     * @return 해당 문제의 정답 보기
     */
    @Query("SELECT qo FROM QuestionOptEntity qo WHERE qo.question.questionId = :questionId AND qo.optIsCorrect = 1")
    List<QuestionOptEntity> findCorrectOptionsByQuestionId(@Param("questionId") String questionId);
    
    /**
     * 특정 문제의 모든 보기 삭제
     * @param questionId 문제 ID
     */
    @Modifying
    @Query("DELETE FROM QuestionOptEntity qo WHERE qo.question.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") String questionId);
    
    /**
     * 특정 문제의 정답 옵션 조회
     * @param questionId 문제 ID
     * @param optIsCorrect 정답 여부 (1: 정답, 0: 오답)
     * @return 해당 문제의 정답 옵션 목록
     */
    @Query("SELECT qo FROM QuestionOptEntity qo WHERE qo.question.questionId = :questionId AND qo.optIsCorrect = :optIsCorrect")
    List<QuestionOptEntity> findByQuestionIdAndOptIsCorrect(@Param("questionId") String questionId, @Param("optIsCorrect") int optIsCorrect);
} 