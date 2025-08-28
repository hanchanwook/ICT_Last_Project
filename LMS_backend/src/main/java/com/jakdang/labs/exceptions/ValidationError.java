package com.jakdang.labs.exceptions;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidationError {
    private final String field;
    private final String message;
    private final Object rejectedValue;
}
