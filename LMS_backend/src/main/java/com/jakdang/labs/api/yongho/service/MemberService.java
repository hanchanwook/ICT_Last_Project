package com.jakdang.labs.api.yongho.service;

import java.util.List;
import java.util.Optional;

import com.jakdang.labs.entity.MemberEntity;

public interface MemberService {

    List<MemberEntity> findByMemberRoleIn(List<String> memberRole);    
    
    // educationId로 필터링된 메서드
    List<MemberEntity> findByMemberRoleInAndEducationId(List<String> memberRole, String educationId);

    Optional<MemberEntity> findByUserId(String userId);
}
