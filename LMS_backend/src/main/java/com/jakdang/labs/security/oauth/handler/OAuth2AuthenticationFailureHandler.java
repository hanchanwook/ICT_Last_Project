//package com.jakdang.labs.security.oauth.handler;
//
//import com.jakdang.labs.security.oauth.service.HttpCookieOAuth2AuthorizationRequestRepository;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
//import org.springframework.stereotype.Component;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.io.IOException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
//
//    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
//
//    @Value("${app.frontend-url}")
//    private String frontendUrl;
//    private String REDIRECT_URI = frontendUrl + "/login";
//
//    @Override
//    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
//            throws IOException, ServletException {
//
//        log.error("OAuth2 인증 실패: {}", exception.getMessage(), exception);
//
//        try {
//            // 예외 정보 로깅
//            log.error("인증 실패 예외 유형: {}", exception.getClass().getName());
//            if (exception.getCause() != null) {
//                log.error("원인 예외: {}", exception.getCause().getMessage());
//            }
//
//            // 요청 정보 로깅
//            log.debug("요청 URI: {}", request.getRequestURI());
//            log.debug("요청 메서드: {}", request.getMethod());
//
//            // 요청에서 쿠키 정보 확인
//            if (request.getCookies() != null) {
//                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
//                    log.debug("쿠키 정보 - 이름: {}, 값 길이: {}", cookie.getName(),
//                            (cookie.getValue() != null ? cookie.getValue().length() : 0));
//                }
//            } else {
//                log.debug("요청에 쿠키 정보 없음");
//            }
//
//            // 인증 요청 쿠키 제거
//            authorizationRequestRepository.removeAuthorizationRequest(request, response);
//            log.debug("인증 요청 쿠키 제거 완료");
//
//        } catch (Exception e) {
//            log.error("쿠키 처리 중 오류 발생", e);
//        }
//
//        // 에러 메시지 인코딩
//        String errorMessage = URLEncoder.encode(
//                (exception.getMessage() != null && !exception.getMessage().isEmpty())
//                        ? exception.getMessage()
//                        : "인증에 실패했습니다.",
//                StandardCharsets.UTF_8
//        );
//
//        // 에러 코드 추가
//        String errorCode = "auth_failure";
//        if (exception.getMessage() != null && exception.getMessage().contains("authorization_request_not_found")) {
//            errorCode = "session_expired";
//            errorMessage = URLEncoder.encode("인증 세션이 만료되었습니다. 다시 시도해주세요.", StandardCharsets.UTF_8);
//        }
//
//        // 리다이렉트 URL 생성
//        String targetUrl = UriComponentsBuilder.fromUriString(REDIRECT_URI)
//                .queryParam("error", errorMessage)
//                .queryParam("error_code", errorCode)
//                .build().toUriString();
//
//        log.debug("리다이렉트 URL: {}", targetUrl);
//
//        // 리다이렉트
//        getRedirectStrategy().sendRedirect(request, response, targetUrl);
//    }
//}