package com.jakdang.labs.api.jaegyeom.registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jakdang.labs.entity.MemberEntity;
import java.util.List;

@Repository
public interface PeopleRegisterRepository extends JpaRepository<MemberEntity, String> {

    List<MemberEntity> findByMemberEmailAndMemberName(String memberEmail, String memberName);

    boolean existsById(String id);

    List<MemberEntity> findByEducationId(String educationId); // 교육기관 ID로 회원 조회
    List<MemberEntity> findByEducationIdAndMemberRole(String educationId, String memberRole); // 교육기관 ID와 역할로 회원 조회
}
