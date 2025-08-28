package com.jakdang.labs.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse<T extends ErrorType>{

    private final LocalDateTime date = LocalDateTime.now();
    private final int statusCode;
    private final String message;
    private final String path;
    private final ErrorType errorCode;
    private final List<ValidationError> errors;

    public static <T extends ErrorType> ErrorResponse<T> of(T errorCode, String path){
        return ErrorResponse.<T>builder()
                .message(errorCode.getMessage())
                .errorCode(errorCode)
                .path(path)
                .statusCode(errorCode.getStatus())
                .build();
    }

}