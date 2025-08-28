package com.jakdang.labs.api.jaegyeom.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.jaegyeom.auth.common.VerificationCodeStore;
import com.jakdang.labs.api.jaegyeom.auth.dto.UserCheckRequestDto;
import com.jakdang.labs.api.jaegyeom.auth.dto.VerifyCodeRequestDto;
import com.jakdang.labs.api.jaegyeom.auth.service.FindMyInfoService;
import com.jakdang.labs.api.jaegyeom.auth.service.MailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/find")
@RequiredArgsConstructor
public class FindMyInfoController {
    
    private final FindMyInfoService findMyInfoService;
    private final MailService mailService;
    private final VerificationCodeStore codeStore;

    @PostMapping("/user-check")
    public ResponseEntity<String> checkUser(@RequestBody UserCheckRequestDto dto) {
        boolean exists = findMyInfoService.checkUser(dto);
        if (exists) {
            return ResponseEntity.ok("사용자가 존재합니다.");
        } else {
            return ResponseEntity.status(404).body("사용자가 찾을 수 없습니다.");
        }
    }

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String code = mailService.sendVerificationCode(email);
        codeStore.saveCode(email, code);
        return ResponseEntity.ok("인증번호가 이메일로 발송되었습니다.");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyCodeRequestDto dto) {
        String email = dto.getEmail();
        String code = dto.getCode();

        boolean verified = codeStore.verifyCode(email, code);
        if (verified) {
            return ResponseEntity.ok("인증 성공");  
        } else {
            return ResponseEntity.status(400).body("인증 실패");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String newPassword = req.get("newPassword");

        boolean result = findMyInfoService.resetPassword(email, newPassword);
        if (result) {
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } else {
            return ResponseEntity.status(400).body("사용자를 찾을 수 없습니다.");
        }
    }
}
