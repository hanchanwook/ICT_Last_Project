package com.jakdang.labs.api.jaegyeom.registration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.CourseEntity;

@Repository
public interface GetCourseRepo extends JpaRepository<CourseEntity, String> {

    @Query("SELECT c FROM CourseEntity c WHERE c.educationId = :educationId")
    List<CourseEntity> getCourseList(@Param("educationId") String educationId);
}
