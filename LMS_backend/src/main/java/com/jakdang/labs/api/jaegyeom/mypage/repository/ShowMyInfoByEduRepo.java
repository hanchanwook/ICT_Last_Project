package com.jakdang.labs.api.jaegyeom.mypage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.entity.EducationEntity;

public interface ShowMyInfoByEduRepo extends JpaRepository<EducationEntity, String> {
    EducationEntity findByEducationId(String educationId);
}
