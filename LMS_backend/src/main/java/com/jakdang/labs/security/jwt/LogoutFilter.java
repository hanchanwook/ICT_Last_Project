package com.jakdang.labs.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jakdang.labs.exceptions.JwtExceptionCode;
import com.jakdang.labs.security.jwt.service.LogoutService;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutFilter extends OncePerRequestFilter {

    private final LogoutService logoutService;
    private final TokenUtils tokenUtils;
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isLogoutRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("로그아웃 요청 처리 시작");

        try {
            String refreshToken = tokenUtils.extractRefreshToken(request.getCookies());
            logoutService.processLogout(refreshToken);

            Cookie logoutCookie = tokenUtils.createLogoutCookie();
            response.addCookie(logoutCookie);

            sendSuccessResponse(response);
            log.info("로그아웃 처리 완료");

        } catch (JwtException e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            sendErrorResponse(response, e.getMessage());
        }
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/auth/logout") && request.getMethod().equals("POST");
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");

        objectMapper.writeValue(response.getOutputStream(),
                Map.of("status", "success", "message", "로그아웃 완료"));
    }

    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");

        objectMapper.writeValue(response.getOutputStream(),
                Map.of("status", "error", "message", errorMessage));
    }
}