package com.jakdang.labs.api.lnuyasha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.QuestionEntity;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, String> {
    
    /**
     * 활성화된 모든 문제 조회
     * @return 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE q.educationId = :educationId AND q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> findAllActiveQuestions(@Param("educationId") String educationId);

    /**
     * 활성화된 모든 문제 조회 (educationId 필터링 없음)
     * @return 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> findAllActiveQuestionsWithoutEducationFilter();


    /**
     * 특정 강사가 등록한 문제 목록 조회
     * @param memberId 강사 ID
     * @return 해당 강사의 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE q.memberId = :memberId AND q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> findByMemberId(@Param("memberId") String memberId);
    
    /**
     * 특정 강사가 등록한 문제 목록 조회 (안전한 버전)
     * @param memberId 강사 ID
     * @return 해당 강사의 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE (:memberId IS NULL OR q.memberId = :memberId) AND q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> findByMemberIdSafe(@Param("memberId") String memberId);
    
    /**
     * 문제 유형별 문제 목록 조회
     * @param questionType 문제 유형
     * @return 해당 유형의 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE q.questionType = :questionType AND q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> findByQuestionType(@Param("questionType") String questionType);
    
    /**
     * 세부과목별 문제 목록 조회
     * @param subDetailId 세부과목 ID
     * @return 해당 세부과목의 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE q.subDetailId = :subDetailId AND q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> findBySubDetailId(@Param("subDetailId") String subDetailId);
    
    
    /**
     * 문제 검색 (제목, 내용으로 검색)
     * @param searchKeyword 검색 키워드
     * @return 검색된 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE (q.questionText LIKE %:searchKeyword% OR q.questionAnswer LIKE %:searchKeyword%) AND q.questionActive = 0 ORDER BY q.createdAt DESC")
    List<QuestionEntity> searchQuestions(@Param("searchKeyword") String searchKeyword);
    
    
    
    /**
     * 문제 유형 및 상태별 개수 조회
     * @param questionType 문제 유형
     * @param questionActive 문제 상태 (0: 활성, 1: 비활성)
     * @param educationId 학원 ID
     * @return 해당 유형 및 상태의 문제 개수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.questionType = :questionType AND q.questionActive = :questionActive AND q.educationId = :educationId")
    long countByQuestionTypeAndQuestionActive(@Param("questionType") String questionType, @Param("questionActive") int questionActive, @Param("educationId") String educationId);
    
    /**
     * 문제 상태별 개수 조회
     * @param questionActive 문제 상태 (0: 활성, 1: 비활성)
     * @return 해당 상태의 문제 개수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.questionActive = :questionActive AND q.educationId = :educationId")
    long countByQuestionActive(@Param("questionActive") int questionActive, @Param("educationId") String educationId);
    
    /**
     * 문제 상태별 개수 조회 (educationId가 null인 경우 모든 문제 포함)
     * @param questionActive 문제 상태 (0: 활성, 1: 비활성)
     * @param educationId 학원 ID (null인 경우 모든 학원)
     * @return 해당 상태의 문제 개수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.questionActive = :questionActive AND (:educationId IS NULL OR q.educationId = :educationId)")
    long countByQuestionActiveWithNull(@Param("questionActive") int questionActive, @Param("educationId") String educationId);
    
    /**
     * 문제 유형 및 상태별 개수 조회 (educationId가 null인 경우 모든 문제 포함)
     * @param questionType 문제 유형
     * @param questionActive 문제 상태 (0: 활성, 1: 비활성)
     * @param educationId 학원 ID (null인 경우 모든 학원)
     * @return 해당 유형 및 상태의 문제 개수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.questionType = :questionType AND q.questionActive = :questionActive AND (:educationId IS NULL OR q.educationId = :educationId)")
    long countByQuestionTypeAndQuestionActiveWithNull(@Param("questionType") String questionType, @Param("questionActive") int questionActive, @Param("educationId") String educationId);
    
    /**
     * 실시간 검색 결과 개수 조회
     * @param keyword 검색 키워드
     * @param questionType 문제 유형
     * @param memberId 강사 ID
     * @param subDetailId 세부과목 ID
     * @param educationId 학원 ID
     * @return 검색된 문제 개수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE " +
           "(:keyword IS NULL OR (q.questionText LIKE %:keyword% OR q.questionAnswer LIKE %:keyword%)) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:memberId IS NULL OR q.memberId = :memberId) AND " +
           "(:subDetailId IS NULL OR q.subDetailId = :subDetailId) AND " +
           "(:educationId IS NULL OR q.educationId = :educationId) AND " +
           "q.questionActive = 0")
    long countQuestionsWithFilters(
            @Param("keyword") String keyword,
            @Param("questionType") String questionType,
            @Param("memberId") String memberId,
            @Param("subDetailId") String subDetailId,
            @Param("educationId") String educationId);
    
    /**
     * 프론트엔드 검색 및 필터링 (텍스트 검색, 연도 필터, 상태 필터)
     * @param searchTerm 검색 키워드
     * @param selectedYear 연도 필터
     * @param selectedStatus 상태 필터
     * @param memberId 강사 ID
     * @param questionType 문제 유형
     * @param subDetailId 세부과목 ID
     * @return 검색된 문제 목록
     */
    @Query("SELECT q FROM QuestionEntity q WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "(q.questionText LIKE %:searchTerm% OR q.questionAnswer LIKE %:searchTerm% OR " +
           "q.questionType LIKE %:searchTerm%)) AND " +
           "(:selectedYear IS NULL OR :selectedYear = 'all' OR " +
           "YEAR(q.createdAt) = CAST(:selectedYear AS integer)) AND " +
           "(:selectedStatus IS NULL OR :selectedStatus = 'all' OR " +
           "CASE WHEN q.questionActive = 0 THEN '활성' ELSE '비활성' END = :selectedStatus) AND " +
           "(:memberId IS NULL OR q.memberId = :memberId) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:subDetailId IS NULL OR q.subDetailId = :subDetailId) AND " +
           "q.questionActive = 0 " +
           "ORDER BY q.createdAt DESC")
    List<QuestionEntity> findQuestionsWithFrontendFilters(
            @Param("searchTerm") String searchTerm,
            @Param("selectedYear") String selectedYear,
            @Param("selectedStatus") String selectedStatus,
            @Param("memberId") String memberId,
            @Param("questionType") String questionType,
            @Param("subDetailId") String subDetailId);
    
    /**
     * 프론트엔드 검색 결과 개수 조회
     * @param searchTerm 검색 키워드
     * @param selectedYear 연도 필터
     * @param selectedStatus 상태 필터
     * @param memberId 강사 ID
     * @param questionType 문제 유형
     * @param subDetailId 세부과목 ID
     * @return 검색된 문제 개수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "(q.questionText LIKE %:searchTerm% OR q.questionAnswer LIKE %:searchTerm% OR " +
           "q.questionType LIKE %:searchTerm%)) AND " +
           "(:selectedYear IS NULL OR :selectedYear = 'all' OR " +
           "YEAR(q.createdAt) = CAST(:selectedYear AS integer)) AND " +
           "(:selectedStatus IS NULL OR :selectedStatus = 'all' OR " +
           "CASE WHEN q.questionActive = 0 THEN '활성' ELSE '비활성' END = :selectedStatus) AND " +
           "(:memberId IS NULL OR q.memberId = :memberId) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:subDetailId IS NULL OR q.subDetailId = :subDetailId) AND " +
           "q.questionActive = 0")
    long countQuestionsWithFrontendFilters(
            @Param("searchTerm") String searchTerm,
            @Param("selectedYear") String selectedYear,
            @Param("selectedStatus") String selectedStatus,
            @Param("memberId") String memberId,
            @Param("questionType") String questionType,
            @Param("subDetailId") String subDetailId);
    
    // ========== 상위 과목 정보를 포함한 조인 쿼리들 ==========
    

    
    /**
     * 상위 과목 정보를 포함한 실시간 검색 쿼리
     * @param keyword 검색 키워드
     * @param questionType 문제 유형
     * @param memberId 강사 ID
     * @param subDetailId 세부과목 ID
     * @param educationId 학원 ID
     * @param sortBy 정렬 기준
     * @param sortDirection 정렬 방향
     * @return 검색된 문제 목록 (상위 과목 정보 포함)
     */
    @Query("SELECT q, sd.subDetailName, s.subjectId, s.subjectName, s.subjectInfo " +
           "FROM QuestionEntity q " +
           "LEFT JOIN SubjectDetailEntity sd ON q.subDetailId = sd.subDetailId " +
           "LEFT JOIN SubDetailGroupEntity sdg ON sd.subDetailId = sdg.subDetailId " +
           "LEFT JOIN SubjectEntity s ON sdg.subjectId = s.subjectId " +
           "WHERE (:keyword IS NULL OR (q.questionText LIKE %:keyword% OR q.questionAnswer LIKE %:keyword%)) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:memberId IS NULL OR q.memberId = :memberId) AND " +
           "(:subDetailId IS NULL OR q.subDetailId = :subDetailId) AND " +
           "(:educationId IS NULL OR q.educationId = :educationId) AND " +
           "q.questionActive = 0 " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'questionType' THEN q.questionType END ASC, " +
           "CASE WHEN :sortBy = 'questionType' AND :sortDirection = 'desc' THEN q.questionType END DESC, " +
           "CASE WHEN :sortBy = 'memberId' THEN q.memberId END ASC, " +
           "CASE WHEN :sortBy = 'memberId' AND :sortDirection = 'desc' THEN q.memberId END DESC, " +
           "CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'asc' THEN q.createdAt END ASC, " +
           "q.createdAt DESC")
    List<Object[]> findQuestionsWithFiltersAndSubjectInfo(
            @Param("keyword") String keyword,
            @Param("questionType") String questionType,
            @Param("memberId") String memberId,
            @Param("subDetailId") String subDetailId,
            @Param("educationId") String educationId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
    
    /**
     * 상위 과목별 문제 목록 조회
     * @param subjectId 상위 과목 ID
     * @return 해당 상위 과목의 문제 목록
     */
    @Query("SELECT q, sd.subDetailName, s.subjectId, s.subjectName, s.subjectInfo " +
           "FROM QuestionEntity q " +
           "LEFT JOIN SubjectDetailEntity sd ON q.subDetailId = sd.subDetailId " +
           "LEFT JOIN SubDetailGroupEntity sdg ON sd.subDetailId = sdg.subDetailId " +
           "LEFT JOIN SubjectEntity s ON sdg.subjectId = s.subjectId " +
           "WHERE sdg.subjectId = :subjectId AND q.questionActive = 0 " +
           "ORDER BY q.createdAt DESC")
    List<Object[]> findBySubjectIdWithSubjectInfo(@Param("subjectId") String subjectId);
    


    // ========== 통계 및 메타데이터 쿼리들 ==========
    
    /**
     * 강사별 통계 정보 조회
     * @return 강사별 문제 수 및 정보
     */
    @Query("SELECT q.memberId, m.memberName, m.memberEmail, COUNT(q) as questionCount, " +
           "m.memberRole, MAX(q.createdAt) as lastActiveDate " +
           "FROM QuestionEntity q " +
           "LEFT JOIN MemberEntity m ON q.memberId = m.memberId " +
           "WHERE q.questionActive = 0 AND m.memberRole = '강사' " +
           "GROUP BY q.memberId, m.memberName, m.memberEmail, m.memberRole " +
           "ORDER BY questionCount DESC")
    List<Object[]> findInstructorStats();
    
    /**
     * 과목별 통계 정보 조회
     * @return 과목별 문제 수 및 정보
     */
    @Query("SELECT s.subjectId, s.subjectName, s.subjectInfo, sd.subDetailId, sd.subDetailName, " +
           "COUNT(q) as questionCount, e.educationId, e.educationName " +
           "FROM QuestionEntity q " +
           "LEFT JOIN SubjectDetailEntity sd ON q.subDetailId = sd.subDetailId " +
           "LEFT JOIN SubDetailGroupEntity sdg ON sd.subDetailId = sdg.subDetailId " +
           "LEFT JOIN SubjectEntity s ON sdg.subjectId = s.subjectId " +
           "LEFT JOIN EducationEntity e ON q.educationId = e.educationId " +
           "WHERE q.questionActive = 0 " +
           "GROUP BY s.subjectId, s.subjectName, s.subjectInfo, sd.subDetailId, sd.subDetailName, " +
           "e.educationId, e.educationName " +
           "ORDER BY questionCount DESC")
    List<Object[]> findSubjectStats();
    
    /**
     * 문제 유형별 통계 조회
     * @return 문제 유형별 개수
     */
    @Query("SELECT q.questionType, COUNT(q) as count " +
           "FROM QuestionEntity q " +
           "WHERE q.questionActive = 0 " +
           "GROUP BY q.questionType " +
           "ORDER BY count DESC")
    List<Object[]> findQuestionTypeStats();
    


    // ========== 동적 통계 쿼리들 ==========
    
    /**
     * 과목별 문제 수 통계 조회 (실제 데이터 기반)
     * @return 과목별 문제 수
     */
    @Query("SELECT sd.subDetailName as subjectName, COUNT(q) as questionCount " +
           "FROM QuestionEntity q " +
           "LEFT JOIN SubjectDetailEntity sd ON q.subDetailId = sd.subDetailId " +
           "WHERE q.questionActive = 0 AND sd.subDetailName IS NOT NULL " +
           "GROUP BY sd.subDetailName " +
           "ORDER BY questionCount DESC")
    List<Object[]> findQuestionsBySubjectStats();
    
    /**
     * 강사별 문제 수 통계 조회 (실제 데이터 기반)
     * @return 강사별 문제 수
     */
    @Query("SELECT m.memberName as instructorName, COUNT(q) as questionCount " +
           "FROM QuestionEntity q " +
           "LEFT JOIN MemberEntity m ON q.memberId = m.memberId " +
           "WHERE q.questionActive = 0 AND m.memberRole = '강사' AND m.memberName IS NOT NULL " +
           "GROUP BY m.memberName " +
           "ORDER BY questionCount DESC")
    List<Object[]> findQuestionsByInstructorStats();
    
    /**
     * 문제 ID로 문제 조회
     * @param questionId 문제 ID
     * @return 문제 정보
     */
    java.util.Optional<QuestionEntity> findByQuestionId(String questionId);
    
    /**
     * 데이터베이스에 존재하는 모든 educationId 값 조회 (디버깅용)
     * @return educationId 목록
     */
    @Query("SELECT DISTINCT q.educationId FROM QuestionEntity q WHERE q.educationId IS NOT NULL")
    List<String> findAllEducationIds();
    
    /**
     * 특정 educationId의 문제 수 조회 (디버깅용)
     * @param educationId 학원 ID
     * @return 해당 educationId의 문제 수
     */
    @Query("SELECT COUNT(q) FROM QuestionEntity q WHERE q.educationId = :educationId")
    long countByEducationId(@Param("educationId") String educationId);
} 