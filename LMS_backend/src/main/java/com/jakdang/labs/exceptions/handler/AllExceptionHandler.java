package com.jakdang.labs.exceptions.handler;

import com.jakdang.labs.exceptions.ErrorResponse;
import com.jakdang.labs.exceptions.ErrorType;
import com.jakdang.labs.exceptions.ResultMessageEnum;
import com.jakdang.labs.exceptions.ValidationError;
import com.jakdang.labs.api.common.ResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class AllExceptionHandler {

    @ExceptionHandler({AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ResponseDTO<?>> handleAuthorizationException(AuthorizationDeniedException e, HttpServletRequest request){

        log.error("AuthorizationDeniedException 발생 [권한 에러 ROLE값 확인 요청] - Error Code: 401, URI: {}", request.getRequestURI());
        return ResponseEntity
                .status(-401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseDTO.createErrorResponse(401, e.getMessage()));
    }

    @ExceptionHandler({FileException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse<ErrorType>> handleFileException(FileException e, HttpServletRequest request) {
        ErrorResponse<ErrorType> response = ErrorResponse.of(
                e.getErrorCode(),
                request.getRequestURI()
        );
        log.error("FileException 발생 - Error Code: {}, URI: {}", e.getErrorCode(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler({JwtException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse<ErrorType>> handleJwtException(JwtException e, HttpServletRequest request) {
        ErrorResponse<ErrorType> response = ErrorResponse.of(
                e.getErrorCode(),
                request.getRequestURI()
        );
        log.error("JwtException 발생 - Error Code: {}, URI: {}", e.getErrorCode(), request.getRequestURI());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // JSON 파싱 오류 처리
    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseDTO<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("JSON 파싱 오류 발생 - URI: {}, Error: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseDTO.createErrorResponse(400, "잘못된 JSON 형식입니다: " + e.getMessage()));
    }

    // 파라미터 바인딩 오류 처리
    @ExceptionHandler({MissingPathVariableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseDTO<?>> handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        log.error("필수 경로 변수 누락 - URI: {}, Variable: {}", request.getRequestURI(), e.getVariableName());
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseDTO.createErrorResponse(400, "필수 경로 변수가 누락되었습니다: " + e.getVariableName()));
    }

    // 파라미터 타입 불일치 오류 처리
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseDTO<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("파라미터 타입 불일치 - URI: {}, Parameter: {}, Value: {}", request.getRequestURI(), e.getName(), e.getValue());
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseDTO.createErrorResponse(400, "잘못된 파라미터 형식입니다: " + e.getName()));
    }

    // 필수 요청 파라미터 누락 오류 처리
    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseDTO<?>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.error("필수 요청 파라미터 누락 - URI: {}, Parameter: {}", request.getRequestURI(), e.getParameterName());
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseDTO.createErrorResponse(400, "필수 파라미터가 누락되었습니다: " + e.getParameterName()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<ErrorResponse<ErrorType>> handleValidationException(MethodArgumentNotValidException e) {
        List<ValidationError> validationErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ValidationError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .toList();

        ErrorResponse<ErrorType> response = ErrorResponse.builder()
                .statusCode(400)
                .message("입력값이 올바르지 않습니다.")
                .errors(validationErrors)
                .build();

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler({Exception.class})
    protected ResponseEntity<ResponseDTO<?>> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("알 수 없는 에러 발생 에러 메세지 : ======> {} {}", requestURI, e.getMessage(), e);
        return ResponseEntity
                .status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseDTO.createErrorResponse(500, "서버 오류가 발생했습니다: " + e.getMessage()));
    }
}