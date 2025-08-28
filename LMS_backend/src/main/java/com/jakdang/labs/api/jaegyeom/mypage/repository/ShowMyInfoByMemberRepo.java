package com.jakdang.labs.api.jaegyeom.mypage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jakdang.labs.entity.MemberEntity;

public interface ShowMyInfoByMemberRepo extends JpaRepository<MemberEntity, String> {
    MemberEntity findByMemberEmail(String email);
}
