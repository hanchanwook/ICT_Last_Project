package com.jakdang.labs.api.jaegyeom.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public String sendVerificationCode(String email) {
        String code = generateCode();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("hussar12@naver.com");
        message.setTo(email);
        message.setSubject("비밀번호 재설정 인증번호");
        message.setText("인증번호: " + code + "\n5분 내에 입력해주세요");

        mailSender.send(message);
        return code;
    }

    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000); // 6자리
        return String.valueOf(num);
    }
}
