package com.jakdang.labs.api.lnuyasha.service;

import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.api.lnuyasha.dto.SubjectInfoDTO;
import com.jakdang.labs.api.lnuyasha.repository.KySubjectDetailRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubjectRepository;
import com.jakdang.labs.entity.SubjectEntity;
import com.jakdang.labs.entity.SubjectDetailEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KySubjectService {
    
    private final KySubjectDetailRepository subjectDetailRepository;
    private final SubjectRepository subjectRepository;
    
    /**
     * educationId로 과목 목록 조회
     * @param educationId 학원 ID
     * @return 과목 정보 목록
     */
    public List<SubjectInfoDTO> getSubjectsByEducationId(String educationId) {
        log.info("educationId로 과목 목록 조회 요청: educationId = {}", educationId);
        
        try {
            // educationId로 과목 목록 조회
            List<Object[]> results = subjectRepository.findSubjectsWithSubDetails(educationId);
            List<SubjectInfoDTO> subjects = new ArrayList<>();
            
            // 결과를 SubjectInfoDTO로 변환
            for (Object[] result : results) {
                String subjectId = (String) result[0];
                String subjectName = (String) result[1];
                String subjectInfo = (String) result[2];
                String subDetailId = (String) result[4];
                String subDetailName = (String) result[5];
                
                // 과목 정보가 있는 경우만 추가
                if (subjectId != null && subjectName != null) {
                    SubjectInfoDTO subjectInfoDTO = SubjectInfoDTO.builder()
                        .subjectId(subjectId)
                        .subjectName(subjectName)
                        .subjectInfo(subjectInfo != null ? subjectInfo : "")
                        .subDetailId(subDetailId)
                        .subDetailName(subDetailName != null ? subDetailName : "")
                        .questionCount(0) // TODO: 실제 문제 수 계산 필요시 구현
                        .educationId(educationId)
                        .educationName("") // TODO: 학원명 조회 필요시 구현
                        .build();
                    
                    subjects.add(subjectInfoDTO);
                }
            }
            
            log.info("과목 목록 조회 완료: {}개 항목", subjects.size());
            return subjects;
            
        } catch (Exception e) {
            log.error("과목 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("과목 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}
