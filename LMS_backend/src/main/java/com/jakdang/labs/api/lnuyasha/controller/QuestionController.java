package com.jakdang.labs.api.lnuyasha.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.lnuyasha.dto.*;
import com.jakdang.labs.api.lnuyasha.service.*;
import com.jakdang.labs.api.lnuyasha.repository.QuestionRepository;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.entity.QuestionEntity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    
    private final QuestionBankService questionBankService;
    private final SubjectDetailService subjectDetailService;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final QuestionRepository questionRepository;

    // 세부과목 테이블에서 세부과목 이름이랑 세부과목 아이디 등 모든 값 조회 (과목명 포함) + 해당 세부과목의 문제 목록
    @GetMapping("/subdetail")
    public ResponseDTO<List<SubDetailWithQuestionsDTO>> getSubDetailListWithQuestions(
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            String educationId;
            
            if (userId != null && !userId.trim().isEmpty()) {
                // userId로 직접 member 정보 조회
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo == null || memberInfo.getEducationId() == null) {
                    return ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다.");
                }
                educationId = memberInfo.getEducationId();
                log.info("userId {}로 조회된 educationId: {}", userId, educationId);
            } else {
                // JWT 토큰에서 이메일 추출
                String email = extractEmailFromToken(request);
                MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
                if (memberInfo == null || memberInfo.getEducationId() == null) {
                    return ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다.");
                }
                educationId = memberInfo.getEducationId();
                log.info("JWT 토큰으로 조회된 educationId: {}", educationId);
            }
            
            List<SubDetailWithQuestionsDTO> subDetailList = subjectDetailService.getSubDetailListWithQuestions(educationId);
            return ResponseDTO.createSuccessResponse("세부과목 및 문제 목록 조회가 완료되었습니다.", subDetailList);
        } catch (Exception e) {
            log.error("세부과목 및 문제 목록 조회 중 에러 발생", e);
            return ResponseDTO.createErrorResponse(500, "세부과목 및 문제 목록 조회 실패: " + e.getMessage());
        }
    }

    // 과목별 세부과목 목록 조회 (그룹화된 구조)
    @GetMapping("/subjectList")
    public ResponseDTO<Map<String, SubjectGroupDTO>> getSubjectList(
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            String educationId;
            
            if (userId != null && !userId.trim().isEmpty()) {
                // userId로 직접 member 정보 조회
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo == null || memberInfo.getEducationId() == null) {
                    return ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다.");
                }
                educationId = memberInfo.getEducationId();
                log.info("userId {}로 조회된 educationId: {}", userId, educationId);
            } else {
                // JWT 토큰에서 이메일 추출
                String email = extractEmailFromToken(request);
                MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
                if (memberInfo == null || memberInfo.getEducationId() == null) {
                    return ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다.");
                }
                educationId = memberInfo.getEducationId();
                log.info("JWT 토큰으로 조회된 educationId: {}", educationId);
            }
            
            Map<String, SubjectGroupDTO> subjectList = subjectDetailService.getSubjectList(educationId);
            return ResponseDTO.createSuccessResponse("과목별 세부과목 목록 조회가 완료되었습니다.", subjectList);
        } catch (Exception e) {
            log.error("과목별 세부과목 목록 조회 중 에러 발생", e);
            return ResponseDTO.createErrorResponse(500, "과목별 세부과목 목록 조회 실패: " + e.getMessage());
        }
    }
    /**
     * 문제 목록 조회 (검색, 필터링, 페이지네이션)
     * 프론트엔드의 getQuestions API에 대응
     */
    // @GetMapping
    // public ResponseEntity<ResponseDTO<Map<String, Object>>> getQuestions(
    //         @RequestParam(required = false) String keyword,
    //         @RequestParam(required = false) String subject,
    //         @RequestParam(required = false) String type,
    //         @RequestParam(required = false) String instructor,
    //         @RequestParam(defaultValue = "1") int page,
    //         @RequestParam(defaultValue = "10") int limit) {
        
    //     try {
    //         log.info("문제 목록 조회 요청 - keyword: {}, subject: {}, type: {}, instructor: {}, page: {}, limit: {}", 
    //                 keyword, subject, type, instructor, page, limit);
            
    //         // 검색 조건 구성
    //         QuestionSearchRequestDTO searchRequest = QuestionSearchRequestDTO.builder()
    //                 .searchTerm(keyword)
    //                 .subDetailId(subject)
    //                 .questionType(type)
    //                 .memberId(instructor)
    //                 .build();
            
    //         // 문제 목록 조회
    //         QuestionListResponseDTO response = questionBankService.searchQuestionsWithFilters(searchRequest, page, limit);
            
    //         // 응답 데이터 구성
    //         Map<String, Object> responseData = new HashMap<>();
    //         responseData.put("questions", response.getQuestions());
    //         responseData.put("totalCount", response.getTotalCount());
    //         responseData.put("currentPage", response.getCurrentPage());
    //         responseData.put("limit", response.getLimit());
    //         responseData.put("totalPages", response.getTotalPages());
    //         responseData.put("hasNext", response.isHasNext());
    //         responseData.put("hasPrevious", response.isHasPrevious());
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("문제 목록 조회가 완료되었습니다.", responseData));
            
    //     } catch (Exception e) {
    //         log.error("문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "문제 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 문제 상세 조회
     * 프론트엔드의 getQuestionDetail API에 대응
     */

    
    /**
     * 다중 문제 생성
     * POST /api/questions/create-batch
     */
    // @PostMapping("/create-batch")
    // public ResponseEntity<ResponseDTO<QuestionBatchCreateResponseDTO>> createQuestionsBatch(
    //         HttpServletRequest request,
    //         @RequestBody QuestionBatchCreateRequestDTO batchRequest) {
        
    //     try {
    //         log.info("=== 다중 문제 생성 요청 시작 ===");
    //         log.info("요청된 문제 개수: {}", batchRequest.getQuestions().size());
            
    //         // JWT 토큰에서 email 추출
    //         String email = extractEmailFromToken(request);
    //         if (email == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
    //         }
            
    //         log.info("JWT 토큰에서 추출한 email: {}", email);
            
    //         // email로 member 정보 조회
    //         MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
    //         if (memberInfo == null) {
    //             return ResponseEntity.badRequest()
    //                     .body(ResponseDTO.createErrorResponse(400, "해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email));
    //         }
            
    //         log.info("조회된 member 정보: memberId = {}, educationId = {}", 
    //                 memberInfo.getId(), memberInfo.getEducationId());
            
    //         QuestionBatchCreateResponseDTO result = questionBankService.createQuestionsBatch(memberInfo.getId(), batchRequest);
            
    //         log.info("=== 다중 문제 생성 완료: {}개 ===", result.getCreatedQuestions().size());
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("문제 일괄 생성 성공", result));
            
    //     } catch (Exception e) {
    //         log.error("다중 문제 생성 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "다중 문제 생성 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 문제 생성
     * 프론트엔드의 createQuestion API에 대응
     */
    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<QuestionDTO>> createQuestion(
            HttpServletRequest request,
            @RequestBody QuestionCreateRequestDTO questionRequest) {
        
        try {
            log.info("=== 문제 생성 요청 상세 정보 ===");
            log.info("문제 내용: {}", questionRequest.getQuestionText());
            log.info("문제 유형: {}", questionRequest.getQuestionType());
            log.info("세부과목명: {}", questionRequest.getSubDetailName());
            log.info("정답: {}", questionRequest.getQuestionAnswer());
            log.info("해설: {}", questionRequest.getExplanation());
            log.info("코드 언어: {}", questionRequest.getCodeLanguage());
            log.info("선택지 개수: {}", questionRequest.getOptions() != null ? questionRequest.getOptions().size() : 0);
            if (questionRequest.getOptions() != null) {
                for (int i = 0; i < questionRequest.getOptions().size(); i++) {
                    var option = questionRequest.getOptions().get(i);
                    log.info("선택지 {}: text={}, isCorrect={}", i + 1, option.getOptText(), option.getOptIsCorrect());
                }
            }
            log.info("=== 문제 생성 요청 상세 정보 끝 ===");
            
            // JWT 토큰에서 email 추출
            String email = extractEmailFromToken(request);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            
            log.info("JWT 토큰에서 추출한 email: {}", email);
            
            // email로 member 정보 조회
            MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email));
            }
            
            log.info("조회된 member 정보: memberId = {}, educationId = {}, memberRole = {}", 
                    memberInfo.getId(), memberInfo.getEducationId() == null ? "1" : memberInfo.getEducationId(), memberInfo.getMemberRole());
            
            // id를 request에 설정
            questionRequest.setInstructorId(memberInfo.getId());
            
            log.info("설정된 instructorId: {}", questionRequest.getInstructorId());
            
            QuestionDTO createdQuestion = questionBankService.createQuestion(memberInfo.getId(), questionRequest);
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("문제가 성공적으로 생성되었습니다.", createdQuestion));
            
        } catch (Exception e) {
            log.error("문제 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "문제 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 쿠키에서 직접 memberId 추출
     */
    private String extractMemberIdFromCookie(HttpServletRequest request) {
        try {
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("memberId".equals(cookie.getName())) {
                        String memberId = cookie.getValue();
                        if (memberId != null && !memberId.isEmpty() && !memberId.equals("undefined")) {
                            log.info("쿠키에서 직접 추출한 memberId: {}", memberId);
                            return memberId;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("쿠키에서 memberId 추출 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JWT 토큰에서 email 추출
     */
    private String extractEmailFromToken(HttpServletRequest request) {
        // 쿠키에서 refresh 토큰 추출
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    String refreshToken = cookie.getValue();
                    return jwtUtil.getUserEmail(refreshToken);
                }
            }
        }
        
        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserEmail(token);
        }
        
        return null;
    }
    
    /**
     * 문제 수정
     * 프론트엔드 요구사항에 맞춰 구현
     * @param questionId 문제 ID (Path Parameter)
     * @param request 수정 요청 데이터
     * @return 수정된 문제 정보
     */
    @PostMapping("/{questionId}/update")
    public ResponseEntity<ResponseDTO<QuestionDTO>> updateQuestion(
            @PathVariable String questionId,
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("문제 수정 요청: questionId = {}, request = {}", questionId, request);
            
            // RequestBody에서 데이터 추출 (프론트엔드 요구사항에 맞춰)
            String instructorId = (String) request.get("instructorId");
            String userId = (String) request.get("userId");
            
            // 권한 확인 (선택사항)
            if (instructorId != null) {
                log.info("수정 요청한 강사 ID: {}", instructorId);
            }
            if (userId != null) {
                log.info("수정 요청한 사용자 ID: {}", userId);
            }
            
            // options 처리 (QuestionOptionDTO 형태로 오는 경우)
            List<String> optionsList = null;
            List<QuestionOptionDTO> questionOptionsList = null;
            Object optionsObj = request.get("options");
            if (optionsObj instanceof List) {
                List<?> options = (List<?>) optionsObj;
                if (!options.isEmpty() && options.get(0) instanceof Map) {
                    // QuestionOptionDTO 형태로 온 경우
                    questionOptionsList = options.stream()
                            .map(option -> {
                                if (option instanceof Map) {
                                    Map<?, ?> optionMap = (Map<?, ?>) option;
                                    return QuestionOptionDTO.builder()
                                            .optText((String) optionMap.get("optText"))
                                            .optIsCorrect((Integer) optionMap.get("optIsCorrect"))
                                            .build();
                                }
                                return null;
                            })
                            .filter(option -> option != null)
                            .collect(java.util.stream.Collectors.toList());
                    log.info("QuestionOptionDTO 형태의 options를 변환: {}", questionOptionsList);
                } else {
                    // 이미 String 리스트인 경우
                    optionsList = options.stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.toList());
                }
            }
            
            // QuestionUpdateRequestDTO로 변환 (프론트엔드 필드명 지원)
            QuestionUpdateRequestDTO updateRequest = QuestionUpdateRequestDTO.builder()
                    .questionId((String) request.get("questionId"))
                    .type((String) request.get("type"))
                    .question((String) request.get("question"))
                    .correctAnswer((String) request.get("correctAnswer"))
                    .codeLanguage((String) request.get("codeLanguage"))
                    .subject((String) request.get("subject"))
                    .options(optionsList)
                    .questionOptions(questionOptionsList)
                    .instructorId(instructorId)
                    // 기존 필드들도 지원 (하위 호환성)
                    .questionType((String) request.get("questionType"))
                    .questionText((String) request.get("questionText"))
                    .questionAnswer((String) request.get("questionAnswer"))
                    .explanation((String) request.get("explanation"))
                    .subDetailId((String) request.get("subDetailId"))
                    .points((Integer) request.get("points"))
                    .build();
            
            log.info("변환된 updateRequest: {}", updateRequest);
            
            QuestionDTO updatedQuestion = questionBankService.updateQuestion(questionId, updateRequest);
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("문제가 성공적으로 수정되었습니다.", updatedQuestion));
            
        } catch (Exception e) {
            log.error("문제 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "문제 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
   
    /**
     * 문제 상태 변경 (활성화/비활성화)
     * PATCH /api/questions/{questionId}/status
     * @param questionId 문제 ID
     * @param request 상태 변경 요청 정보
     * @return 상태 변경 결과
     */
    @PatchMapping("/{questionId}/status")
    public ResponseEntity<ResponseDTO<String>> updateQuestionStatus(
            @PathVariable String questionId,
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("=== 문제 상태 변경 요청 ===");
            log.info("questionId: {}", questionId);
            log.info("request: {}", request);
            
            // RequestBody에서 데이터 추출
            log.info("전체 request 데이터: {}", request);
            log.info("request의 모든 키: {}", request.keySet());
            
            Object questionActiveObj = request.get("questionActive");
            log.info("questionActive 원본 값: {} (타입: {})", questionActiveObj, questionActiveObj != null ? questionActiveObj.getClass().getSimpleName() : "null");
            
            Integer questionActive = null;
            if (questionActiveObj != null) {
                if (questionActiveObj instanceof Integer) {
                    questionActive = (Integer) questionActiveObj;
                } else if (questionActiveObj instanceof String) {
                    try {
                        questionActive = Integer.parseInt((String) questionActiveObj);
                    } catch (NumberFormatException e) {
                        log.error("questionActive 문자열을 숫자로 변환 실패: {}", questionActiveObj);
                    }
                } else if (questionActiveObj instanceof Number) {
                    questionActive = ((Number) questionActiveObj).intValue();
                }
            }
            
            String userId = (String) request.get("userId");
            
            log.info("변환된 questionActive: {}", questionActive);
            
            // questionActive 검증 (기본값 설정)
            if (questionActive == null) {
                log.warn("questionActive가 null이므로 기본값 1(비활성화)로 설정합니다.");
                questionActive = 1; // 기본값: 비활성화
            }
            
            if (questionActive != 0 && questionActive != 1) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "questionActive는 0(활성화) 또는 1(비활성화)만 가능합니다."));
            }
            
            log.info("상태 변경 요청: questionActive = {}", questionActive);
            if (userId != null) {
                log.info("요청한 사용자 ID: {}", userId);
            }
            
            // 문제 상태 업데이트
            questionBankService.updateQuestionStatus(questionId, questionActive);
            
            String message = questionActive == 1 ? "문제가 성공적으로 비활성화되었습니다." : "문제가 성공적으로 활성화되었습니다.";
            log.info("=== 문제 상태 변경 완료 ===");
            log.info("questionId: {}, newStatus: {}", questionId, questionActive == 1 ? "비활성화" : "활성화");
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse(message, questionId));
            
        } catch (Exception e) {
            log.error("문제 상태 변경 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "문제 상태 변경 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 문제 논리적 삭제 (비활성화) - 기존 엔드포인트 (하위 호환성)
     * /questions/{id}/delete 경로로 요청
     * @param questionId 문제 ID
     * @param request 삭제 요청 정보 (questionActive, instructorId)
     * @return 삭제 결과
     */
    // @PostMapping("/{questionId}/delete")
    // public ResponseEntity<ResponseDTO<String>> deleteQuestion(
    //         @PathVariable String questionId,
    //         @RequestBody Map<String, Object> request) {
        
    //     try {
    //         log.info("문제 논리적 삭제 요청: questionId = {}, request = {}", questionId, request);
            
    //         // RequestBody에서 데이터 추출
    //         Integer questionActive = (Integer) request.get("questionActive");
    //         String instructorId = (String) request.get("instructorId");
            
    //         // 기본값 설정
    //         if (questionActive == null) {
    //             questionActive = 1; // 기본값: 비활성화
    //         }
            
    //         // 권한 확인 (선택사항)
    //         if (instructorId != null) {
    //             log.info("삭제 요청한 강사 ID: {}", instructorId);
    //             // 여기에 권한 확인 로직을 추가할 수 있습니다
    //         }
            
    //         // 문제 상태 업데이트
    //         questionBankService.updateQuestionStatus(questionId, questionActive);
            
    //         String message = questionActive == 1 ? "문제가 성공적으로 삭제되었습니다." : "문제가 성공적으로 활성화되었습니다.";
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse(message, "삭제 완료"));
            
    //     } catch (Exception e) {
    //         log.error("문제 삭제 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "문제 삭제 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 문제은행 통계 조회
     * 프론트엔드의 getQuestionBankStats API에 대응
     */
    @GetMapping("/stats")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getQuestionBankStats(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request) {
        
        try {
            log.info("문제은행 통계 조회 요청 - userId: {}", userId);
            
            String memberId = null;
            
            // 1. userId 파라미터가 있으면 우선 사용
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("userId 파라미터로 memberId 조회 시도: {}", userId);
                
                // userId가 id인지 memberId인지 확인하기 위해 member 정보 조회
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo != null) {
                    log.info("userId {}로 조회된 member 정보: memberId={}, id={}", 
                            userId, memberInfo.getMemberId(), memberInfo.getId());
                    
                    // memberId로 먼저 시도
                    try {
                        QuestionListResponseDTO.QuestionBankStats stats = questionBankService.getQuestionBankStats(memberInfo.getMemberId());
                        if (stats.getTotalQuestions() > 0) {
                            log.info("memberId로 통계 조회 성공: {}", memberInfo.getMemberId());
                            memberId = memberInfo.getMemberId();
                        }
                    } catch (Exception e) {
                        log.warn("memberId로 통계 조회 실패: {}, 오류: {}", memberInfo.getMemberId(), e.getMessage());
                    }
                    
                    // memberId로 실패하면 id로 시도
                    if (memberId == null) {
                        try {
                            QuestionListResponseDTO.QuestionBankStats stats = questionBankService.getQuestionBankStats(memberInfo.getId());
                            log.info("id로 통계 조회 성공: {}", memberInfo.getId());
                            memberId = memberInfo.getId();
                        } catch (Exception e) {
                            log.warn("id로 통계 조회 실패: {}, 오류: {}", memberInfo.getId(), e.getMessage());
                        }
                    }
                } else {
                    log.warn("userId {}에 해당하는 member 정보를 찾을 수 없습니다.", userId);
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "유효하지 않은 userId입니다: " + userId));
                }
            }
            
            // 2. userId 파라미터가 없거나 실패한 경우 JWT 토큰에서 이메일 추출
            if (memberId == null) {
                log.info("JWT 토큰에서 이메일 추출 시도");
                String email = extractEmailFromToken(request);
                if (email == null) {
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
                }
                
                log.info("JWT 토큰에서 추출한 email: {}", email);
                
                // email로 member 정보 조회
                MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
                if (memberInfo == null) {
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email));
                }
                
                log.info("JWT 토큰으로 조회된 member 정보: memberId = {}, educationId = {}", 
                        memberInfo.getMemberId(), memberInfo.getEducationId());
                
                memberId = memberInfo.getMemberId();
            }
            
            // 3. 최종적으로 memberId로 통계 조회
            if (memberId == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "유효한 memberId를 찾을 수 없습니다."));
            }
            
                            QuestionListResponseDTO.QuestionBankStats stats = questionBankService.getQuestionBankStats(memberId);
            
            // 프론트엔드 요구사항에 맞춰 응답 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("totalQuestions", stats.getTotalQuestions());
            responseData.put("activeQuestions", stats.getActiveQuestions());
            responseData.put("inactiveQuestions", stats.getInactiveQuestions());
            responseData.put("objectiveCount", stats.getObjectiveCount());
            responseData.put("descriptiveCount", stats.getDescriptiveCount());
            responseData.put("codeCount", stats.getCodeCount());
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("문제은행 통계 조회가 완료되었습니다.", responseData));
            
        } catch (Exception e) {
            log.error("문제은행 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "문제은행 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 전체 문제 목록 조회 (모든 문제 + 강사 정보)
     */
    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getAllQuestions(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("전체 문제 목록 조회 요청 (모든 문제 + 강사 정보) - userId: {}", userId);
            
            String educationId = null;
            
            // 1. userId 파라미터가 있으면 우선 사용
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("userId 파라미터로 member 정보 조회 시도: {}", userId);
                
                try {
                    // userId로 member 정보 조회
                    MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                    log.info("memberService.getMemberInfo 결과: {}", memberInfo);
                    
                    if (memberInfo != null && memberInfo.getEducationId() != null) {
                        educationId = memberInfo.getEducationId();
                        log.info("userId {}로 조회된 educationId: {}", userId, educationId);
                    } else {
                        log.warn("userId {}에 해당하는 member 정보를 찾을 수 없거나 educationId가 없습니다. memberInfo: {}", userId, memberInfo);
                    }
                } catch (Exception e) {
                    log.error("userId로 member 정보 조회 중 오류: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
            
            // 2. userId로 찾지 못했으면 JWT 토큰에서 email 추출
            if (educationId == null) {
                log.info("userId로 educationId를 찾지 못해서 JWT 토큰에서 email 추출 시도");
                
                try {
                    String email = extractEmailFromToken(request);
                    log.info("JWT 토큰에서 추출한 email: {}", email);
                    
                    if (email == null) {
                        return ResponseEntity.badRequest()
                                .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
                    }
                    
                    // email로 member 정보 조회
                    MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
                    log.info("email로 조회된 memberInfo: {}", memberInfo);
                    
                    if (memberInfo == null) {
                        return ResponseEntity.badRequest()
                                .body(ResponseDTO.createErrorResponse(400, "해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email));
                    }
                    
                    educationId = memberInfo.getEducationId();
                    log.info("JWT 토큰으로 조회된 educationId: {}", educationId);
                } catch (Exception e) {
                    log.error("JWT 토큰에서 이메일 추출 중 오류: error={}", e.getMessage(), e);
                    return ResponseEntity.badRequest()
                            .body(ResponseDTO.createErrorResponse(400, "JWT 토큰 처리 중 오류가 발생했습니다: " + e.getMessage()));
                }
            }
            
            // 3. educationId가 없으면 오류 반환
            if (educationId == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "educationId를 찾을 수 없습니다."));
            }
            
            log.info("최종 사용할 educationId: {}", educationId);
            
            // 모든 문제와 강사 정보 조회
            try {
                Map<String, Object> response = questionBankService.getAllQuestionsWithInstructors(educationId);
                log.info("questionBankService.getAllQuestionsWithInstructors 성공");
                return ResponseEntity.ok(ResponseDTO.createSuccessResponse("전체 문제 목록과 강사 정보 조회가 완료되었습니다.", response));
            } catch (Exception e) {
                log.error("questionBankService.getAllQuestionsWithInstructors 중 오류: educationId={}, error={}", educationId, e.getMessage(), e);
                throw e;
            }
            
        } catch (Exception e) {
            log.error("전체 문제 목록 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "전체 문제 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 현재 로그인한 사용자의 문제 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> getMyQuestions(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request) {
        try {
            log.info("현재 사용자의 문제 목록 조회 요청 - userId: {}", userId);
            
            // 1. userId 파라미터가 있으면 우선 사용
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("userId 파라미터로 문제 조회 시도: {}", userId);
                
                // userId가 id인지 memberId인지 확인하기 위해 member 정보 조회
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo != null) {
                    log.info("userId {}로 조회된 member 정보: memberId={}, id={}", 
                            userId, memberInfo.getMemberId(), memberInfo.getId());
                    
                    // memberId로 먼저 시도
                    try {
                        QuestionListResponseDTO response = questionBankService.getQuestionsByTeacher(memberInfo.getMemberId());
                        if (response.getQuestions().size() > 0) {
                            log.info("memberId로 조회 성공: {}", memberInfo.getMemberId());
                            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("현재 사용자의 문제 목록 조회가 완료되었습니다.", response));
                        }
                    } catch (Exception e) {
                        log.warn("memberId로 조회 실패: {}, 오류: {}", memberInfo.getMemberId(), e.getMessage());
                    }
                    
                    // memberId로 실패하면 id로 시도
                    try {
                        QuestionListResponseDTO response = questionBankService.getQuestionsByTeacher(memberInfo.getId());
                        log.info("id로 조회 성공: {}", memberInfo.getId());
                        return ResponseEntity.ok(ResponseDTO.createSuccessResponse("현재 사용자의 문제 목록 조회가 완료되었습니다.", response));
                    } catch (Exception e) {
                        log.warn("id로도 조회 실패: {}, 오류: {}", memberInfo.getId(), e.getMessage());
                    }
                } else {
                    // member 정보를 찾을 수 없으면 userId를 그대로 사용
                    log.warn("userId {}에 해당하는 member 정보를 찾을 수 없어서 userId를 그대로 사용", userId);
                    QuestionListResponseDTO response = questionBankService.getQuestionsByTeacher(userId);
                    return ResponseEntity.ok(ResponseDTO.createSuccessResponse("현재 사용자의 문제 목록 조회가 완료되었습니다.", response));
                }
            }
            
            // 2. userId가 없으면 쿠키에서 직접 memberId 추출 시도
            String memberId = extractMemberIdFromCookie(request);
            
            if (memberId != null) {
                log.info("쿠키에서 직접 추출한 memberId로 문제 조회: {}", memberId);
                QuestionListResponseDTO response = questionBankService.getQuestionsByTeacher(memberId);
                return ResponseEntity.ok(ResponseDTO.createSuccessResponse("현재 사용자의 문제 목록 조회가 완료되었습니다.", response));
            }
            
            // 2. 쿠키에서 memberId를 찾을 수 없으면 JWT 토큰에서 email 추출 후 member 정보 조회
            log.info("쿠키에서 memberId를 찾을 수 없어서 JWT 토큰에서 email 추출 시도");
            String email = extractEmailFromToken(request);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "JWT 토큰에서 이메일을 추출할 수 없습니다."));
            }
            
            log.info("JWT 토큰에서 추출한 email: {}", email);
            
            // email로 member 정보 조회
            MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
            if (memberInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDTO.createErrorResponse(400, "해당 이메일로 등록된 회원 정보를 찾을 수 없습니다: " + email));
            }
            
            log.info("조회된 member 정보: memberId = {}, id = {}, educationId = {}, memberRole = {}", 
                    memberInfo.getMemberId(), memberInfo.getId(), memberInfo.getEducationId(), memberInfo.getMemberRole());
            
            // 디버깅: 모든 문제의 memberId 확인
            log.info("=== 디버깅: 데이터베이스의 모든 문제 정보 확인 ===");
            try {
                List<QuestionEntity> allQuestions = questionRepository.findAll();
                log.info("전체 문제 수: {}", allQuestions.size());
                for (QuestionEntity q : allQuestions) {
                    log.info("문제 ID: {}, memberId: {}, educationId: {}, questionActive: {}", 
                             q.getQuestionId(), q.getMemberId(), q.getEducationId(), q.getQuestionActive());
                }
            } catch (Exception e) {
                log.error("전체 문제 조회 중 오류: {}", e.getMessage());
            }
            
            QuestionListResponseDTO response = null;
            
            // 3. memberId로 시도
            try {
                log.info("memberId로 문제 조회 시도: memberId = {}", memberInfo.getMemberId());
                response = questionBankService.getQuestionsByTeacher(memberInfo.getMemberId());
                log.info("memberId로 조회 성공: 조회된 문제 수 = {}", response.getQuestions().size());
            } catch (Exception e) {
                log.warn("memberId로 조회 실패: memberId = {}, 오류 = {}", memberInfo.getMemberId(), e.getMessage());
                
                // 4. memberId로 실패하면 id로 시도
                try {
                    log.info("id로 문제 조회 시도: id = {}", memberInfo.getId());
                    response = questionBankService.getQuestionsByTeacher(memberInfo.getId());
                    log.info("id로 조회 성공: 조회된 문제 수 = {}", response.getQuestions().size());
                } catch (Exception e2) {
                    log.error("id로도 조회 실패: id = {}, 오류 = {}", memberInfo.getId(), e2.getMessage());
                    
                    // 5. educationId로 시도 (전체 문제 조회)
                    try {
                        log.info("educationId로 전체 문제 조회 시도: educationId = {}", memberInfo.getEducationId());
                        response = questionBankService.getAllQuestions(memberInfo.getEducationId());
                        log.info("educationId로 조회 성공: 조회된 문제 수 = {}", response.getQuestions().size());
                    } catch (Exception e3) {
                        log.error("educationId로도 조회 실패: educationId = {}, 오류 = {}", memberInfo.getEducationId(), e3.getMessage());
                        throw new RuntimeException("사용자의 문제 목록을 찾을 수 없습니다. memberId: " + memberInfo.getMemberId() + ", id: " + memberInfo.getId() + ", educationId: " + memberInfo.getEducationId());
                    }
                }
            }
            
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("현재 사용자의 문제 목록 조회가 완료되었습니다.", response));
            
        } catch (Exception e) {
            log.error("현재 사용자의 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "현재 사용자의 문제 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 특정 강사의 문제 목록 조회
     */
    // @GetMapping("/teacher/{memberId}")
    // public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> getQuestionsByTeacher(
    //         @PathVariable String memberId) {
        
    //     try {
    //         log.info("강사별 문제 목록 조회 요청: memberId = {}", memberId);
            
    //         QuestionListResponseDTO response = questionBankService.getQuestionsByTeacher(memberId);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강사별 문제 목록 조회가 완료되었습니다.", response));
            
    //     } catch (Exception e) {
    //         log.error("강사별 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "강사별 문제 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 문제 유형별 목록 조회
     */
    // @GetMapping("/type/{questionType}")
    // public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> getQuestionsByType(
    //         @PathVariable String questionType) {
        
    //     try {
    //         log.info("문제 유형별 목록 조회 요청: questionType = {}", questionType);
            
    //         QuestionListResponseDTO response = questionBankService.getQuestionsByType(questionType);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("문제 유형별 목록 조회가 완료되었습니다.", response));
            
    //     } catch (Exception e) {
    //         log.error("문제 유형별 목록 조회 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "문제 유형별 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 세부과목별 문제 목록 조회
     */
    // @GetMapping("/subdetail/{subDetailId}")
    // public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> getQuestionsBySubDetail(
    //         @PathVariable String subDetailId) {
        
    //     try {
    //         log.info("세부과목별 문제 목록 조회 요청: subDetailId = {}", subDetailId);
            
    //         QuestionListResponseDTO response = questionBankService.getQuestionsBySubDetail(subDetailId);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("세부과목별 문제 목록 조회가 완료되었습니다.", response));
            
    //     } catch (Exception e) {
    //         log.error("세부과목별 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "세부과목별 문제 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 상위 과목별 문제 목록 조회
     */
    // @GetMapping("/subject/{subjectId}")
    // public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> getQuestionsBySubject(
    //         @PathVariable String subjectId) {
        
    //     try {
    //         log.info("상위 과목별 문제 목록 조회 요청: subjectId = {}", subjectId);
            
    //         QuestionListResponseDTO response = questionBankService.getQuestionsBySubject(subjectId);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("상위 과목별 문제 목록 조회가 완료되었습니다.", response));
            
    //     } catch (Exception e) {
    //         log.error("상위 과목별 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "상위 과목별 문제 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    /**
     * 키워드 검색
     */
    // @GetMapping("/search")
    // public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> searchQuestions(
    //         @RequestParam String keyword) {
        
    //     try {
    //         log.info("키워드 검색 요청: keyword = {}", keyword);
            
    //         QuestionListResponseDTO response = questionBankService.searchQuestions(keyword);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("키워드 검색이 완료되었습니다.", response));
            
    //     } catch (Exception e) {
    //         log.error("키워드 검색 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "키워드 검색 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    // /**
    //  * 실시간 검색 (복합 조건)
    //  */
    // @PostMapping("/realtime-search")
    // public ResponseEntity<ResponseDTO<QuestionListResponseDTO>> realTimeSearch(
    //         @RequestBody Map<String, String> request) {
        
    //     try {
    //         log.info("실시간 검색 요청: {}", request);
            
    //         String keyword = request.get("keyword");
    //         String questionType = request.get("questionType");
    //         String memberId = request.get("memberId");
    //         String subDetailId = request.get("subDetailId");
    //         String educationId = request.get("educationId");
    //         String sortBy = request.get("sortBy");
    //         String sortDirection = request.get("sortDirection");
            
    //         QuestionListResponseDTO response = questionBankService.realTimeSearch(
    //                 keyword, questionType, memberId, subDetailId, educationId, sortBy, sortDirection);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("실시간 검색이 완료되었습니다.", response));
            
    //     } catch (Exception e) {
    //         log.error("실시간 검색 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "실시간 검색 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    // /**
    //  * 실시간 검색 결과 개수 조회
    //  */
    // @PostMapping("/realtime-search/count")
    // public ResponseEntity<ResponseDTO<Long>> getRealTimeSearchCount(
    //         @RequestBody Map<String, String> request) {
        
    //     try {
    //         log.info("실시간 검색 결과 개수 조회 요청: {}", request);
            
    //         String keyword = request.get("keyword");
    //         String questionType = request.get("questionType");
    //         String memberId = request.get("memberId");
    //         String subDetailId = request.get("subDetailId");
    //         String educationId = request.get("educationId");
            
    //         long count = questionBankService.getRealTimeSearchCount(
    //                 keyword, questionType, memberId, subDetailId, educationId);
            
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("실시간 검색 결과 개수 조회가 완료되었습니다.", count));
            
    //     } catch (Exception e) {
    //         log.error("실시간 검색 결과 개수 조회 중 오류 발생: {}", e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(ResponseDTO.createErrorResponse(400, "실시간 검색 결과 개수 조회 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }
    
    
}