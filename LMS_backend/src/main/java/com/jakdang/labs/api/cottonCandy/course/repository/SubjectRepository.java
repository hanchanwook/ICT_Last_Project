package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jakdang.labs.entity.SubjectEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, String> {

    List<SubjectEntity> findBySubjectActive(int subjectActive);
    
    /**
     * 활성화된 과목과 연결된 상세과목 정보를 함께 조회
     * @return 과목 정보와 상세과목 정보 리스트 (Object[] 형태로 반환)
     */
    @Query("SELECT s.subjectId, s.subjectName, s.subjectInfo, s.createdAt, " +
           "sdg.subDetailId, sd.subDetailName, sd.subDetailInfo " +
           "FROM SubjectEntity s " +
           "LEFT JOIN com.jakdang.labs.entity.SubDetailGroupEntity sdg ON s.subjectId = sdg.subjectId " +
           "LEFT JOIN com.jakdang.labs.entity.SubjectDetailEntity sd ON sdg.subDetailId = sd.subDetailId " +
           "WHERE s.subjectActive = 0 AND s.educationId = :educationId " +
           "ORDER BY s.createdAt DESC, sd.subDetailName ASC")
    List<Object[]> findSubjectsWithSubDetails(String educationId);
}
