package com.jakdang.labs.api.cottonCandy.evaluation.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestEvaluationQuestionDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseEvaluationQuestionDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.service.EvaluationQuestionService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;

import org.springframework.web.bind.annotation.RequestBody;

import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.api.common.ResponseGetEducationDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/evaluation")
public class EvaluationQuestionController {
    private final EvaluationQuestionService evaluationQuestionService;
    private final EducationId education;
     
    // 직원 - 평가 질문 리스트 조회 =
    @GetMapping("/list")
    public ResponseDTO<List<ResponseEvaluationQuestionDTO>> getEvaluationQuestionList(@RequestParam(value = "userId") String userId) {
        Optional<ResponseGetEducationDTO> educationId = education.findById(userId);

        List<ResponseEvaluationQuestionDTO> evaluationQuestionList = evaluationQuestionService.getEvaluationList(educationId.get().getEducationId());
        return ResponseDTO.createSuccessResponse("평가 질문 리스트 조회 성공", evaluationQuestionList);
    } 

    // 직원 - 평가 질문 등록 =
    @PostMapping("/create")
    public ResponseDTO<ResponseEvaluationQuestionDTO> createEvaluationQuestion(@RequestBody RequestEvaluationQuestionDTO dto) {
        Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
        ResponseEvaluationQuestionDTO createdEvaluationQuestion = evaluationQuestionService.createEvaluationQuestion(dto, educationId.get().getEducationId());
        return ResponseDTO.createSuccessResponse("평가 질문 등록 성공", createdEvaluationQuestion);
    } 

   // 직원 - 평가 잘문 삭제 =
   @PostMapping("/delete/{id}")
   public ResponseDTO<Void> deleteEvaluationQuestion(@PathVariable String id) {
    try {
        evaluationQuestionService.deleteEvaluationQuestion(id);
        return ResponseDTO.createSuccessResponse("평가 질문 삭제 성공", null);
    } catch (Exception e) {
        return ResponseDTO.createErrorResponse(500, "평가질문 삭제 실패: " + e.getMessage());
        
    }
   }


}