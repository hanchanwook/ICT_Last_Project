package com.jakdang.labs.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JwtExceptionCode implements ErrorType {
    INVALID_JWT_TOKEN("유효하지 않은 JWT 토큰입니다.", 401),
    EXPIRED_JWT_TOKEN("만료된 JWT 토큰입니다.", 401),
    UNSUPPORTED_JWT_TOKEN("지원되지 않는 JWT 토큰입니다.", 401),
    EMPTY_JWT_CLAIMS("JWT 클레임이 비어있습니다.", 401),
    MALFORMED_JWT_TOKEN("잘못된 형식의 JWT 토큰입니다.", 401),
    SIGNATURE_VALIDATION_ERROR("JWT 서명 검증에 실패했습니다.", 401),
    MISSING_TOKEN("JWT 토큰이 누락되었습니다.", 401),
    INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다.", 401),
    EXPIRED_REFRESH_TOKEN("만료된 리프레시 토큰입니다.", 401),
    TOKEN_GENERATION_ERROR("JWT 토큰 생성에 실패했습니다.", 500),
    UNAUTHORIZED_ACCESS("인증되지 않은 접근입니다.", 401),
    TOKEN_BLACKLISTED("블랙리스트에 등록된 토큰입니다.", 401),
    INVALID_TOKEN_SUBJECT("토큰의 주체(Subject)가 유효하지 않습니다.", 401),
    INVALID_TOKEN_ISSUER("토큰의 발급자(Issuer)가 유효하지 않습니다.", 401),
    TOKEN_PARSE_ERROR("토큰 파싱에 실패했습니다.", 500);

    private final String message;
    private final int status;
}
