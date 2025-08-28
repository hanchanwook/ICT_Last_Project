package com.jakdang.labs.api.yongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.MemberEntity;

@Repository
public interface MembersRepository extends JpaRepository<MemberEntity, String> {
    
    List<MemberEntity> findByMemberRoleIn(List<String> memberRole);
    
    // educationId와 역할로 필터링
    List<MemberEntity> findByMemberRoleInAndEducationId(List<String> memberRole, String educationId);

    // 기존 PK(memberId) 검색용 메서드는 그대로 두고...
    // 사용자 ID(=MemberEntity.id 컬럼)로도 조회할 수 있도록 추가
    Optional<MemberEntity> findById(String userId);
}
