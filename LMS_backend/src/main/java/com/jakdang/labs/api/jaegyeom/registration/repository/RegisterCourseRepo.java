package com.jakdang.labs.api.jaegyeom.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jakdang.labs.entity.MemberEntity;
import java.util.List;

public interface RegisterCourseRepo extends JpaRepository<MemberEntity, String> {
    List<MemberEntity> findByMemberEmail(String memberEmail); // 이메일로 회원 조회
}
