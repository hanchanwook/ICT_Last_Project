package com.jakdang.labs.api.auth.repository;

import com.jakdang.labs.api.auth.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, String> {
    Optional<UserToken> findByRefreshToken(String refreshToken);
}
