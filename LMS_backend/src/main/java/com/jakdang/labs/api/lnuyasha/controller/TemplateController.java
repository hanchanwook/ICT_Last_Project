package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.service.TemplateService;
import com.jakdang.labs.api.lnuyasha.service.MemberService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import com.jakdang.labs.api.lnuyasha.dto.ExamDTO;
import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.dto.ExamCreateCompleteRequestDTO;

import com.jakdang.labs.api.lnuyasha.dto.TemplateCreateRequestDTO;
import com.jakdang.labs.api.lnuyasha.dto.TemplateResponseDTO;
import com.jakdang.labs.api.lnuyasha.dto.TemplateUpdateRequestDTO;

@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {
    
    private final TemplateService templateService;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    
    /**
     * 1. 시험 템플릿 목록 조회
     * GET /api/templates
     */
    // @GetMapping
    // public ResponseEntity<ResponseDTO<List<TemplateResponseDTO>>> getAllTemplates(HttpServletRequest request) {
    //     try {
    //         log.info("=== 시험 템플릿 목록 조회 요청 ===");
            
    //         // JWT 토큰에서 email 추출
    //         String email = extractEmailFromToken(request);
    //         if (email == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
    //         }
            
    //         // email로 member 정보 조회
    //         var memberInfo = memberService.getMemberInfoByEmail(email);
    //         if (memberInfo == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //         }
            
    //         List<TemplateResponseDTO> templates = templateService.getMyTemplates(
    //                 memberInfo.getMemberId(), memberInfo.getEducationId());
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 템플릿 목록 조회 성공", templates));
            
    //     } catch (Exception e) {
    //         log.error("시험 템플릿 목록 조회 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "시험 템플릿 목록 조회 실패: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 2. 시험 템플릿 생성
     * POST /api/templates
     */
    // @PostMapping
    // public ResponseEntity<ResponseDTO<TemplateResponseDTO>> createTemplate(
    //         @RequestBody TemplateCreateRequestDTO request,
    //         HttpServletRequest httpRequest) {
    //     try {
    //         log.info("=== 시험 템플릿 생성 요청 시작 ===");
    //         log.info("요청 본문의 memberId: {}", request.getMemberId());
    //         log.info("요청 본문의 educationId: {}", request.getEducationId());
    //         log.info("요청 본문의 templateName: {}", request.getTemplateName());
    //         log.info("요청 본문의 subGroupId: {}", request.getSubGroupId());
            
    //         // 요청 본문에 필수 필드가 없으면 JWT 토큰에서 추출
    //         if (request.getMemberId() == null || request.getMemberId().trim().isEmpty()) {
    //             log.info("요청 본문에 memberId가 없어서 JWT 토큰에서 추출");
    //             String email = extractEmailFromToken(httpRequest);
    //             var memberInfo = memberService.getMemberInfoByEmail(email);
    //             if (memberInfo == null) {
    //                 return ResponseEntity.badRequest()
    //                         .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //             }
    //             request.setMemberId(memberInfo.getMemberId());
    //             log.info("JWT 토큰에서 추출한 memberId: {}", request.getMemberId());
    //         }
            
    //         if (request.getEducationId() == null || request.getEducationId().trim().isEmpty()) {
    //             log.info("요청 본문에 educationId가 없어서 JWT 토큰에서 추출");
    //             String email = extractEmailFromToken(httpRequest);
    //             var memberInfo = memberService.getMemberInfoByEmail(email);
    //             if (memberInfo == null) {
    //                 return ResponseEntity.badRequest()
    //                         .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //             }
    //             request.setEducationId(memberInfo.getEducationId());
    //             log.info("JWT 토큰에서 추출한 educationId: {}", request.getEducationId());
    //         }
            
    //         log.info("=== 시험 템플릿 생성 요청 끝 ===");
            
    //         TemplateResponseDTO result = templateService.createTemplate(request);
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 템플릿 생성 성공", result));
            
    //     } catch (Exception e) {
    //         log.error("시험 템플릿 생성 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "시험 템플릿 생성 실패: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 3. 시험 템플릿 상세 정보 조회
     * GET /api/templates/{templateId}
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<ResponseDTO<TemplateResponseDTO>> getTemplateDetail(
            @PathVariable String templateId,
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("시험 템플릿 상세 조회 요청: templateId = {}, userId = {}", templateId, userId);
            
            // userId 파라미터가 있으면 권한 확인
            if (userId != null && !userId.trim().isEmpty()) {
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo == null) {
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다: " + userId));
                }
                log.info("사용자 권한 확인: memberId = {}, educationId = {}", 
                        memberInfo.getMemberId(), memberInfo.getEducationId());
            }
            
            TemplateResponseDTO template = templateService.getTemplateDetail(templateId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 템플릿 상세 조회 성공", template));
            
        } catch (Exception e) {
            log.error("시험 템플릿 상세 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "시험 템플릿 상세 조회 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 4. 시험 템플릿 수정
     * PUT /api/templates/{templateId}
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<ResponseDTO<TemplateResponseDTO>> updateTemplate(
            @PathVariable String templateId,
            @RequestBody TemplateCreateRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            log.info("=== 시험 템플릿 수정 요청 ===");
            log.info("templateId: {}", templateId);
            log.info("요청 데이터: {}", request);
            
            // JWT 토큰에서 email 추출
            String email = extractEmailFromToken(httpRequest);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            
            // email로 member 정보 조회
            var memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
            }
            
            // 시험 템플릿 수정
            TemplateResponseDTO result = templateService.updateTemplate(templateId, request, memberInfo.getMemberId());
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 템플릿 수정 성공", result));
            
        } catch (Exception e) {
            log.error("시험 템플릿 수정 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "시험 템플릿 수정 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 4-1. 시험 열기/닫기 업데이트
     * PATCH /api/templates/{templateId}
     */
    @PatchMapping("/{templateId}")
    public ResponseEntity<ResponseDTO<TemplateResponseDTO>> updateTemplateOpenClose(
            @PathVariable String templateId,
            @RequestBody TemplateUpdateRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            log.info("=== 시험 열기/닫기 업데이트 요청 ===");
            log.info("templateId: {}", templateId);
            log.info("templateOpen: {}, templateClose: {}", request.getTemplateOpen(), request.getTemplateClose());
            
            // JWT 토큰에서 email 추출
            String email = extractEmailFromToken(httpRequest);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            
            // email로 member 정보 조회
            var memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
            }
            
            // 시험 열기/닫기 업데이트
            TemplateResponseDTO result = templateService.updateTemplateOpenClose(templateId, request, memberInfo.getMemberId());
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 템플릿 업데이트 성공", result));
            
        } catch (Exception e) {
            log.error("시험 열기/닫기 업데이트 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "시험 템플릿 업데이트 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 4-2. 시험-문제 연결 수정
     * PUT /api/templates/{templateId}/questions
     */
    // @PutMapping("/{templateId}/questions")
    // public ResponseEntity<ResponseDTO<TemplateQuestionUpdateResponseDTO>> updateTemplateQuestions(
    //         @PathVariable String templateId,
    //         @RequestBody TemplateQuestionUpdateRequestDTO request,
    //         HttpServletRequest httpRequest) {
    //     try {
    //         log.info("=== 시험-문제 연결 수정 요청 ===");
    //         log.info("templateId: {}", templateId);
    //         log.info("수정할 문제 개수: {}", request.getQuestions().size());
            
    //         // JWT 토큰에서 email 추출
    //         String email = extractEmailFromToken(httpRequest);
    //         if (email == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
    //         }
            
    //         // email로 member 정보 조회
    //         var memberInfo = memberService.getMemberInfoByEmail(email);
    //         if (memberInfo == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //         }
            
    //         // 시험-문제 연결 수정
    //         TemplateQuestionUpdateResponseDTO result = templateService.updateTemplateQuestions(templateId, request, memberInfo.getMemberId());
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험-문제 연결 수정 성공", result));
            
    //     } catch (Exception e) {
    //         log.error("시험-문제 연결 수정 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "시험-문제 연결 수정 실패: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 5. 시험 템플릿 비활성화
     * PATCH /api/templates/{templateId}/status
     */
    @PatchMapping("/{templateId}/status")
    public ResponseEntity<ResponseDTO<String>> deactivateTemplate(
            @PathVariable String templateId,
            HttpServletRequest httpRequest) {
        try {
            log.info("=== 시험 템플릿 비활성화 요청 ===");
            log.info("templateId: {}", templateId);
            
            // JWT 토큰에서 email 추출
            String email = extractEmailFromToken(httpRequest);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            
            // email로 member 정보 조회
            var memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
            }
            
            // 시험 템플릿 비활성화
            templateService.deactivateTemplate(templateId, memberInfo.getMemberId());
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 템플릿이 성공적으로 비활성화되었습니다.", templateId));
            
        } catch (Exception e) {
            log.error("시험 템플릿 비활성화 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "시험 템플릿 비활성화 실패: " + e.getMessage()));
        }
    }
    

    
    /**
     * 7. 시험-문제 연결 (기존 기능 유지)
     * POST /api/templates/templateQuestions/create
     */
    // @PostMapping("/templateQuestions/create")
    // public ResponseEntity<ResponseDTO<TemplateResponseDTO>> createTemplateQuestions(
    //         @RequestBody TemplateQuestionCreateRequestDTO request) {
    //     try {
    //         log.info("시험-문제 연결 생성 요청: {}", request);
            
    //         TemplateResponseDTO result = templateService.createTemplateQuestions(request);
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험-문제 연결 생성 성공", result));
            
    //     } catch (Exception e) {
    //         log.error("시험-문제 연결 생성 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "시험-문제 연결 생성 실패: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 8. 내 시험 목록 조회 (기존 기능 유지)
     * GET /api/templates/my
     */
    @GetMapping("/my")
    public ResponseEntity<ResponseDTO<List<TemplateResponseDTO>>> getMyTemplates(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("내 시험 목록 조회 요청 - userId: {}", userId);
            
            MemberInfoDTO memberInfo = null;
            
            // 1. userId 파라미터가 있으면 우선 사용
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("userId 파라미터로 member 정보 조회 시도: {}", userId);
                memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo != null) {
                    log.info("userId {}로 조회된 member 정보: memberId={}, educationId={}", 
                            userId, memberInfo.getMemberId(), memberInfo.getEducationId());
                }
            }
            
            // 2. userId로 찾지 못했으면 JWT 토큰에서 email 추출
            if (memberInfo == null) {
                log.info("userId로 member 정보를 찾을 수 없어서 JWT 토큰에서 email 추출 시도");
                String email = extractEmailFromToken(request);
                if (email == null) {
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
                }
                
                memberInfo = memberService.getMemberInfoByEmail(email);
                if (memberInfo == null) {
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
                }
                
                log.info("JWT 토큰으로 조회된 member 정보: memberId={}, educationId={}", 
                        memberInfo.getMemberId(), memberInfo.getEducationId());
            }
            
            List<TemplateResponseDTO> templates = templateService.getMyTemplates(
                    memberInfo.getMemberId(), memberInfo.getEducationId());
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("내 시험 목록 조회 성공", templates));
            
        } catch (Exception e) {
            log.error("내 시험 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "내 시험 목록 조회 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 내 시험 목록 조회 (개선된 버전)
     * GET /api/templates/my-exams
     */
    // @GetMapping("/my-exams")
    // public ResponseEntity<ResponseDTO<List<TemplateResponseDTO>>> getMyExams(HttpServletRequest request) {
    //     try {
    //         log.info("내 시험 목록 조회 요청 (개선된 버전)");
            
    //         // JWT 토큰에서 사용자 정보 추출
    //         String email = extractEmailFromToken(request);
    //         var memberInfo = memberService.getMemberInfoByEmail(email);
    //         if (memberInfo == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //         }
            
    //         List<TemplateResponseDTO> templates = templateService.getMyTemplates(
    //                 memberInfo.getMemberId(), memberInfo.getEducationId());
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("내 시험 목록 조회 성공", templates));
            
    //     } catch (Exception e) {
    //         log.error("내 시험 목록 조회 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "내 시험 목록 조회 실패: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 9. 통합 시험 생성 (새 문제 생성 + 시험 템플릿 생성 + 시험-문제 연결)
     * POST /api/templates/create-complete
     */
    @PostMapping("/create-complete")
    public ResponseEntity<ResponseDTO<ExamDTO>> createCompleteExam(
            @RequestBody ExamCreateCompleteRequestDTO request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("=== 통합 시험 생성 요청 시작 ===");
            log.info("요청 URL: {}", httpRequest.getRequestURL());
            log.info("요청 메서드: {}", httpRequest.getMethod());
            log.info("요청 경로: {}", httpRequest.getRequestURI());
            
            // examData null 체크
            if (request.getExamData() == null) {
                log.error("examData가 null입니다.");
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "시험 데이터(examData)가 누락되었습니다."));
            }
            
            log.info("시험명: {}", request.getExamData().getTemplateName());
            log.info("examData 전체: {}", request.getExamData());
            log.info("userId: {}", request.getUserId());
            
            // JWT 토큰에서 email 추출
            String email = extractEmailFromToken(httpRequest);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            
            log.info("JWT 토큰에서 추출한 email: {}", email);
            
            // email로 member 정보 조회
            var memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                return ResponseEntity.badRequest()                                      
                        .body(ResponseDTO.createErrorResponse(400, "해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email));
            }
            
            log.info("조회된 member 정보: memberId = {}, educationId = {}, id = {}", 
                    memberInfo.getMemberId(), memberInfo.getEducationId(), memberInfo.getId());
            
            // 요청 데이터에 사용자 정보 설정 (없는 경우에만)
            if (request.getExamData().getMemberId() == null || request.getExamData().getMemberId().trim().isEmpty()) {
                // template 테이블에는 UUID를 저장하고, 문제 생성에는 사용자 로그인 ID를 사용
                request.getExamData().setMemberId(memberInfo.getMemberId());
                log.info("요청에 memberId 설정: {} (UUID)", memberInfo.getMemberId());
                log.info("참고: member의 id 필드 = {} (사용자 로그인 ID)", memberInfo.getId());
            }
            
            // 새 문제들에도 올바른 instructorId 설정 (사용자 로그인 ID 사용)
            if (request.getNewQuestions() != null) {
                for (var newQuestion : request.getNewQuestions()) {
                    if (newQuestion.getMemberId() == null || newQuestion.getMemberId().trim().isEmpty()) {
                        newQuestion.setMemberId(memberInfo.getId()); // 사용자 로그인 ID 사용
                        log.info("새 문제에 instructorId 설정: {} (사용자 로그인 ID)", memberInfo.getId());
                    }
                }
            }
            
            if (request.getExamData().getEducationId() == null || request.getExamData().getEducationId().trim().isEmpty()) {
                request.getExamData().setEducationId(memberInfo.getEducationId());
                log.info("요청에 educationId 설정: {}", memberInfo.getEducationId());
            }
            
            // 새 문제들에도 educationId 설정 (없는 경우에만)
            if (request.getNewQuestions() != null) {
                for (var newQuestion : request.getNewQuestions()) {
                    if (newQuestion.getEducationId() == null || newQuestion.getEducationId().trim().isEmpty()) {
                        newQuestion.setEducationId(memberInfo.getEducationId());
                        log.info("새 문제에 educationId 설정: {}", memberInfo.getEducationId());
                    }
                }
            }
            
            // 통합 시험 생성 처리
            ExamDTO response = templateService.createCompleteExam(request);
            
            log.info("=== 통합 시험 생성 완료 ===");
            log.info("templateId: {}", response.getTemplateId());
            log.info("새 문제: {}개", response.getNewQuestions());
            log.info("기존 문제: {}개", response.getBankQuestions());
            log.info("총 문제: {}개", response.getTotalQuestions());
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("시험 생성 성공", response));
                
        } catch (Exception e) {
            log.error("통합 시험 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "시험 생성 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 10. 답안 채점 API (강사용)
     * PUT /api/templates/{templateId}/answers/{answerId}/grade
     */
    // @PutMapping("/{templateId}/answers/{answerId}/grade")
    // public ResponseEntity<ResponseDTO<String>> gradeAnswer(
    //         @PathVariable String templateId,
    //         @PathVariable String answerId,
    //         @RequestBody Map<String, Object> requestBody,
    //         HttpServletRequest httpRequest) {
        
    //     try {
    //         log.info("답안 채점 요청: templateId={}, answerId={}", templateId, answerId);
            
    //         // JWT 토큰에서 email 추출
    //         String email = extractEmailFromToken(httpRequest);
    //         if (email == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
    //         }
            
    //         // email로 member 정보 조회
    //         var memberInfo = memberService.getMemberInfoByEmail(email);
    //         if (memberInfo == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //         }
            
    //         // 요청 데이터 추출
    //         Integer score = (Integer) requestBody.get("score");
    //         String teacherComment = (String) requestBody.get("teacherComment");
            
    //         if (score == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "점수는 필수입니다."));
    //         }
            
    //         // 답안 채점 처리
    //         templateService.gradeAnswer(answerId, score, teacherComment, memberInfo.getMemberId());
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("답안 채점 완료", "채점이 완료되었습니다."));
            
    //     } catch (Exception e) {
    //         log.error("답안 채점 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "답안 채점 실패: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 시험 종료 처리 - 미제출 학생 자동 0점 처리
     */
    // @PostMapping("/{templateId}/end")
    // public ResponseEntity<ResponseDTO<ExamDTO>> endExam(
    //         @PathVariable String templateId,
    //         HttpServletRequest httpRequest) {
    //     try {
    //         log.info("=== 시험 종료 처리 요청 ===");
    //         log.info("templateId: {}", templateId);
            
    //         // JWT 토큰에서 email 추출
    //         String email = extractEmailFromToken(httpRequest);
    //         if (email == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
    //         }
            
    //         // email로 member 정보 조회
    //         var memberInfo = memberService.getMemberInfoByEmail(email);
    //         if (memberInfo == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
    //         }
            
    //         // 시험 종료 처리
    //         ExamDTO result = templateService.endExam(templateId, memberInfo.getMemberId());
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse(
    //             "시험이 종료되었습니다. 미제출 학생들이 자동으로 0점 처리되었습니다.", result));
            
    //     } catch (RuntimeException e) {
    //         log.error("시험 종료 처리 실패: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
    //     } catch (Exception e) {
    //         log.error("시험 종료 처리 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.status(500)
    //                 .body(ResponseDTO.createErrorResponse(500, "시험 종료 처리 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }

    /**
     * 시험 종료 및 미제출 학생 자동 처리
     * POST /api/templates/{templateId}/close-with-auto-submission
     */
    @PostMapping("/{templateId}/close-with-auto-submission")
    public ResponseEntity<ResponseDTO<ExamDTO>> closeWithAutoSubmission(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            log.info("=== 시험 종료 및 미제출 학생 자동 처리 요청 ===");
            log.info("templateId: {}", templateId);
            
            // JWT 토큰에서 email 추출
            String email = extractEmailFromToken(httpRequest);
            if (email == null) {
                log.error("JWT 토큰에서 이메일을 추출할 수 없습니다.");
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            log.info("JWT 토큰에서 추출한 email: {}", email);
            
            // email로 member 정보 조회
            var memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                log.error("사용자 정보를 찾을 수 없습니다: email={}", email);
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다."));
            }
            log.info("사용자 정보 조회 성공: memberId={}", memberInfo.getMemberId());
            
            // 요청 데이터 추출
            String templateClose = (String) request.get("templateClose");
            @SuppressWarnings("unchecked")
            List<String> studentMemberIds = (List<String>) request.get("studentMemberIds");
            
            // 시험 종료 및 미제출 학생 자동 처리
            log.info("TemplateService.closeWithAutoSubmission 호출 시작");
            ExamDTO result = templateService.closeWithAutoSubmission(
                    templateId, templateClose, studentMemberIds, memberInfo.getMemberId());
            log.info("TemplateService.closeWithAutoSubmission 호출 완료: result={}", result);
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse(
                "시험이 종료되었습니다. 미제출 학생들이 자동으로 0점 처리되었습니다.", result));
            
        } catch (RuntimeException e) {
            log.error("시험 종료 및 미제출 학생 자동 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("시험 종료 및 미제출 학생 자동 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.createErrorResponse(500, "시험 종료 및 미제출 학생 자동 처리 중 오류가 발생했습니다: " + e.getMessage()));
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
                    log.info("Token found in Authorization header");
                    return jwtUtil.getUserEmail(token);
                }
            }
            
            // 쿠키에서 토큰 추출
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                            log.info("Token found in Cookie: {}", cookie.getName());
                            return jwtUtil.getUserEmail(token);
                        }
                    }
                }
            }
            
            throw new RuntimeException("JWT token not found in Authorization header or Cookie");
        } catch (Exception e) {
            log.error("JWT 토큰에서 email 추출 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("JWT token not found in Authorization header or Cookie");
        }
    }
}
