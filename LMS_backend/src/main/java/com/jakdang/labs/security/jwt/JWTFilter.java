package com.jakdang.labs.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakdang.labs.api.auth.dto.RoleType;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.config.SecurityConfig;
import com.jakdang.labs.exceptions.JwtExceptionCode;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.exceptions.handler.CustomException;
import com.jakdang.labs.exceptions.handler.JwtException;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenUtils tokenUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        String accessToken = extractAccessToken(request);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Access token 확인: {}", accessToken);

        try {
            log.debug("토큰 검증 시작: {}", accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            
            // Refresh 토큰도 Access 토큰으로 사용 (임시 해결책)
            boolean isValidToken = tokenUtils.isAccessTokenValid(accessToken) || 
                                  (accessToken != null && jwtUtil.isExpired(accessToken) == false) ||
                                  (accessToken != null && jwtUtil.isExpired(accessToken) == false && 
                                   ("refresh".equals(jwtUtil.getCategory(accessToken)) || "access".equals(jwtUtil.getCategory(accessToken))));
            
            if (isValidToken) {
                Authentication authentication = createAuthentication(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("사용자 인증 성공: {}", authentication.getName());
            } else {
                log.warn("토큰이 유효하지 않음");
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰");
            handleJwtException(response, JwtExceptionCode.EXPIRED_JWT_TOKEN);
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰}");
            handleJwtException(response, JwtExceptionCode.INVALID_JWT_TOKEN);
        } catch (SignatureException e) {
            log.error("JWT 서명 위조가 의심됩니다.");
            handleJwtException(response, JwtExceptionCode.SIGNATURE_VALIDATION_ERROR);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰  EX) 유저 롤값 정의");
            handleJwtException(response, JwtExceptionCode.INVALID_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰 처리 중 오류");
            handleJwtException(response, JwtExceptionCode.INVALID_JWT_TOKEN);
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 예상치 못한 오류");
            handleJwtException(response, JwtExceptionCode.INVALID_JWT_TOKEN);
        }
    }

    private void handleJwtException(HttpServletResponse response, JwtExceptionCode exceptionCode) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        objectMapper.writeValue(response.getOutputStream(),
                Map.of("error", exceptionCode.getMessage(), "message", exceptionCode.getMessage()));

        throw new JwtException(exceptionCode);
    }

    private String extractAccessToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 (우선순위 1)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!token.isEmpty() && !token.equals("undefined")) {
                log.debug("Authorization 헤더에서 토큰 추출: {}", token.substring(0, Math.min(20, token.length())) + "...");
                return token;
            }
        }
        
        // 2. 쿠키에서 토큰 추출 (우선순위 2)
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                log.debug("쿠키 확인: {} = {}", cookie.getName(), cookie.getValue() != null ? cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "..." : "null");
                
                if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                        log.debug("쿠키에서 토큰 추출: {} = {}", cookie.getName(), token.substring(0, Math.min(20, token.length())) + "...");
                        return token;
                    }
                }
            }
        }
        
        log.debug("토큰을 찾을 수 없음");
        return null;
    }

    private Authentication createAuthentication(String accessToken) {
        CustomUserDetails userDetails = buildUserDetails(accessToken);
        log.info(userDetails.toString());
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private CustomUserDetails buildUserDetails(String accessToken) {
        return new CustomUserDetails(
                UserEntity.builder()
                        .id(jwtUtil.getUserId(accessToken))
                        .name(jwtUtil.getUsername(accessToken))
                        .email(jwtUtil.getUserEmail(accessToken))
                        .role(RoleType.valueOf(jwtUtil.getRole(accessToken)))
                        .build()
                ,
                MemberEntity.builder()
                        .id(jwtUtil.getEducationId(accessToken))
                        .build()
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // QR 관련 엔드포인트는 JWT 필터를 거치지 않음
        if (path.startsWith("/api/student/qr/")) {
            return true;
        }

        // SecurityConfig에 정의된 PUBLIC_URLS 패턴 활용
        return Arrays.stream(SecurityConfig.PUBLIC_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path))
                || Arrays.stream(SecurityConfig.SWAGGER_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}