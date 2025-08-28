package com.jakdang.labs.api.cottonCandy.evaluation.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.api.common.ResponseGetEducationDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestQuestiontemplateDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseQuestiontemplateDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.service.Questiontemplateservice;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/questiontemplate")
public class QuestiontemplateController {
    private final Questiontemplateservice questiontemplateservice;
    private final EducationId education;
    
    
    // 과정 평가 템플릿 생성 =
    @PostMapping("/create")
    public ResponseDTO<ResponseQuestiontemplateDTO> createQuestiontemplate(@RequestBody RequestQuestiontemplateDTO dto) {
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "사용자 ID에 해당하는 교육기관 정보를 찾을 수 없습니다.");
            }
            
            ResponseQuestiontemplateDTO createdQuestiontemplate = questiontemplateservice.createQuestiontemplate(dto, educationId.get().getEducationId());
            return ResponseDTO.createSuccessResponse("평가 템플릿 생성 성공", createdQuestiontemplate);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "평가 템플릿 생성 실패: " + e.getMessage());
        }
    }

    // 템플릿 리스트 =
    @GetMapping("/list")
    public ResponseDTO<List<ResponseQuestiontemplateDTO>> getQuestiontemplateList(@RequestParam String userId) {
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(userId);
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "사용자 ID에 해당하는 교육기관 정보를 찾을 수 없습니다.");
            }
            
            List<ResponseQuestiontemplateDTO> templateList = questiontemplateservice.getQuestiontemplateList(educationId.get().getEducationId());
            
            return ResponseDTO.createSuccessResponse("평가 템플릿 리스트 조회 성공", templateList);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "평가 템플릿 리스트 조회 실패: " + e.getMessage());
        }
    }

    // 템플릿 수정  =
    @PostMapping("/update")
    public ResponseDTO<ResponseQuestiontemplateDTO> updateQuestiontemplate(@RequestBody RequestQuestiontemplateDTO dto) {
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "사용자 ID에 해당하는 교육기관 정보를 찾을 수 없습니다.");
            }

            ResponseQuestiontemplateDTO updatedTemplate = questiontemplateservice.updateQuestiontemplate(
                dto,
                educationId.get().getEducationId()
            );
            return ResponseDTO.createSuccessResponse("평가 템플릿 수정 성공", updatedTemplate);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "평가 템플릿 수정 실패: " + e.getMessage());
        }
    }

    // 템플릿 삭제 =
    @PostMapping("/delete")
    public ResponseDTO<String> deleteQuestiontemplate(@RequestBody RequestQuestiontemplateDTO dto) {
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "사용자 ID에 해당하는 교육기관 정보를 찾을 수 없습니다.");
            }

            questiontemplateservice.deleteQuestiontemplate(educationId.get().getEducationId(), dto.getQuestionTemplateNum());
            return ResponseDTO.createSuccessResponse("평가 템플릿 삭제 성공", "템플릿이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "평가 템플릿 삭제 실패: " + e.getMessage());
        }
    }
}
