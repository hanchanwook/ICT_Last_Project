package com.jakdang.labs.api.auth.service;

import com.jakdang.labs.api.auth.dto.TokenDTO;
import com.jakdang.labs.api.auth.entity.UserToken;
import com.jakdang.labs.api.auth.repository.UserTokenRepository;
import com.jakdang.labs.security.jwt.service.TokenService;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final TokenUtils tokenUtils;
    private final TokenService tokenService;
    private final UserTokenRepository userTokenRepository;

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.
     *
     * @param cookies 요청에서 전달된 쿠키 배열
     * @param response HTTP 응답 객체
     * @return 새로 발급된 액세스 토큰, 새 토큰이 발급되지 않았다면 null
     */
    @Transactional
    public String refreshTokens(Cookie[] cookies, HttpServletResponse response) {
        String refreshToken = tokenUtils.extractRefreshToken(cookies);

        if (refreshToken == null) {
            log.warn("리프레시 토큰이 제공되지 않았습니다");
            return null;
        }

        try {
            // 리프레시 토큰 검증
            tokenUtils.validateRefreshToken(refreshToken);

            // 새 토큰 쌍 생성
            TokenDTO tokenPair = tokenService.refreshTokenPair(refreshToken);

            // 응답에 새 리프레시 토큰 쿠키 추가
            tokenUtils.addRefreshTokenCookie(response, tokenPair.getRefreshToken());

            log.info("토큰이 성공적으로 갱신되었습니다");
            return tokenPair.getAccessToken();

        } catch (JwtException e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 리프레시 토큰의 유효성을 검증하고 해당 엔티티를 반환합니다.
     *
     * @param refreshToken 검증할 리프레시 토큰
     * @return 리프레시 토큰 엔티티
     * @throws JwtException 토큰이 유효하지 않거나 저장소에 없는 경우
     */
    public UserToken validateAndGetRefreshEntity(String refreshToken) {
        tokenUtils.validateRefreshToken(refreshToken);

        return userTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new JwtException("리프레시 토큰을 저장소에서 찾을 수 없습니다"));
    }
}