package com.jakdang.labs.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.auth.dto.TokenDTO;
import com.jakdang.labs.api.auth.dto.UserLoginRequest;
import com.jakdang.labs.api.common.ResponseDTO;
// import com.jakdang.labs.api.jaegyeom.common.GetMemberIdService;
import com.jakdang.labs.security.jwt.service.TokenService;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final TokenUtils tokenUtils;
    private final ObjectMapper objectMapper;
    // private final GetMemberIdService getMemberIdService;
    public LoginFilter(AuthenticationManager authenticationManager,
                       TokenService tokenService,
                       TokenUtils tokenUtils,
                       ObjectMapper objectMapper
                       // GetMemberIdService getMemberIdService
                       ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.tokenUtils = tokenUtils;
        this.objectMapper = objectMapper;
        // this.getMemberIdService = getMemberIdService;
        this.setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        try {
            UserLoginRequest loginRequest = objectMapper.readValue(
                    request.getInputStream(),
                    UserLoginRequest.class
            );

            log.info("로그인 시도: {} {}", loginRequest.getUsername(), loginRequest.getPassword());

            // db에 저장된 역할 추출
            try {
                // String requestedRole = loginRequest.getRole();
                UserDetails userDetails = (UserDetails) authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsername(),
                                loginRequest.getPassword()
                        )
                ).getPrincipal();

                String storedRole = userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(GrantedAuthority::getAuthority)
                        .orElse(null);

                log.info("저장된 역할: {}", storedRole);
            } catch (Exception e) {
                log.error("역할 유효성 검사 중 오류 발생", e);
                throw new RuntimeException("역할이 불일치합니다", e);
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword(),
                    null
            );

            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            log.error("로그인 요청 처리 중 오류 발생", e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다", e);
        }
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        if (!userDetails.isEnabled()) {
            log.warn("활성화되지 않은 사용자 {} 로그인 시도", userDetails.getUsername());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(),
                    Map.of("error", "인증 실패", "message", "활성화되지 않은 계정입니다."));
            return;
        }

        String username = authentication.getName();
        String role = extractRole(authentication);
        String userId = extractUserId(authentication);
        String email = extractUserEmail(authentication);
        String educationId = extractEducationId(authentication);

        log.info("사용자 {} {} {} {} {} 로그인 성공", username, role, userId, email, educationId);
        System.out.println("사용자 " + username + " " + role + " " + userId + " " + email + " 로그인 성공");
        
        log.info("토큰 생성 및 저장 시작");
        TokenDTO tokenPair = tokenService.createTokenPair(username, role, email, userId, educationId);
        log.info("토큰 생성 완료", tokenPair);


        // 응답 설정
        response.setStatus(HttpStatus.OK.value());
        response.setHeader("Authorization", "Bearer " + tokenPair.getAccessToken());
        tokenUtils.addRefreshTokenCookie(response, tokenPair.getRefreshToken());

        // user의 id를 받았던거 member의 memeberId로 변경(용호님 요청)
        // String memberId = getMemberIdService.getMemberId(userId);

        // 로그인 성공 응답 작성
        writeSuccessResponse(response, username, role, /*memberId*/userId);
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        log.warn("로그인 실패: {}", exception.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        objectMapper.writeValue(response.getOutputStream(),
                Map.of("error", "인증 실패", "message", "사용자 이름 또는 비밀번호가 잘못되었습니다"));
    }

    private String extractRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("역할이 없습니다");
    }

    private String extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }

    private String extractUserEmail(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getEmail();
    }

    private String extractEducationId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getEducationId();
    }

    private void writeSuccessResponse(HttpServletResponse response, String username, String role, String userId) throws IOException {
        response.setContentType("application/json");

        Map<String, Object> userData = Map.of(
            "name", username,
            "role", role,
            "memberId", userId
        );

        objectMapper.writeValue(response.getOutputStream(),
            ResponseDTO.createSuccessResponse("로그인 성공", userData));
    }
}