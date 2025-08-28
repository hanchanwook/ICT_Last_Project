package com.jakdang.labs.api.jaegyeom.mypage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.api.auth.entity.UserEntity;

import java.util.Optional;

public interface ShowMyInfoByUserRepo extends JpaRepository<UserEntity, String> {
    UserEntity findByEmail(String email);
    Optional<UserEntity> findById(String userId);
}
