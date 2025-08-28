package com.jakdang.labs.api.cottonCandy.course.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.api.cottonCandy.course.dto.RequestSubjectDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseSubjectDTO;
import com.jakdang.labs.api.cottonCandy.course.repository.SubDetailGroupRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubDetailRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubGroupRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubjectRepository;
import com.jakdang.labs.entity.SubjectEntity;
import com.jakdang.labs.entity.SubDetailGroupEntity;
import com.jakdang.labs.entity.SubjectDetailEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final SubDetailGroupRepository subDetailGroupRepository;
    private final SubDetailRepository subDetailRepository;
    private final SubGroupRepository subGroupRepository;

    // 과목 리스트 조회
    public List<ResponseSubjectDTO> getSubjectList(String educationId) {
        List<Object[]> results = subjectRepository.findSubjectsWithSubDetails(educationId);
        
        // 결과를 과목별로 그룹화
        Map<String, ResponseSubjectDTO> subjectMap = new HashMap<>();
        
        for (Object[] result : results) {
            String subjectId = (String) result[0];
            String subjectName = (String) result[1];
            String subjectInfo = (String) result[2];
            Object createdAt = result[3];
            String subDetailId = (String) result[4];
            String subDetailName = (String) result[5];
            String subDetailInfo = (String) result[6];
            
            int useSubject = subGroupRepository.countBySubjectId(subjectId);
            // 과목 정보가 없으면 생성
            if (!subjectMap.containsKey(subjectId)) {
                ResponseSubjectDTO subjectDTO = ResponseSubjectDTO.builder()
                    .subjectId(subjectId)
                    .subjectName(subjectName)
                    .subjectInfo(subjectInfo)
                    .createdAt(createdAt.toString())
                    .subDetails(new ArrayList<>())  
                    .useSubject(useSubject)
                    .build();
                subjectMap.put(subjectId, subjectDTO);
            }
            
            // 상세과목 정보가 있으면 추가
            if (subDetailId != null && subDetailName != null) {
                ResponseSubjectDTO.SubDetailList subDetail = ResponseSubjectDTO.SubDetailList.builder()
                    .subDetailId(subDetailId)
                    .subDetailName(subDetailName)
                    .subDetailInfo(subDetailInfo)
                    .build();
                
                subjectMap.get(subjectId).getSubDetails().add(subDetail);
            }
        }
        
        return new ArrayList<>(subjectMap.values());
    }

    // 과목 등록
    @Transactional
    public ResponseSubjectDTO createSubject(RequestSubjectDTO dto, String educationId) {
        SubjectEntity subject = new SubjectEntity();
        subject.setEducationId(educationId);
        subject.setSubjectName(dto.getSubjectName());
        subject.setSubjectInfo(dto.getSubjectInfo());
        SubjectEntity savedSubject = subjectRepository.save(subject);
        
        // 쉼표로 구분된 subDetailId를 분리하여 각각 저장
        if (dto.getSubDetailId() != null && !dto.getSubDetailId().trim().isEmpty()) {
            String[] subDetailIds = dto.getSubDetailId().split(",");
            
            for (String individualSubDetailId : subDetailIds) {
                String trimmedSubDetailId = individualSubDetailId.trim();
                
                if (!trimmedSubDetailId.isEmpty()) {
                    try {
                        // subDetailId가 실제로 존재하는지 확인
                        boolean exists = subDetailRepository.existsById(trimmedSubDetailId);
                        if (!exists) {
                            continue;
                        }
                        
                        SubDetailGroupEntity subDetailGroup = SubDetailGroupEntity.builder()
                            .subDetailId(trimmedSubDetailId)
                            .subjectId(savedSubject.getSubjectId()) // 새로 생성된 SubjectEntity의 ID 사용
                            .build();
                        
                        subDetailGroupRepository.save(subDetailGroup);
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }
        }
        
        // // 저장 후 subjectId로 다시 조회하여 createdAt 등 값 보장
        // SubjectEntity loadedSubject = subjectRepository.findById(savedSubject.getSubjectId()).orElse(savedSubject);
        
        // 과목과 연결된 상세과목 정보를 조회
        List<Object[]> subDetailInfoList = subDetailGroupRepository.findSubDetailInfoBySubjectId(savedSubject.getSubjectId());
        List<ResponseSubjectDTO.SubDetailList> subDetails = subDetailInfoList.stream()
            .map(info -> ResponseSubjectDTO.SubDetailList.builder()
                .subDetailId((String) info[0])
                .subDetailName((String) info[1])
                .subDetailInfo((String) info[2])
                .build())
            .collect(Collectors.toList());
        
        // ResponseSubjectDTO 생성 및 반환
        ResponseSubjectDTO responseDTO = ResponseSubjectDTO.builder()
            .subjectId(savedSubject.getSubjectId())
            .subjectName(savedSubject.getSubjectName())
            .subjectInfo(savedSubject.getSubjectInfo())
            .createdAt(savedSubject.getCreatedAt() != null ? savedSubject.getCreatedAt().toString() : null)
            .subDetails(subDetails)
            .build();
        
        return responseDTO;
    }

    // 과목 Active 수정
    @Transactional
    public Boolean updateSubject(String id) {
        SubjectEntity subject = new SubjectEntity();
        subject.setSubjectId(id);
        
        // 데이터가 존재하는지 확인
        SubjectEntity existingSubject = subjectRepository.findById(subject.getSubjectId())
        .orElseThrow(() -> {
            return new RuntimeException("세부과목을 찾을 수 없습니다. ID: " + subject.getSubjectId());
        });
        
        // 기존 데이터의 active를 1로 변경 (비활성화)
        existingSubject.setSubjectActive(1);
        subjectRepository.save(existingSubject);
        return true;
    }

}
