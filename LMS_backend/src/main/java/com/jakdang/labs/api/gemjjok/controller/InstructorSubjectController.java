package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.api.lnuyasha.dto.SubjectInfoDTO;
import com.jakdang.labs.api.lnuyasha.service.KySubjectService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/instructor")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5176"})
public class InstructorSubjectController {
    
    @Autowired
    private KySubjectService kySubjectService;
    
    @Autowired
    private EducationId educationId;

    // API 상태 확인
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> getHealth() {
        try {
            log.info("=== API 헬스체크 요청 ===");
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("status", "UP");
            healthInfo.put("service", "InstructorSubjectController");
            healthInfo.put("timestamp", System.currentTimeMillis());
            
            // 의존성 서비스 상태 확인
            try {
                // KySubjectService 상태 확인 (간단한 테스트)
                healthInfo.put("kySubjectService", "UP");
            } catch (Exception e) {
                healthInfo.put("kySubjectService", "DOWN");
                log.warn("KySubjectService 상태 확인 실패: {}", e.getMessage());
            }
            
            try {
                // EducationId 컴포넌트 상태 확인
                healthInfo.put("educationId", "UP");
            } catch (Exception e) {
                healthInfo.put("educationId", "DOWN");
                log.warn("EducationId 컴포넌트 상태 확인 실패: {}", e.getMessage());
            }
            
            log.info("=== API 헬스체크 완료 ===");
            return ResponseDTO.createSuccessResponse("API 상태 확인 완료", healthInfo);
        } catch (Exception e) {
            log.error("API 헬스체크 중 오류 발생: {}", e.getMessage(), e);
            return ResponseDTO.createErrorResponse(500, "API 상태 확인 실패: " + e.getMessage());
        }
    }

    // 전체 과목 목록 조회
    @GetMapping("/subjects/all")
    public ResponseDTO<List<SubjectInfoDTO>> getAllSubjects(@RequestParam(required = false) String userId) {
        try {
            log.info("=== 전체 과목 목록 조회 요청 시작 ===");
            log.info("요청 파라미터 - userId: {}", userId);
            
            String targetUserId = userId;
            
            // userId 파라미터가 없으면 JWT에서 추출
            if (targetUserId == null || targetUserId.trim().isEmpty()) {
                log.info("userId 파라미터가 없어서 JWT에서 추출 시도");
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                    targetUserId = userDetails.getUserId();
                    log.info("JWT에서 userId 추출 성공: {}", targetUserId);
                } else {
                    log.warn("JWT에서 userId 추출 실패 - 인증 정보 없음");
                    return ResponseDTO.createErrorResponse(400, "인증된 사용자 정보를 찾을 수 없습니다.");
                }
            } else {
                log.info("파라미터로 전달된 userId 사용: {}", targetUserId);
            }
            
            // EducationId 컴포넌트를 사용해서 educationId 조회 
            log.info("educationId 조회 시도 - userId: {}", targetUserId);
            Optional<String> educationIdOpt = educationId.getEducationIdByUserId(targetUserId);
            if (educationIdOpt.isEmpty()) {
                log.error("educationId 조회 실패 - userId: {}", targetUserId);
                return ResponseDTO.createErrorResponse(400, "사용자의 educationId를 찾을 수 없습니다.");
            }
            
            String userEducationId = educationIdOpt.get();
            log.info("educationId 조회 성공: {}", userEducationId);
            
            // 과목 목록 조회
            log.info("과목 목록 조회 시도 - educationId: {}", userEducationId);
            List<SubjectInfoDTO> subjects = kySubjectService.getSubjectsByEducationId(userEducationId);
            log.info("과목 목록 조회 완료: {}개", subjects.size());
            
            // 응답 데이터 로깅 (개발용)
            if (log.isDebugEnabled()) {
                subjects.forEach(subject -> log.debug("과목 정보: {}", subject));
            }
    
            log.info("=== 전체 과목 목록 조회 요청 완료 ===");
            return ResponseDTO.createSuccessResponse("과목 목록 조회가 완료되었습니다.", subjects);
        } catch (Exception e) {
            log.error("과목 목록 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseDTO.createErrorResponse(500, "과목 목록 조회 실패: " + e.getMessage());
        }
    }

    // 전체 세부과목 목록 조회
    @GetMapping("/subject-details/all")
    public ResponseDTO<List<SubjectInfoDTO>> getAllSubjectDetails(@RequestParam(required = false) String userId) {
        try {
            log.info("=== 전체 세부과목 목록 조회 요청 시작 ===");
            log.info("요청 파라미터 - userId: {}", userId);
            
            String targetUserId = userId;
            
            // userId 파라미터가 없으면 JWT에서 추출
            if (targetUserId == null || targetUserId.trim().isEmpty()) {
                log.info("userId 파라미터가 없어서 JWT에서 추출 시도");
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                    targetUserId = userDetails.getUserId();
                    log.info("JWT에서 userId 추출 성공: {}", targetUserId);
                } else {
                    log.warn("JWT에서 userId 추출 실패 - 인증 정보 없음");
                    return ResponseDTO.createErrorResponse(400, "인증된 사용자 정보를 찾을 수 없습니다.");
                }
            } else {
                log.info("파라미터로 전달된 userId 사용: {}", targetUserId);
            }
            
            // EducationId 컴포넌트를 사용해서 educationId 조회 
            log.info("educationId 조회 시도 - userId: {}", targetUserId);
            Optional<String> educationIdOpt = educationId.getEducationIdByUserId(targetUserId);
            if (educationIdOpt.isEmpty()) {
                log.error("educationId 조회 실패 - userId: {}", targetUserId);
                return ResponseDTO.createErrorResponse(400, "사용자의 educationId를 찾을 수 없습니다.");
            }
            
            String userEducationId = educationIdOpt.get();
            log.info("educationId 조회 성공: {}", userEducationId);
            
            // 세부과목 목록 조회 (과목과 동일한 서비스 사용)
            log.info("세부과목 목록 조회 시도 - educationId: {}", userEducationId);
            List<SubjectInfoDTO> subjectDetails = kySubjectService.getSubjectsByEducationId(userEducationId);
            log.info("세부과목 목록 조회 완료: {}개", subjectDetails.size());
            
            // 응답 데이터 로깅 (개발용)
            if (log.isDebugEnabled()) {
                subjectDetails.forEach(detail -> log.debug("세부과목 정보: {}", detail));
            }
    
            log.info("=== 전체 세부과목 목록 조회 요청 완료 ===");
            return ResponseDTO.createSuccessResponse("세부과목 목록 조회가 완료되었습니다.", subjectDetails);
        } catch (Exception e) {
            log.error("세부과목 목록 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseDTO.createErrorResponse(500, "세부과목 목록 조회 실패: " + e.getMessage());
        }
    }
} 