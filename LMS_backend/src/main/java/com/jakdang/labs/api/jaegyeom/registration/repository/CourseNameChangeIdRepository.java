package com.jakdang.labs.api.jaegyeom.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jakdang.labs.entity.CourseEntity;

@Repository
public interface CourseNameChangeIdRepository extends JpaRepository<CourseEntity, String> {
    CourseEntity findByCourseName(String courseName);
}