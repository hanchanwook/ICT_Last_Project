package com.jakdang.labs.api.yongho.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.yongho.repository.MembersRepository;


@Service
public class MemberServiceImpl implements MemberService {

    private final MembersRepository memberRepository;

    public MemberServiceImpl(MembersRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Optional<MemberEntity> findByUserId(String userId) {
        return memberRepository.findById(userId);
    }

    @Override
    public List<MemberEntity> findByMemberRoleIn(List<String> memberRoles) {
        return memberRepository.findByMemberRoleIn(memberRoles);
    }

    @Override
    public List<MemberEntity> findByMemberRoleInAndEducationId(List<String> memberRoles, String educationId) {
        List<MemberEntity> result = memberRepository.findByMemberRoleInAndEducationId(memberRoles, educationId);
        
        for (MemberEntity member : result) {
            System.out.println("- " + member.getMemberName() + " (역할: " + member.getMemberRole() + ", educationId: " + member.getEducationId() + ")");
        }
        return result;
    }
}
