package com.jakdang.labs.security.jwt.service;

import com.jakdang.labs.api.auth.dto.TokenDTO;
import com.jakdang.labs.api.auth.entity.UserToken;
import com.jakdang.labs.api.auth.repository.UserTokenRepository;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${spring.jwt.access.expired}")
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh.expired}")
    private long refreshTokenExpiration;

    private final JwtUtil jwtUtil;
    private final UserTokenRepository userTokenRepository;


    @Transactional
    public TokenDTO createTokenPair(String username, String role, String email, String userId, String educationId) {
        String accessToken = createAccessToken(username, role, email, userId, educationId);
        String refreshToken = createRefreshToken(username, role, email, userId, educationId);

        saveTokenPair(accessToken, refreshToken);

        return new TokenDTO(accessToken, refreshToken);
    }

    @Transactional
    public TokenDTO refreshTokenPair(String refreshToken) {
        UserToken userToken = findRefreshEntity(refreshToken);

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        String userId = jwtUtil.getUserId(refreshToken);
        String email = jwtUtil.getUserEmail(refreshToken);
        String educationId = jwtUtil.getEducationId(refreshToken);

        String newAccessToken = createAccessToken(username, role, email, userId, educationId);
        String newRefreshToken = createRefreshToken(username, role, email, userId, educationId);

        updateRefreshEntity(userToken, newAccessToken, newRefreshToken);

        return new TokenDTO(newAccessToken, newRefreshToken);
    }

    @Transactional
    public TokenDTO createTokenPairForSocial(String username, String role, String email, String userId) {
        String accessToken = createAccessTokenForSocial(username, role, email, userId);
        String refreshToken = createRefreshTokenForSocial(username, role, email, userId);

        saveTokenPair(accessToken, refreshToken);

        return new TokenDTO(accessToken, refreshToken);
    }

    private UserToken findRefreshEntity(String refreshToken) {
        return userTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("리프레시 토큰을 찾을 수 없습니다"));
    }

    private String createAccessToken(String username, String role, String email, String userId, String educationId) {
        return jwtUtil.createJwt("access", username, role, email, userId, educationId, accessTokenExpiration);
    }

    private String createRefreshToken(String username, String role, String email, String userId, String educationId) {
        return jwtUtil.createJwt("refresh", username, role, email, userId, educationId, refreshTokenExpiration);
    }

    private String createAccessTokenForSocial(String username, String role, String email, String userId) {
        return jwtUtil.createJwtForSocial("access", username, role, email, userId, accessTokenExpiration);
    }

    private String createRefreshTokenForSocial(String username, String role, String email, String userId) {
        return jwtUtil.createJwtForSocial("refresh", username, role, email, userId, refreshTokenExpiration);
    }

    @Transactional
    protected void saveTokenPair(String accessToken, String refreshToken) {
        // TODO 값 넣기
        UserToken refreshEntity = UserToken.builder()
                .userId(jwtUtil.getUserId(accessToken))
                .refreshToken(refreshToken)
                .build();

        userTokenRepository.save(refreshEntity);
    }

    @Transactional
    protected void updateRefreshEntity(UserToken oldEntity, String newAccessToken, String newRefreshToken) {
        oldEntity.setRefreshToken(newRefreshToken);
    }
}