//package com.jakdang.labs.security.oauth.service;
//
//import com.jakdang.labs.utils.CookieUtils;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
//import org.springframework.stereotype.Component;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import java.util.Optional;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
//
//    private final CookieUtils cookieUtils;
//
//    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
//    private static final int COOKIE_EXPIRE_SECONDS = 180;
//
//    @Override
//    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
//        try {
//            Optional<OAuth2AuthorizationRequest> requestOptional = CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
//                    .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class));
//
//            return requestOptional.orElse(null);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Override
//    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
//        if (authorizationRequest == null) {
//            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
//            return;
//        }
//
//        try {
//            CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
//                    CookieUtils.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
//        } catch (Exception e) {
//        }
//    }
//
//    @Override
//    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
//        try {
//            OAuth2AuthorizationRequest authRequest = this.loadAuthorizationRequest(request);
//            if (authRequest != null) {
//                CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
//                return authRequest;
//            } else {
//                return null;
//            }
//        } catch (Exception e) {
//            log.error("인증 요청 쿠키 삭제 중 오류: {}", e.getMessage(), e);
//            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
//            return null;
//        }
//    }
//}