package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.service.MemberService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 정보 조회 컨트롤러
 * id를 가지고 memberId와 educationId를 조회하는 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @GetMapping("/instructors/simple")
    public ResponseEntity<ResponseDTO<List<MemberInfoDTO>>> getInstructorListSimple(
            HttpServletRequest request) {
        try {
            // JWT 토큰에서 사용자 이메일 추출
            String email = extractEmailFromToken(request);
            MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
            
            if (memberInfo == null || memberInfo.getEducationId() == null) {
                return ResponseEntity.ok(ResponseDTO.createSuccessResponse("사용자 정보를 찾을 수 없습니다.", List.of()));
            }
            
            String educationId = memberInfo.getEducationId();
            
            List<MemberInfoDTO> instructorList = memberService.getInstructorListSimple(educationId);
            if (instructorList == null) {
                return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강사 정보가 없습니다.", List.of()));
            }
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강사 목록 조회 성공", instructorList));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "강사 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * userId로 memberId와 educationId 조회
     */
    @GetMapping("/resolve-id/{userId}")
    public ResponseEntity<Map<String, Object>> resolveMemberId(
            @PathVariable String userId,
            @RequestParam(required = false) String userIdParam) {
        try {
            // userId 파라미터 검증
            if (userId == null || userId.trim().isEmpty() || "undefined".equals(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않은 userId입니다.");
                errorResponse.put("userId", userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // MemberService를 통해 memberId와 educationId 조회
            MemberInfoDTO memberInfo = memberService.getMemberInfoByUserId(userId);
            
            if (memberInfo == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "사용자 정보를 찾을 수 없습니다.");
                errorResponse.put("userId", userId);
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "사용자 정보 조회 성공");
            response.put("userId", userId);
            response.put("memberId", memberInfo.getMemberId());
            response.put("educationId", memberInfo.getEducationId());
            response.put("memberName", memberInfo.getMemberName());
            response.put("memberRole", memberInfo.getMemberRole());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("userId", userId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * JWT 토큰에서 이메일 추출
     */
    private String extractEmailFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserEmail(token);
        }
        throw new IllegalArgumentException("유효한 토큰이 없습니다.");
    }
    
} 