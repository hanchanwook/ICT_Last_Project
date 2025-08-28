package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.ExamGradingSubmitRequestDTO;
import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.service.ExamGradingService;
import com.jakdang.labs.api.lnuyasha.service.MemberService;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 시험 채점 컨트롤러 (강사용)
 */
@Slf4j
@RestController
@RequestMapping("/api/instructor/exam/grading")
@RequiredArgsConstructor
public class ExamGradingController {
    
    private final ExamGradingService examGradingService;
    private final MemberService memberService;
    private final KyMemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    
    /**
     * 채점 완료 제출
     * POST /api/instructor/exam/grading/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<ResponseDTO<Object>> submitGrading(
            @RequestBody ExamGradingSubmitRequestDTO request,
            @RequestParam(required = false) String userId,
            HttpServletRequest httpRequest) {
        try {
            // userId 파라미터가 있으면 사용, 없으면 JWT 토큰에서 추출
            MemberEntity memberEntity = null;
            if (userId != null && !userId.trim().isEmpty()) {
                // userId로 직접 사용자 정보 조회
                List<MemberEntity> members = memberRepository.findByMemberId(userId);
                if (!members.isEmpty()) {
                    memberEntity = members.get(0);
                }
            } else {
                String email = extractEmailFromToken(httpRequest);
                if (email == null) {
                    return ResponseEntity.status(403)
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .body(ResponseDTO.createErrorResponse(403, "인증 토큰이 유효하지 않습니다."));
                }
                var memberInfo = memberService.getMemberInfoByEmail(email);
                if (memberInfo != null) {
                    List<MemberEntity> members = memberRepository.findByMemberId(memberInfo.getMemberId());
                    if (!members.isEmpty()) {
                        memberEntity = members.get(0);
                    }
                }
            }
            
            if (memberEntity == null) {
                return ResponseEntity.status(403)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .body(ResponseDTO.createErrorResponse(403, "사용자 정보를 찾을 수 없습니다."));
            }
            
            // 강사 권한 확인
            if (!isInstructor(memberEntity.getMemberId())) {
                return ResponseEntity.status(403)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .body(ResponseDTO.createErrorResponse(403, "강사 권한이 필요합니다."));
            }
            
            // 채점 완료 처리
            // 요청 데이터 검증
            validateGradingRequest(request);
            
            AnswerDTO result = examGradingService.submitGrading(request);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(ResponseDTO.createSuccessResponse("채점이 성공적으로 완료되었습니다.", result));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
                    
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(ResponseDTO.createErrorResponse(409, e.getMessage()));
                    
        } catch (RuntimeException e) {
            if (e.getMessage().contains("JWT token not found")) {
                return ResponseEntity.status(403)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .body(ResponseDTO.createErrorResponse(403, "인증 토큰이 유효하지 않습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
            }
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(ResponseDTO.createErrorResponse(500, "서버 내부 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 채점 요청 데이터 검증
     */
    private void validateGradingRequest(ExamGradingSubmitRequestDTO request) {
        
        // 필수 필드 검증
        if (request.getMemberId() == null || request.getMemberId().trim().isEmpty()) {
            throw new IllegalArgumentException("학생 ID는 필수입니다.");
        }
        
        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            throw new IllegalArgumentException("시험 템플릿 ID는 필수입니다.");
        }
        
        if (request.getScore() == null || request.getScore() < 0) {
            throw new IllegalArgumentException("총점은 0 이상의 숫자여야 합니다.");
        }
        
        if (request.getIsChecked() == null || request.getIsChecked() != 1) {
            throw new IllegalArgumentException("채점 완료 여부는 1(완료)이어야 합니다.");
        }
        
        if (request.getQuestionDetails() == null || request.getQuestionDetails().isEmpty()) {
            throw new IllegalArgumentException("문제별 상세 정보는 필수입니다.");
        }
        
        // 문제별 상세 정보 검증
        for (int i = 0; i < request.getQuestionDetails().size(); i++) {
            var detail = request.getQuestionDetails().get(i);
            if (detail.getQuestionId() == null || detail.getQuestionId().trim().isEmpty()) {
                throw new IllegalArgumentException("문제 ID는 필수입니다. (인덱스: " + i + ")");
            }
            if (detail.getScore() == null || detail.getScore() < 0) {
                throw new IllegalArgumentException("문제 점수는 0 이상의 숫자여야 합니다. (인덱스: " + i + ")");
            }
        }
        
    }
    
    /**
     * 강사 권한 확인
     */
    private boolean isInstructor(String memberId) {
        try {
            List<MemberEntity> members = memberRepository.findByMemberId(memberId);
            if (!members.isEmpty()) {
                MemberEntity member = members.get(0);
                String role = member.getMemberRole();
                return "강사".equals(role) || "ROLE_INSTRUCTOR".equals(role) || "INSTRUCTOR".equals(role);
            }
            return false;
        } catch (Exception e) {
            return false;
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