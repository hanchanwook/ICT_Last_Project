package com.jakdang.labs.api.jaegyeom.mypage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import com.jakdang.labs.api.jaegyeom.mypage.dto.MyPageCrudRequestDto;
import com.jakdang.labs.api.jaegyeom.mypage.dto.MyPageCrudResponseDto;
import com.jakdang.labs.api.jaegyeom.mypage.repository.ShowMyInfoByUserRepo;
import com.jakdang.labs.api.jaegyeom.mypage.repository.ShowMyInfoByMemberRepo;
import com.jakdang.labs.api.auth.entity.UserEntity;
import java.time.LocalDateTime;
import com.jakdang.labs.entity.MemberEntity;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageCrudService {
    @Autowired
    private final ShowMyInfoByUserRepo showMyInfoByUserRepo;
    @Autowired
    private final ShowMyInfoByMemberRepo showMyInfoByMemberRepo;
    @Autowired
    private final Argon2PasswordEncoder passwordEncoder;

    @Transactional
public MyPageCrudResponseDto updateUserInfo(MyPageCrudRequestDto request) {
    UserEntity user = showMyInfoByUserRepo.findByEmail(request.getEmail());
    if (user == null) throw new RuntimeException("사용자를 찾을 수 없습니다.");
    MemberEntity member = showMyInfoByMemberRepo.findByMemberEmail(request.getEmail());
    if (member == null) throw new RuntimeException("회원 정보를 찾을 수 없습니다.");

    // 데이터 업데이트
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPhone(request.getPhone());
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
        user.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    member.setMemberName(request.getName());
    member.setPassword(user.getPassword());
    member.setMemberEmail(request.getEmail());
    member.setMemberPhone(request.getPhone());
    member.setMemberAddress(request.getMemberAddress());

    showMyInfoByUserRepo.save(user);
    showMyInfoByMemberRepo.save(member);

    return MyPageCrudResponseDto.builder()
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .memberAddress(member.getMemberAddress())
            .build();
}

@Transactional
public MyPageCrudResponseDto deleteUserInfo(MyPageCrudRequestDto request) {
    UserEntity user = showMyInfoByUserRepo.findByEmail(request.getEmail());
    if (user == null) throw new RuntimeException("사용자를 찾을 수 없습니다.");
    MemberEntity member = showMyInfoByMemberRepo.findByMemberEmail(request.getEmail());
    if (member == null) throw new RuntimeException("회원 정보를 찾을 수 없습니다.");

    user.setActivated(false);
    member.setMemberExpired(LocalDateTime.now());

    showMyInfoByUserRepo.save(user);
    showMyInfoByMemberRepo.save(member);

    return MyPageCrudResponseDto.builder()
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .memberAddress(member.getMemberAddress())
            .build();
}
}
