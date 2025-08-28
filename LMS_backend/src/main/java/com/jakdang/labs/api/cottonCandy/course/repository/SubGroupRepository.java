package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jakdang.labs.entity.SubGroupEntity;

import org.springframework.stereotype.Repository;

@Repository
public interface SubGroupRepository extends JpaRepository<SubGroupEntity, String>{
    
      
    @Query("SELECT sg.subjectId, s.subjectName, s.subjectInfo, sg.subjectTime " +
       "FROM SubGroupEntity sg " +
       "JOIN com.jakdang.labs.entity.SubjectEntity s ON sg.subjectId = s.subjectId " +
       "WHERE sg.courseId = :courseId")
    List<Object[]> findSubGroupInfoByCourseId(@Param("courseId") String courseId);
   
    SubGroupEntity findBySubGroupId(String subGroupId);

    @Query("SELECT COUNT(sg) FROM SubGroupEntity sg " +
           "JOIN com.jakdang.labs.entity.CourseEntity c ON sg.courseId = c.courseId " +
           "WHERE sg.subjectId = :subjectId AND c.courseActive = 0")
    int countBySubjectId(@Param("subjectId") String subjectId);
    
}
