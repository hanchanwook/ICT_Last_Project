package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.service.CourseStudentService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/instructor/subgroups")
@RequiredArgsConstructor
public class CourseStudentController {
    
    private final CourseStudentService courseStudentService;
    
    /**
     * 서브그룹별 학생 목록 조회
     * GET /api/instructor/subgroups/{subGroupId}/students
     */
    @GetMapping("/{subGroupId}/students")
    public ResponseEntity<ResponseDTO<List<MemberInfoDTO>>> getSubGroupStudents(
            @PathVariable String subGroupId,
            @RequestParam(value = "userId", required = false) String userId) {
        
        try {
            List<MemberInfoDTO> students = courseStudentService.getCourseStudents(subGroupId, userId);
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("서브그룹별 학생 목록 조회 성공", students));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "서브그룹별 학생 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 