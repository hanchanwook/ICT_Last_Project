package com.jakdang.labs.api.cottonCandy.course.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jakdang.labs.api.cottonCandy.course.dto.ResponseSubDetailDTO;
import com.jakdang.labs.api.cottonCandy.course.repository.SubDetailRepository;
import com.jakdang.labs.entity.SubjectDetailEntity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubDetailService {

    private final SubDetailRepository subDetailRepository;

    
    // 세부과목 리스트 조회     
    public List<ResponseSubDetailDTO> getSubjectDetailList(String educationId) {
        List<SubjectDetailEntity> subDetailList = subDetailRepository.findByEducationIdAndSubDetailActive(educationId, 0);  
        List<ResponseSubDetailDTO> responseSubDetailList = subDetailList.stream()
            .map(ResponseSubDetailDTO::fromEntity)
            .collect(Collectors.toList());
        return responseSubDetailList;
    }

    // 세부과목 등록
    public ResponseSubDetailDTO createSubDetail(SubjectDetailEntity subDetail) {
        
        SubjectDetailEntity saved = subDetailRepository.save(subDetail);
        ResponseSubDetailDTO responseSubDetail = ResponseSubDetailDTO.builder()
            .subDetailId(saved.getSubDetailId())
            .subDetailName(saved.getSubDetailName())
            .subDetailInfo(saved.getSubDetailInfo())
            .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null)
            .build();
        return responseSubDetail;
    }
    
    // 세부과목 수정 (기존 데이터는 그대로 두고 새로운 데이터 등록)
    @Transactional
    public ResponseSubDetailDTO updateSubDetail(SubjectDetailEntity subDetail) {
        // 기존 데이터가 존재하는지 확인
        SubjectDetailEntity existingSubDetail = subDetailRepository.findById(subDetail.getSubDetailId())
        .orElseThrow(() -> {
            return new RuntimeException("세부과목을 찾을 수 없습니다. ID: " + subDetail.getSubDetailId());
        });
        
        // 기존 데이터의 active를 1로 변경 (비활성화)
        existingSubDetail.setSubDetailActive(1);
        subDetailRepository.save(existingSubDetail);
        
        // 새로운 데이터 생성 (수정된 정보로)
        SubjectDetailEntity newSubDetail = new SubjectDetailEntity();
        newSubDetail.setEducationId(existingSubDetail.getEducationId()); // 기존 educationId 유지
        newSubDetail.setSubDetailName(subDetail.getSubDetailName()); // 새로운 이름
        newSubDetail.setSubDetailInfo(subDetail.getSubDetailInfo()); // 새로운 정보
        // subDetailActive는 기본값 0으로 설정됨
        
        SubjectDetailEntity savedNewSubDetail = subDetailRepository.save(newSubDetail);
        
        return ResponseSubDetailDTO.fromEntity(savedNewSubDetail);
    }
    // 세부과목 삭제
    public void deleteSubDetail(String id) {
        // 기존 데이터가 존재하는지 확인
        SubjectDetailEntity existingSubDetail = subDetailRepository.findById(id)
        .orElseThrow(() -> {
            return new RuntimeException("세부과목을 찾을 수 없습니다. ID: " + id);
        });
        
        // 기존 데이터의 active를 1로 변경 (비활성화)
        existingSubDetail.setSubDetailActive(1);
        subDetailRepository.save(existingSubDetail);
    }
    
}
