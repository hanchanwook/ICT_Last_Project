package com.jakdang.labs.exceptions.handler;

import com.jakdang.labs.exceptions.ErrorType;
import lombok.Getter;

import java.io.IOException;

@Getter
public class FileException extends IOException {
    private final ErrorType errorCode;

    public FileException(ErrorType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public FileException(ErrorType errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

}
