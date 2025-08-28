package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jakdang.labs.entity.SubDetailGroupEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface SubDetailGroupRepository extends JpaRepository<SubDetailGroupEntity, String>{
    

    
     @Query("SELECT sg.subjectId, s.subjectName, s.subjectInfo " +
        "FROM SubDetailGroupEntity sg " +
        "JOIN com.jakdang.labs.entity.SubjectEntity s ON sg.subjectId = s.subjectId " +
        "WHERE sg.subjectId = :subjectId")
    List<Object[]> findSubDetailInfoBySubjectId(@Param("subjectId") String subjectId);


}
