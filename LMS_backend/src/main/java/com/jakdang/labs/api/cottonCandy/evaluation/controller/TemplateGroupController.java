package com.jakdang.labs.api.cottonCandy.evaluation.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.api.common.ResponseGetEducationDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestTemplateGroupDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseTemplateGroupDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.service.TemplateGroupService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templategroup")
public class TemplateGroupController {
    private final TemplateGroupService templateGroupService;
    private final EducationId education;
    
    // 과정 평가 강의 리스트 조회 
    @GetMapping("/list")
    public ResponseDTO<List<ResponseTemplateGroupDTO>> getCourseEvaluationList(@RequestParam String userId) {
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(userId);
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "사용자 ID에 해당하는 교육기관 정보를 찾을 수 없습니다.");
            }
            
            List<ResponseTemplateGroupDTO> courseList = templateGroupService.getCourseEvaluationList(educationId.get().getEducationId());
            
            return ResponseDTO.createSuccessResponse("과정 평가 강의 리스트 조회 성공", courseList);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "과정 평가 강의 리스트 조회 실패: " + e.getMessage());
        }
    }

    // 과정에 템플릿 등록
    @PostMapping("/create")
    public ResponseDTO<ResponseTemplateGroupDTO> createEvaluationQuestion(@RequestBody RequestTemplateGroupDTO dto) {
        try {
            ResponseTemplateGroupDTO createdEvaluationQuestion = templateGroupService.createTemplateGroup(dto);
            return ResponseDTO.createSuccessResponse("템플릿 그룹 등록 성공", createdEvaluationQuestion);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "템플릿 그룹 등록 실패: " + e.getMessage());
        }     
    } 

    // 과정에 설정된 템플릿 그룹 수정(평가 일정 수정)
    @PostMapping("/update")
    public ResponseDTO<ResponseTemplateGroupDTO> updateEvaluationQuestion(@RequestBody RequestTemplateGroupDTO dto) {
        try {
            ResponseTemplateGroupDTO updatedEvaluationQuestion = templateGroupService.updateTemplateGroup(dto);
            return ResponseDTO.createSuccessResponse("템플릿 그룹 수정 성공", updatedEvaluationQuestion);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "템플릿 그룹 수정 실패: " + e.getMessage());
        }
    }

    // 과정에 설정된 템플릿 그룹 삭제(일정 삭제)
    @PostMapping("/delete/{templateGroupId}")
    public ResponseDTO<String> deleteEvaluationQuestion(@PathVariable String templateGroupId) {
        try {
            // templateGroupId 검증
            if (templateGroupId == null || templateGroupId.isEmpty() || "undefined".equals(templateGroupId)) {
                return ResponseDTO.createErrorResponse(400, "templateGroupId는 필수이며 유효한 값이어야 합니다.");
            }            
            templateGroupService.deleteTemplateGroup(templateGroupId);
            return ResponseDTO.createSuccessResponse("템플릿 그룹 삭제 성공", null);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "템플릿 그룹 삭제 실패: " + e.getMessage());
        }
    }
            
}
