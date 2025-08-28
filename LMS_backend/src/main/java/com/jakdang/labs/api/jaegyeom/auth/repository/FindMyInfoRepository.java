package com.jakdang.labs.api.jaegyeom.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.api.auth.entity.UserEntity;

public interface FindMyInfoRepository extends JpaRepository<UserEntity, String> {
    UserEntity findByNameAndEmail(String name, String email);
    UserEntity findByEmail(String email);
}
