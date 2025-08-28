package com.jakdang.labs.api.lnuyasha.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.common.EducationId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    
    private final KyMemberRepository memberRepository;
    private final EducationId educationId;


    // id 가지고 memberId와 educationId 조회
    public MemberInfoDTO getMemberInfo(String id) {
        log.info("getMemberInfo 호출: id = {}", id);
        
        // 1. 먼저 member 테이블의 id 필드로 조회 (users 테이블의 id와 매칭)
        log.info("memberRepository.findMemberInfoById() 호출 시작: id = {}", id);
        List<MemberInfoDTO> members = memberRepository.findMemberInfoById(id);
        log.info("findMemberInfoById 결과: {}개", members.size());
        
        if (!members.isEmpty()) {
            MemberInfoDTO result = members.get(0);
            log.info("member 테이블의 id 필드로 조회 성공: id = {}, memberInfo = {}", id, result);
            log.info("반환되는 memberId: {}", result.getMemberId());
            return result;
        }
        
        // 2. memberId로도 조회 시도
        log.info("member 테이블의 id 필드로 조회 실패, memberId로 조회 시도: {}", id);
        List<MemberEntity> memberEntities = memberRepository.findByMemberId(id);
        log.info("findByMemberId 결과: {}개", memberEntities.size());
        
        if (!memberEntities.isEmpty()) {
            MemberEntity member = memberEntities.get(0);
            MemberInfoDTO result = MemberInfoDTO.builder()
                .memberId(member.getMemberId())
                .educationId(member.getEducationId())
                .memberName(member.getMemberName())
                .memberRole(member.getMemberRole())
                .memberEmail(member.getMemberEmail())
                .id(member.getId())
                .build();
            log.info("memberId로 조회 성공: id = {}, memberInfo = {}", id, result);
            log.info("반환되는 memberId: {}", result.getMemberId());
            return result;
        }
        
        log.warn("getMemberInfo 실패: id = {}에 해당하는 member 정보를 찾을 수 없습니다.", id);
        return null;
    }

    // email로 memberId와 educationId 조회
    public MemberInfoDTO getMemberInfoByEmail(String email) {
        List<MemberInfoDTO> members = memberRepository.findMemberInfoByEmail(email);
        if (!members.isEmpty()) {
            // 첫 번째 결과를 반환 (또는 필요한 로직에 따라 처리)
            return members.get(0);
        }
        return null;
    }

    // 강사 리스트 불러오기
    public List<MemberInfoDTO> getInstructorListSimple(String educationId) {
        List<MemberEntity> instructors = memberRepository.findTeacherByEducationId(educationId);
        List<MemberInfoDTO> instructorList = new ArrayList<>();
        
        for (MemberEntity instructor : instructors) {
            MemberInfoDTO memberInfo = MemberInfoDTO.builder()
                    .memberId(instructor.getMemberId())
                    .educationId(instructor.getEducationId())
                    .memberName(instructor.getMemberName())
                    .memberRole(instructor.getMemberRole())
                    .memberEmail(instructor.getMemberEmail())
                    .id(instructor.getId())
                    .build();
            instructorList.add(memberInfo);
        }
        
        return instructorList;
    }

    // 문제 ID로 강사 정보 조회 (memberId는 실제로 member 테이블의 id 컬럼과 매칭)
    public MemberInfoDTO getInstructorByQuestionId(String memberId) {
        // memberId가 null이거나 빈 문자열인 경우 null 반환
        if (memberId == null || memberId.trim().isEmpty()) {
            log.warn("getInstructorByQuestionId: memberId가 null이거나 빈 문자열입니다.");
            return null;
        }
        
        try {
            List<MemberEntity> members = memberRepository.findByIdColumn(memberId);
            if (!members.isEmpty()) {
                MemberEntity member = members.get(0);
                MemberInfoDTO result = MemberInfoDTO.builder()
                        .memberId(member.getMemberId())
                        .educationId(member.getEducationId())
                        .memberName(member.getMemberName())
                        .memberRole(member.getMemberRole())
                        .memberEmail(member.getMemberEmail())
                        .id(member.getId())
                        .build();
                log.info("getInstructorByQuestionId 성공: memberId = {}, memberInfo = {}", memberId, result);
                return result;
            } else {
                log.warn("getInstructorByQuestionId: memberId = {}에 해당하는 멤버를 찾을 수 없습니다.", memberId);
                return null;
            }
        } catch (Exception e) {
            log.error("getInstructorByQuestionId 중 오류 발생: memberId = {}, error = {}", memberId, e.getMessage(), e);
            throw new RuntimeException("memberId가 선택되지 않았습니다: " + memberId, e);
        }
    }

    // memberId로 강사 정보 조회 (member 테이블의 memberId 컬럼과 매칭)
    public MemberInfoDTO getMemberInfoByMemberId(String memberId) {
        List<MemberEntity> members = memberRepository.findByMemberId(memberId);
        if (!members.isEmpty()) {
            MemberEntity member = members.get(0);
            return MemberInfoDTO.builder()
                    .memberId(member.getMemberId())
                    .educationId(member.getEducationId())
                    .memberName(member.getMemberName())
                    .memberRole(member.getMemberRole())
                    .memberEmail(member.getMemberEmail())
                    .id(member.getId())
                    .build();
        }
        return null;
    }

    // userId로 회원 정보 조회 (users 테이블의 id = member 테이블의 id)
    public MemberInfoDTO getMemberInfoByUserId(String userId) {
        List<MemberEntity> members = memberRepository.findByIdColumn(userId);
        if (!members.isEmpty()) {
            MemberEntity member = members.get(0);
            
            // EducationId 컴포넌트를 사용하여 educationId 조회
            String resolvedEducationId = educationId.getEducationIdByUserIdOrNull(userId);
            
            return MemberInfoDTO.builder()
                    .memberId(member.getMemberId())
                    .educationId(resolvedEducationId != null ? resolvedEducationId : member.getEducationId())
                    .memberName(member.getMemberName())
                    .memberRole(member.getMemberRole())
                    .memberEmail(member.getMemberEmail())
                    .id(member.getId())
                    .build();
        }
        return null;
    }


} 