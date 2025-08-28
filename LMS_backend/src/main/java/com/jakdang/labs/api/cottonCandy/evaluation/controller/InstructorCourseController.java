package com.jakdang.labs.api.cottonCandy.evaluation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseTemplateGroupDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.service.InstructorCourseService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instructor/evaluations")
public class InstructorCourseController {
    
    private final InstructorCourseService instructorCourseService;
    
    // 선생님이 자신의 모든 강의 조회
    @GetMapping("/list")
    public ResponseDTO<List<ResponseTemplateGroupDTO>> getInstructorCourses(@RequestParam String userId) {
        try {
            List<ResponseTemplateGroupDTO> courseList = instructorCourseService.getInstructorCourses(userId);
            
            return ResponseDTO.createSuccessResponse("선생님 강의 조회 성공", courseList);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "선생님 강의 조회 실패: " + e.getMessage());
        }
    }
} 