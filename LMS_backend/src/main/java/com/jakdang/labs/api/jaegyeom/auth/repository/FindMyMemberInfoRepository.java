package com.jakdang.labs.api.jaegyeom.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.entity.MemberEntity;

public interface FindMyMemberInfoRepository extends JpaRepository<MemberEntity, String> {
    MemberEntity findByMemberEmail(String email);
}
