package com.jakdang.labs.api.cottonCandy.evaluation.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestEvaluationAnswerDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseStudentCourseDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseStudentEvaluationDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseStudentEvaluationDetailDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.service.StudentEvaluationService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import com.jakdang.labs.entity.MemberEntity;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/evaluation/student")
public class StudentEvaluationController {

    private final StudentEvaluationService studentEvaluationService;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    
    // 내 강의 목록조회
    @GetMapping("/course/list")
    public ResponseDTO<List<ResponseStudentCourseDTO>> getMyCourseList(HttpServletRequest request) {
        try {
            // JWT 토큰에서 이메일 추출
            String userEmail = extractEmailFromToken(request);
            if (userEmail == null) {
                return ResponseDTO.createErrorResponse(401, "JWT 토큰에서 이메일을 추출할 수 없습니다.");
            }
            
            // 이메일과 학생 역할로 멤버 정보 조회 (ROLE_STUDENT인 경우만)
            List<MemberEntity> memberList = memberRepository.findByMemberEmailAndStudentRole(userEmail);
            if (memberList.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "이메일에 해당하는 학생 멤버 정보를 찾을 수 없습니다.");
            }
            
            // 첫 번째 멤버에서 userId와 educationId 추출 (모든 레코드가 동일한 학생이므로)
            MemberEntity firstMember = memberList.get(0);
            String userId = firstMember.getId();
            String educationId = firstMember.getEducationId();
            
            if (educationId == null) {
                return ResponseDTO.createErrorResponse(400, "멤버의 교육기관 정보가 없습니다.");
            }
            
            // 모든 courseId 수집
            List<String> courseIds = memberList.stream()
                .map(MemberEntity::getCourseId)
                .filter(courseId -> courseId != null && !courseId.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
            
            // courseId 목록으로 강의 정보 조회
            List<ResponseStudentCourseDTO> response = studentEvaluationService.getMyCourseListByCourseIds(courseIds, educationId, userId);
            return ResponseDTO.createSuccessResponse("내 강의 목록 조회 성공", response);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "내 강의 목록 조회 실패: " + e.getMessage());
        }
    }

    // 해당 템플릿 평가 항목 조회
    @GetMapping("/evaluation/list")
    public ResponseDTO<ResponseStudentEvaluationDTO> getEvaluationList(@RequestParam String templateGroupId) {
        try {
            ResponseStudentEvaluationDTO response = studentEvaluationService.findByTemplateGroupId(templateGroupId);
            return ResponseDTO.createSuccessResponse("해당 템플릿 평가 항목 조회 성공", response);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "해당 템플릿 평가 항목 조회 실패: " + e.getMessage());
        }
    }

    // 학생의 평가 답변 조회 
    @GetMapping("/evaluation/response")
    public ResponseDTO<ResponseStudentEvaluationDetailDTO> getStudentEvaluationResponses(
            @RequestParam String templateGroupId, HttpServletRequest request) {
        try {
            // JWT 토큰에서 이메일 추출
            String userEmail = extractEmailFromToken(request);
            
            if (userEmail == null) {
                return ResponseDTO.createErrorResponse(401, "JWT 토큰에서 이메일을 추출할 수 없습니다.");
            }
            
            // 이메일과 학생 역할로 멤버 정보 조회 (ROLE_STUDENT인 경우만)
            List<MemberEntity> memberList = memberRepository.findByMemberEmailAndStudentRole(userEmail);
            
            if (memberList.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "이메일에 해당하는 학생 멤버 정보를 찾을 수 없습니다.");
            }
            
            // 첫 번째 멤버에서 userId 추출 (동일한 이메일이므로 첫 번째 사용)
            MemberEntity firstMember = memberList.get(0);
            String userId = firstMember.getId();
            
            ResponseStudentEvaluationDetailDTO response = 
            studentEvaluationService.getStudentEvaluationDetailByTemplateGroupId(templateGroupId, userId);
            
            return ResponseDTO.createSuccessResponse("학생 설문 응답 조회 성공", response);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "학생 설문 응답 조회 실패: " + e.getMessage());
        }
    }

    // 해당 강의 평가 답변 등록 
    @PostMapping("/evaluation/answer")
    public ResponseDTO<ResponseStudentEvaluationDTO> createEvaluationAnswer(@RequestBody RequestEvaluationAnswerDTO dto, HttpServletRequest request) {
        try {
            // JWT 토큰에서 이메일 추출
            String userEmail = extractEmailFromToken(request);
            if (userEmail == null) {
                return ResponseDTO.createErrorResponse(401, "JWT 토큰에서 이메일을 추출할 수 없습니다.");
            }
            
            // 이메일과 학생 역할로 멤버 정보 조회 (ROLE_STUDENT인 경우만)
            List<MemberEntity> memberList = memberRepository.findByMemberEmailAndStudentRole(userEmail);
            if (memberList.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "이메일에 해당하는 학생 멤버 정보를 찾을 수 없습니다.");
            }
            
            // 첫 번째 멤버에서 userId 추출 (동일한 이메일이므로 첫 번째 사용)
            MemberEntity firstMember = memberList.get(0);
            String userId = firstMember.getId();
            
            ResponseStudentEvaluationDTO response = studentEvaluationService.createEvaluationAnswer(dto, userId);
            return ResponseDTO.createSuccessResponse("해당 강의 평가 답변 등록 성공", response);
        } catch (Exception e) {
            return ResponseDTO.createErrorResponse(500, "평가 답변 등록 실패: " + e.getMessage());
        }
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    private String extractEmailFromToken(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    return jwtUtil.getUserEmail(token);
                }
            }
            
            // 쿠키에서 토큰 추출
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                            return jwtUtil.getUserEmail(token);
                        }
                    }
                }
            }
            
            throw new RuntimeException("JWT token not found in Authorization header or Cookie");
        } catch (Exception e) {
            throw new RuntimeException("JWT token not found in Authorization header or Cookie");
        }
    }

}
