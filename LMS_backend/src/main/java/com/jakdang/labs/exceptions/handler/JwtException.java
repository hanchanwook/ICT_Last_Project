package com.jakdang.labs.exceptions.handler;

import com.jakdang.labs.exceptions.ErrorType;
import lombok.Getter;

import javax.security.sasl.AuthenticationException;

@Getter
public class JwtException extends AuthenticationException {

    private final ErrorType errorCode;

    public JwtException(ErrorType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JwtException(ErrorType errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

}
