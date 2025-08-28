package com.jakdang.labs.api.cottonCandy.course.controller;

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
import com.jakdang.labs.api.cottonCandy.course.dto.RequestSubjectDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseSubjectDTO;
import com.jakdang.labs.api.cottonCandy.course.service.SubjectService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subject")
public class SubjectController {
    private final SubjectService subjectService;
    private final EducationId education;

    // 과목 리스트 조회
    @GetMapping("/list")
    public ResponseDTO<List<ResponseSubjectDTO>> getSubjectList(@RequestParam(required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseDTO.createErrorResponse(400, "userId 파라미터가 필요합니다.");
        }
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(userId);
            List<ResponseSubjectDTO> subjectList = subjectService.getSubjectList(educationId.get().getEducationId());
            return ResponseDTO.createSuccessResponse("과목 리스트 조회 성공", subjectList);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "과목 리스트 조회 실패: " + e.getMessage());
        }
    }

    // 과목 등록
    @PostMapping("/create")
    public ResponseDTO<ResponseSubjectDTO> createSubject(@RequestBody RequestSubjectDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isEmpty()) {
            return ResponseDTO.createErrorResponse(400, "userId 파라미터가 필요합니다.");
        }
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
            ResponseSubjectDTO createdSubject = subjectService.createSubject(dto, educationId.get().getEducationId());
            return ResponseDTO.createSuccessResponse("과목 등록 성공", createdSubject);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "과목 등록 실패: " + e.getMessage());
        }
    }

    // 과목 수정
    @Transactional
    @PostMapping("/update/{id}")
    public ResponseDTO<ResponseSubjectDTO> updateSubject(
            @PathVariable String id,
            @RequestBody RequestSubjectDTO dto) {
        try {
            if (dto.getUserId() == null || dto.getUserId().isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "userId 파라미터가 필요합니다.");
            }
            Boolean changeActive = subjectService.updateSubject(id);
            if (changeActive) {
                Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
                ResponseSubjectDTO createdSubject = subjectService.createSubject(dto, educationId.get().getEducationId());
                return ResponseDTO.createSuccessResponse("과목 수정 성공", createdSubject);
            } else {
                return ResponseDTO.createSuccessResponse("과목 수정 실패. Active 변경 불가", null);
            }
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "과목 수정 실패: " + e.getMessage());
        }
    }

    // 과목 삭제
    @PostMapping("/delete/{id}")
    public ResponseDTO<ResponseSubjectDTO> deleteSubject(@PathVariable String id) {
        try {
            Boolean deleteSubject = subjectService.updateSubject(id);
            if (deleteSubject) {
                return ResponseDTO.createSuccessResponse("과목 삭제 성공", null);
            } else {
                return ResponseDTO.createSuccessResponse("과목 삭제 실패. Active 변경 불가", null);
            }
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "과목 삭제 실패: " + e.getMessage());
        }
    }
}

