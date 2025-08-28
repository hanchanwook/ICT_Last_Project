package com.jakdang.labs.api.auth.repository;

import com.jakdang.labs.api.auth.dto.UserDTO;
import com.jakdang.labs.api.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<UserEntity, String> {
    boolean existsByEmail(String email);
    Optional<UserEntity> findByEmail(String email);
    boolean existsById(String id);

    List<UserEntity> findByIdIn(List<String> likedUserIds);
}
