package com.jakdang.labs.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultMessageEnum {
    STATUS_SUCCESS("데이터 응답 성공", 200),
    STATUS_ERROR("데이터 응답 에러", 500),
    STATUS_FAILED("데이터 응답 실패", 400);

    private final String message;
    private final int status;
}
