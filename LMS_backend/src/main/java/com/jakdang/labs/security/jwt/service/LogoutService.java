package com.jakdang.labs.security.jwt.service;

import com.jakdang.labs.api.auth.entity.UserToken;
import com.jakdang.labs.api.auth.repository.UserTokenRepository;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {

    private final TokenUtils tokenUtils;
    private final UserTokenRepository userTokenRepository;

    /**
     * 로그아웃 프로세스를 처리합니다.
     *
     * @param refreshToken 리프레시 토큰
     * @throws JwtException 토큰이 유효하지 않거나 처리할 수 없는 경우
     */
    @Transactional
    public void processLogout(String refreshToken) {
        if (refreshToken == null) {
            throw new JwtException("리프레시 토큰이 제공되지 않았습니다");
        }

        // 토큰 유효성 검증
        tokenUtils.validateRefreshToken(refreshToken);

        // 토큰 삭제
        deleteRefreshToken(refreshToken);

        log.info("리프레시 토큰이 성공적으로 무효화되었습니다");
    }

    /**
     * 리프레시 토큰을 저장소에서 삭제합니다.
     *
     * @param refreshToken 삭제할 리프레시 토큰
     * @throws JwtException 토큰을 찾을 수 없는 경우
     */
    private void deleteRefreshToken(String refreshToken) {
        UserToken refreshEntity = userTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new JwtException("리프레시 토큰을 찾을 수 없습니다"));

        userTokenRepository.delete(refreshEntity);
        log.debug("리프레시 토큰이 저장소에서 삭제되었습니다");
    }
}