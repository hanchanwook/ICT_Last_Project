package com.jakdang.labs.exceptions.handler;

import com.jakdang.labs.exceptions.ErrorType;

import java.io.IOException;

public class CustomException extends RuntimeException {
    private final ErrorType errorCode;

    private String message;
    private int status;

    public CustomException(String message, int status) {
        super(message);
        this.message = message;
        this.status = status;
        this.errorCode = new ErrorType() {
            @Override
            public String getMessage() {
                return "";
            }

            @Override
            public int getStatus() {
                return 0;
            }
        };
    }

    public CustomException(ErrorType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorType errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

}
