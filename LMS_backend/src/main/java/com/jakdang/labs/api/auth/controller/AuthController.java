package com.jakdang.labs.api.auth.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.jakdang.labs.aop.RunningTime;
import com.jakdang.labs.api.auth.dto.*;
import com.jakdang.labs.api.auth.service.AuthService;
import com.jakdang.labs.api.auth.service.RefreshTokenService;
import com.jakdang.labs.api.common.ResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    @RunningTime
    public ResponseEntity<ResponseDTO<?>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = refreshTokenService.refreshTokens(request.getCookies(), response);
        response.setHeader("Authorization", "Bearer " + accessToken);
        return ResponseEntity.ok().body(ResponseDTO.createSuccessResponse("리프레시 토큰 갱신완료", accessToken));
    }

    @PostMapping(value = "/signUp")
    @RunningTime
    public ResponseEntity<ResponseDTO<?>> signUp(@RequestBody SignUpDTO signUpDTO) {
        if ( signUpDTO == null ) {
            return ResponseEntity.badRequest().body(ResponseDTO.builder()
                            .resultCode(400)
                            .resultMessage("요청값이 비어있습니다.")
                    .build());
        }

        return ResponseEntity.ok().body(authService.signUpUser(signUpDTO));
    }

//    @PostMapping(value = "/join-sns")
//    @RunningTime
//    public ResponseEntity<ResponseDTO<?>> joinSns(@RequestHeader Map<String, Object> header,
//                                                  @RequestBody SnsSignInRequest dto, HttpServletResponse res) throws FirebaseAuthException {
//        if ( dto == null ){
//            return ResponseEntity.badRequest().body(
//                    ResponseDTO.createErrorResponse(-200, "요청값이 비어있습니다."));
//        }
//
//        return ResponseEntity.ok().body(authService.joinSns(header, dto, res));
//    }

    @PostMapping(value = "/join-sns2")
    @RunningTime
    public ResponseEntity<ResponseDTO<?>> joinApple(@RequestHeader Map<String, Object> header,
                                                  @RequestBody UserDTO dto){
        if ( dto == null ){
            return ResponseEntity.badRequest().body(
                    ResponseDTO.createErrorResponse(-200, "요청값이 비어있습니다."));
        }

        return ResponseEntity.ok().body(authService.joinApple(header, dto));
    }


    @GetMapping(value = "/get-info")
    @RunningTime
    public ResponseEntity<ResponseDTO<?>> getUserInfo(@AuthenticationPrincipal CustomUserDetails details){
        if ( details == null ){
            return ResponseEntity.badRequest().body(
                    ResponseDTO.createErrorResponse(-200, "인증/인가되지 않은 사용자"));
        }
        return ResponseEntity.ok().body(authService.getUserInfo(details.getUserId()));
    }

    @PutMapping(value = "/info/{id}")
    @RunningTime
    public ResponseEntity<ResponseDTO<?>> updateUserInfo(@RequestBody UserUpdateDTO dto,
                                                         @PathVariable("id") String id,
                                                         @AuthenticationPrincipal CustomUserDetails details){
        if ( dto == null){
            return ResponseEntity.badRequest().body(
                    ResponseDTO.createErrorResponse(-200, "요청값이 비어있습니다."));
        }

        return ResponseEntity.ok().body(authService.updateUserInfo(dto, id));
    }

    @GetMapping(value = "/find/user-check", consumes = {"application/json", "text/plain", "*/*"})
    @RunningTime
    public ResponseEntity<ResponseDTO<?>> checkUser(@RequestParam String userId,
                                                   @AuthenticationPrincipal CustomUserDetails details) {
        if (details == null) {
            return ResponseEntity.badRequest().body(
                    ResponseDTO.createErrorResponse(-200, "인증되지 않은 사용자"));
        }
        
        try {
            // 사용자 정보 조회
            ResponseDTO<?> userInfo = authService.getUserInfo(userId);
            return ResponseEntity.ok().body(userInfo);
        } catch (Exception e) {
            log.error("사용자 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ResponseDTO.createErrorResponse(-200, "사용자 확인에 실패했습니다"));
        }
    }
}