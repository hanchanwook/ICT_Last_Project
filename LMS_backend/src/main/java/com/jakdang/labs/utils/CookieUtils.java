package com.jakdang.labs.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
public class CookieUtils {

    private static ObjectMapper objectMapper;

    @Autowired
    public CookieUtils(ObjectMapper objectMapper) {
        CookieUtils.objectMapper = objectMapper;
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
    }

    public static String serialize(Object object) {
        try {
            if (objectMapper == null) {
                throw new IllegalStateException("ObjectMapper가 초기화되지 않았습니다. CookieUtils가 빈으로 등록되어 있는지 확인해주세요.");
            }

            String serialized = Base64.getUrlEncoder().encodeToString(
                    objectMapper.writeValueAsBytes(object));
            return serialized;
        } catch (Exception e) {
            log.error("직렬화 오류: {}", e.getMessage(), e);
            throw new RuntimeException("직렬화 오류", e);
        }
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try {
            if (objectMapper == null) {
                throw new IllegalStateException("ObjectMapper가 초기화되지 않았습니다. CookieUtils가 빈으로 등록되어 있는지 확인해주세요.");
            }

            byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
            T result = objectMapper.readValue(decodedBytes, cls);
            return result;
        } catch (IOException e) {
            log.error("역직렬화 IO 오류: {}", e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Base64 디코딩 오류: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("기타 역직렬화 오류: {}", e.getMessage(), e);
            return null;
        }
    }
}
