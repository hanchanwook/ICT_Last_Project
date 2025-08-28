package com.jakdang.labs.api.jaegyeom.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jakdang.labs.api.auth.entity.UserEntity;
import java.util.List;

public interface CheckEmailAndPhoneRepo extends JpaRepository<UserEntity, String> {
    UserEntity findByEmail(String email);
    UserEntity findByPhone(String phone);
    List<UserEntity> findAllByPhone(String phone);
}
