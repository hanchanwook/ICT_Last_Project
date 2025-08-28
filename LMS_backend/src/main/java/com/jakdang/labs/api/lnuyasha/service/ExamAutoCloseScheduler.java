package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.ExamDTO;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.SubGroupEntity;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.SubGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시험 자동 종료 스케줄러
 * 종료 시간이 된 시험을 자동으로 종료하고 미제출자에 대해 0점 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamAutoCloseScheduler {

    private final TemplateRepository templateRepository;
    private final KyMemberRepository memberRepository;
    private final TemplateService templateService;
    private final SubGroupRepository subGroupRepository;

    /**
     * 30초마다 실행되는 시험 자동 종료 스케줄러
     * 종료 시간이 된 활성화된 시험을 찾아서 자동 종료 처리
     */
    @Scheduled(fixedRate = 30000) // 30초마다 실행
    @Transactional
    public void autoCloseExams() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 1. 종료 시간이 된 활성화된 시험 템플릿 조회
            List<TemplateEntity> expiredTemplates = templateRepository.findByTemplateActiveAndTemplateCloseBefore(1, now);
            
            if (expiredTemplates.isEmpty()) {
                return;
            }
            
            // 2. 각 시험에 대해 자동 종료 처리
            for (TemplateEntity template : expiredTemplates) {
                try {
                    // 3. 해당 시험의 학생 목록 조회 (courseId로 조회)
                    String courseId = null;
                    if (template.getSubGroupId() != null && !template.getSubGroupId().trim().isEmpty()) {
                        try {
                            SubGroupEntity subGroup = subGroupRepository.findBySubGroupId(template.getSubGroupId());
                            if (subGroup != null) {
                                courseId = subGroup.getCourseId();
                            } else {
                                continue;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                    
                    List<String> studentMemberIds = memberRepository
                            .findByCourseIdAndMemberRole(courseId, "ROLE_STUDENT")
                            .stream()
                            .filter(member -> member.getMemberExpired() == null) // 만료되지 않은 학생만
                            .map(MemberEntity::getMemberId)
                            .collect(Collectors.toList());
                    
                    if (studentMemberIds.isEmpty()) {
                        continue;
                    }
                    
                    // 4. 시험 종료 및 미제출 학생 자동 0점 처리
                    ExamDTO result = templateService.closeWithAutoSubmission(
                            template.getTemplateId(),
                            template.getTemplateClose().toString(),
                            studentMemberIds,
                            template.getMemberId() // 시험 소유자 ID
                    );
                    
                    
                } catch (Exception e) {
                }
            }
            
            
        } catch (Exception e) {
        }
    }
} 