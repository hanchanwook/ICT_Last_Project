package com.jakdang.labs.api.cottonCandy.course.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.api.common.ResponseGetEducationDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.RequestSubDetailDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseSubDetailDTO;
import com.jakdang.labs.api.cottonCandy.course.service.SubDetailService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.entity.SubjectDetailEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subDetail")
public class SubDetailController {
    private final SubDetailService subDetailService;
    private final EducationId education;


    // 세부과목 리스트 조회 
    @GetMapping("/list")
    public ResponseDTO<List<ResponseSubDetailDTO>> getSubjectDetailList(@RequestParam(required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseDTO.createErrorResponse(400, "userId 파라미터가 필요합니다.");
        }
        Optional<ResponseGetEducationDTO> educationId = education.findById(userId); 
        List<ResponseSubDetailDTO> subjectDetailList = subDetailService.getSubjectDetailList(educationId.get().getEducationId());
        return ResponseDTO.createSuccessResponse("성공", subjectDetailList);
    }

    // 세부과목 등록
    @PostMapping("/create")
    public ResponseDTO<ResponseSubDetailDTO> createSubDetail(@RequestBody RequestSubDetailDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isEmpty()) {
            return ResponseDTO.createErrorResponse(400, "userId 파라미터가 필요합니다.");
        }
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
            // createSubDetail 메서드가 SubjectDetailEntity 타입을 요구하므로 변환 필요
            SubjectDetailEntity subDetail = new SubjectDetailEntity();
            subDetail.setEducationId(educationId.get().getEducationId());
            subDetail.setSubDetailName(dto.getSubDetailName());
            subDetail.setSubDetailInfo(dto.getSubDetailInfo());
            // 필요하다면 dto에서 다른 필드도 세팅
            ResponseSubDetailDTO result = subDetailService.createSubDetail(subDetail);
            return ResponseDTO.createSuccessResponse("성공", result);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "세부과목 등록 실패: " + e.getMessage());
        }
    }

    // 세부과목 수정
    @PostMapping("/update/{id}")
    public ResponseDTO<ResponseSubDetailDTO> updateSubDetail(
            @PathVariable String id,
            @RequestBody RequestSubDetailDTO dto) {
        try {
            SubjectDetailEntity subDetail = new SubjectDetailEntity();
            subDetail.setSubDetailId(id); // URL에서 받은 id 사용
            subDetail.setSubDetailName(dto.getSubDetailName());
            subDetail.setSubDetailInfo(dto.getSubDetailInfo());
            // subDetailActive는 Service에서 기존 값 유지
            ResponseSubDetailDTO result = subDetailService.updateSubDetail(subDetail);
            return ResponseDTO.createSuccessResponse("성공", result);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "세부과목 수정 실패: " + e.getMessage());
        }
    }
    
    // 세부과목 삭제
    @PostMapping("/delete/{id}")
    public ResponseDTO<ResponseSubDetailDTO> deleteSubDetail(@PathVariable String id) {
        try {
            subDetailService.deleteSubDetail(id);
            return ResponseDTO.createSuccessResponse("성공", null);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "세부과목 삭제 실패: " + e.getMessage());
        }
    }

}