package com.jakdang.labs.api.lnuyasha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.SubjectDetailEntity;

import java.util.List;

@Repository
public interface KySubjectDetailRepository extends JpaRepository<SubjectDetailEntity, String> {
    
    /**
     * 과목별 세부과목 목록 조회 (조인 쿼리)
     * @return 과목명과 세부과목 정보를 포함한 목록
     */
    @Query("SELECT s.subjectName, sd.subDetailName, sd.subDetailId, sd.subDetailActive " +
           "FROM SubjectEntity s " +
           "LEFT JOIN SubDetailGroupEntity sdg ON s.subjectId = sdg.subjectId " +
           "LEFT JOIN SubjectDetailEntity sd ON sdg.subDetailId = sd.subDetailId " +
           "WHERE sd.subDetailId IS NOT NULL " +
           "ORDER BY s.subjectName, sd.subDetailName")
    List<Object[]> findSubjectsWithSubDetails();
    
    /**
     * 세부과목명으로 세부과목 조회
     * @param subDetailName 세부과목명
     * @return 세부과목 목록
     */
    List<SubjectDetailEntity> findBySubDetailName(String subDetailName);
    
    /**
     * educationId로 세부과목 목록 조회 (활성화된 것만)
     * @param educationId 학원 ID
     * @return 해당 학원의 활성화된 세부과목 목록
     */
    @Query("SELECT sd FROM SubjectDetailEntity sd WHERE sd.educationId = :educationId AND sd.subDetailActive = 0")
    List<SubjectDetailEntity> findByEducationId(@Param("educationId") String educationId);
    
    
    
    /**
     * educationId로 SubDetailGroupEntity 매핑 확인 (활성화된 것만)
     * @param educationId 학원 ID
     * @return 매핑 정보
     */
    @Query("SELECT sdg.subDetailId, sdg.subjectId, s.subjectName, sd.subDetailName " +
           "FROM SubDetailGroupEntity sdg " +
           "LEFT JOIN SubjectEntity s ON sdg.subjectId = s.subjectId " +
           "LEFT JOIN SubjectDetailEntity sd ON sdg.subDetailId = sd.subDetailId " +
           "WHERE (s.educationId = :educationId OR sd.educationId = :educationId) " +
           "AND (s.subjectActive = 0 OR s.subjectActive IS NULL) " +
           "AND (sd.subDetailActive = 0 OR sd.subDetailActive IS NULL) " +
           "ORDER BY sdg.subDetailId")
    List<Object[]> findSubDetailGroupMappings(@Param("educationId") String educationId);

     
    @Query("SELECT sg.subDetailGroupId FROM SubDetailGroupEntity sg " +
           "WHERE sg.subjectId = :subjectId")
    String findSubDetailGroupIdBySubject(@Param("subjectId") String subjectId);
} 