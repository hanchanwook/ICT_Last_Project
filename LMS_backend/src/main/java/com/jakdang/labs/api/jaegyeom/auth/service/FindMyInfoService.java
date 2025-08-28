package com.jakdang.labs.api.jaegyeom.auth.service;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jakdang.labs.api.jaegyeom.auth.dto.UserCheckRequestDto;
import com.jakdang.labs.api.jaegyeom.auth.repository.FindMyInfoRepository;
import com.jakdang.labs.api.jaegyeom.auth.repository.FindMyMemberInfoRepository;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.entity.MemberEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindMyInfoService {
    private final FindMyInfoRepository findMyInfoRepository;
    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final FindMyMemberInfoRepository findMyMemberInfoRepository;

    public boolean checkUser(UserCheckRequestDto request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim() : "";

        UserEntity user = findMyInfoRepository.findByNameAndEmail(name, email);

        return user != null;
    }

    public boolean resetPassword(String email, String newPassword) {
        UserEntity user = findMyInfoRepository.findByEmail(email);
        if (user == null) return false;

        String encodedPassword = argon2PasswordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        findMyInfoRepository.save(user);

        MemberEntity member = findMyMemberInfoRepository.findByMemberEmail(email);
        if (member == null) {
            member.setPassword(encodedPassword);
            findMyMemberInfoRepository.save(member);
        }

        return true;
    }
}
