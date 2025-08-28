package com.jakdang.labs.api.lnuyasha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.SubGroupEntity;

import java.util.List;

@Repository("lnuyashaSubGroupRepository")
public interface SubGroupRepository extends JpaRepository<SubGroupEntity, String> {
    
    /**
     * courseId와 subjectId로 SubGroupEntity 조회
     */
    SubGroupEntity findByCourseIdAndSubjectId(String courseId, String subjectId);
    
    /**
     * courseId로 SubGroupEntity 목록 조회
     */
    List<SubGroupEntity> findByCourseId(String courseId);
    
    /**
     * subjectId로 SubGroupEntity 목록 조회
     */
    List<SubGroupEntity> findBySubjectId(String subjectId);
    
    /**
     * subGroupId로 SubGroupEntity 조회
     */
    SubGroupEntity findBySubGroupId(String subGroupId);
} 