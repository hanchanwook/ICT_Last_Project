package com.jakdang.labs.api.jaegyeom.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jakdang.labs.entity.EducationEntity;

@Repository
public interface EduRegisterRepository extends JpaRepository<EducationEntity, String> {

    EducationEntity findByEducationName(String educationName);
    EducationEntity findByEducationId(String educationId);
}