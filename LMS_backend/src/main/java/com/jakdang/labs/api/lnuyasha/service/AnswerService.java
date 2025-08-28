package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.repository.AnswerRepository;
import com.jakdang.labs.entity.AnswerEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.jakdang.labs.api.lnuyasha.util.TimeZoneUtil;

/**
 * 학생 답안 정보 조회를 위한 Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnswerService {
    
    private final AnswerRepository AnswerRepository;
    private final MemberService memberService;
    
    /**
     * 사용자 이메일로 답안 목록 조회 (해당 학원의 답안만)
     * @param email 사용자 이메일
     * @return 해당 학원의 답안 목록
     */
    public List<AnswerDTO> getAnswersByUserEmail(String email) {
        
        // 1. 사용자 이메일로 member 정보 조회
        MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
        if (memberInfo == null) {
            log.warn("해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: {}", email);
            throw new IllegalArgumentException("해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email);
        }
        
        // 2. 해당 회원의 답안 목록 조회 (활성화된 답안만)
        List<AnswerEntity> answers = AnswerRepository.findByMemberIdAndAnswerActive(memberInfo.getMemberId(), 0);
        
        // 3. AnswerDTO로 변환
        List<AnswerDTO> answerDTOs = answers.stream()
            .map(answer -> AnswerDTO.builder()
                .answerId(answer.getAnswerId())
                .answerContent(answer.getAnswerText())
                .answerScore(answer.getAnswerScore())
                .memberId(answer.getMemberId())
                .answerDate(TimeZoneUtil.toKoreanTime(answer.getCreatedAt()))
                .build())
            .collect(Collectors.toList());
        
        return answerDTOs;
    }
} 