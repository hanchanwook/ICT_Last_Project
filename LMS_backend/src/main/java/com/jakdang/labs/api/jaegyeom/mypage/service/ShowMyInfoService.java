package com.jakdang.labs.api.jaegyeom.mypage.service;

import com.jakdang.labs.api.jaegyeom.mypage.dto.MyPageRequestDto;
import com.jakdang.labs.api.jaegyeom.mypage.dto.UserAndEduDto;
import com.jakdang.labs.api.jaegyeom.mypage.repository.ShowMyInfoByEduRepo;
import com.jakdang.labs.api.jaegyeom.mypage.repository.ShowMyInfoByUserRepo;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.entity.EducationEntity;
import com.jakdang.labs.api.common.EducationId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShowMyInfoService {
    @Autowired
    private ShowMyInfoByUserRepo showMyInfoByUserRepo;

    @Autowired
    private ShowMyInfoByEduRepo showMyInfoByEduRepo;
    
    @Autowired
    private EducationId educationId;
    
    public UserAndEduDto getUserInfo(MyPageRequestDto request) {
        UserEntity user = null;
        
        // userId가 있으면 userId로 사용자 찾기
        if (request.getUserId() != null && !request.getUserId().trim().isEmpty()) {
            Optional<UserEntity> userOpt = showMyInfoByUserRepo.findById(request.getUserId());
            if (userOpt.isPresent()) {
                user = userOpt.get();
            }
        }
        
        // userId로 찾지 못했고 email이 있으면 email로 사용자 찾기
        if (user == null && request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user = showMyInfoByUserRepo.findByEmail(request.getEmail());
        }
        
        // 사용자가 존재하지 않는 경우 예외 처리
        if (user == null) {
            String errorMsg = "사용자를 찾을 수 없습니다.";
            if (request.getUserId() != null) {
                errorMsg += " userId: " + request.getUserId();
            }
            if (request.getEmail() != null) {
                errorMsg += " email: " + request.getEmail();
            }
            throw new RuntimeException(errorMsg);
        }
        
        // UserAndEduDto 빌더 시작
        UserAndEduDto.UserAndEduDtoBuilder builder = UserAndEduDto.builder()
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().toString())
            .phone(user.getPhone());
        
        // educationId 결정 로직
        String finalEducationId = null;
        
        // 1. 요청에 educationId가 있으면 우선 사용
        if (request.getEducationId() != null && !request.getEducationId().trim().isEmpty()) {
            finalEducationId = request.getEducationId();
        }
        // 2. 요청에 educationId가 없으면 사용자 ID로부터 자동 조회
        else if (user.getId() != null) {
            Optional<String> educationIdOpt = educationId.getEducationIdByUserId(user.getId());
            if (educationIdOpt.isPresent()) {
                finalEducationId = educationIdOpt.get();
            }
        }
        
        // 최종 educationId로 교육 정보 찾기
        if (finalEducationId != null && !finalEducationId.trim().isEmpty()) {
            EducationEntity education = showMyInfoByEduRepo.findByEducationId(finalEducationId);
            
            if (education != null) {
                builder.educationId(education.getEducationId())
                       .educationName(education.getEducationName());
            } else {
                // 교육 정보가 없으면 educationId만 설정
                builder.educationId(finalEducationId)
                       .educationName(null);
            }
        } else {
            // educationId가 없으면 null로 설정
            builder.educationId(null)
                   .educationName(null);
        }
        
        return builder.build();
    }
}