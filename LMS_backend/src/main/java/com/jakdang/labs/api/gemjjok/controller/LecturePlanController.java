package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.gemjjok.DTO.LecturePlanRequestDTO;
import com.jakdang.labs.api.gemjjok.DTO.LecturePlanResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.LecturePlanListResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.WeeklyPlanDTO;
import com.jakdang.labs.api.gemjjok.DTO.WeeklyPlanRequest;
import com.jakdang.labs.api.gemjjok.service.LecturePlanService;
import com.jakdang.labs.api.gemjjok.service.WeeklyPlanService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instructor/lectureplan")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5176"})
public class LecturePlanController {

    private static final Logger log = LoggerFactory.getLogger(LecturePlanController.class);

    @Autowired
    private LecturePlanService lecturePlanService;

    @Autowired
    private WeeklyPlanService weeklyPlanService;
    
    @Autowired
    private ObjectMapper objectMapper;

    // planId로 강의 계획서 상세 조회
    @GetMapping("/plan/{planId}")
    public ResponseEntity<ResponseDTO<?>> getLecturePlan(@PathVariable("planId") String planId) {
        try {
            LecturePlanResponseDTO response = lecturePlanService.getLecturePlan(planId);
            return ResponseEntity.ok().body(ResponseDTO.createSuccessResponse("강의 계획서 상세 조회 성공", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        }
    }

    // courseId로 강의 계획서(planId) 조회 - 프론트엔드 요청에 맞게 수정
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ResponseDTO<?>> getLecturePlanByCourseId(@PathVariable("courseId") String courseId) {
        log.info("=== 강의 계획서 조회 요청 시작 ===");
        log.info("강의 계획서 조회 요청 - courseId: {}", courseId);
        
        try {
            // 1차: 그대로 조회
            log.info("1차 조회 시도 - courseId: {}", courseId);
            List<LecturePlanListResponseDTO> plans = lecturePlanService.getLecturePlansByCourseId(courseId);
            log.info("1차 조회 결과 - courseId: {}, plans.size: {}", courseId, plans != null ? plans.size() : 0);
            
            // 2차: 숫자라면 문자열로 변환해서도 조회 시도
            if ((plans == null || plans.isEmpty()) && courseId != null) {
                try {
                    Integer intId = Integer.valueOf(courseId);
                    log.info("2차 조회 시도 - intId: {}", intId);
                    plans = lecturePlanService.getLecturePlansByCourseId(intId.toString());
                    log.info("2차 조회 결과 - intId: {}, plans.size: {}", intId, plans != null ? plans.size() : 0);
                } catch (NumberFormatException e) {
                    log.warn("courseId를 숫자로 변환할 수 없음: {}", courseId);
                }
            }
            
            if (plans == null || plans.isEmpty()) {
                log.warn("강의 계획서가 없습니다 - courseId: {}", courseId);
                return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "강의 계획서가 없습니다. courseId: " + courseId));
            }
            
            String planId = plans.get(0).getPlanId();
            log.info("첫 번째 강의 계획서 선택 - planId: {}", planId);
            
            log.info("강의 계획서 상세 조회 시도 - planId: {}", planId);
            LecturePlanResponseDTO response = lecturePlanService.getLecturePlan(planId);
            log.info("강의 계획서 조회 성공 - courseId: {}, planId: {}", courseId, planId);
            log.info("=== 강의 계획서 조회 요청 완료 ===");
            return ResponseEntity.ok().body(ResponseDTO.createSuccessResponse("강의 계획서 조회 성공", response));
        } catch (RuntimeException e) {
            log.error("강의 계획서 조회 중 RuntimeException 발생 - courseId: {}, error: {}", courseId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseDTO.createErrorResponse(400, "강의 계획서 조회 실패: " + e.getMessage()));
        } catch (Exception e) {
            log.error("강의 계획서 조회 중 예상치 못한 오류 발생 - courseId: {}, error: {}", courseId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "서버 오류: " + e.getMessage()));
        }
    }

    // 프론트 요청에 맞는 새로운 엔드포인트: /api/instructor/lectureplan/direct/{courseId}
    @GetMapping("/direct/{courseId}")
    public ResponseEntity<ResponseDTO<?>> getLecturePlanByCourseIdDirect(@PathVariable("courseId") String courseId) {
        try {
            log.info("강의 계획서 직접 조회 요청 - courseId: {}", courseId);
            
            // 1차: 그대로 조회
            List<LecturePlanListResponseDTO> plans = lecturePlanService.getLecturePlansByCourseId(courseId);
            // 2차: 숫자라면 문자열로 변환해서도 조회 시도
            if ((plans == null || plans.isEmpty()) && courseId != null) {
                try {
                    Integer intId = Integer.valueOf(courseId);
                    plans = lecturePlanService.getLecturePlansByCourseId(intId.toString());
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
            if (plans == null || plans.isEmpty()) {
                log.warn("강의 계획서가 없습니다 - courseId: {}", courseId);
                return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "강의 계획서가 없습니다."));
            }
            String planId = plans.get(0).getPlanId();
            LecturePlanResponseDTO response = lecturePlanService.getLecturePlan(planId);
            log.info("강의 계획서 직접 조회 성공 - courseId: {}, planId: {}", courseId, planId);
            return ResponseEntity.ok().body(ResponseDTO.createSuccessResponse("강의 계획서 조회 성공", response));
        } catch (Exception e) {
            log.error("강의 계획서 직접 조회 실패 - courseId: {}, error: {}", courseId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "서버 오류: " + e.getMessage()));
        }
    }

    // 주차별 계획 조회
    @GetMapping("/{planId}/weeklyplan")
    public ResponseEntity<ResponseDTO<?>> getWeeklyPlan(@PathVariable("planId") String planId) {
        log.info("=== 주차별 계획 조회 요청 === planId: {}", planId);
        
        try {
            // planId 유효성 검사
            if (planId == null || planId.trim().isEmpty()) {
                log.error("주차별 계획 조회 실패: planId가 비어있습니다.");
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "planId는 필수입니다."));
            }
            
            List<WeeklyPlanDTO> weeklyPlan = weeklyPlanService.getWeeklyPlansByPlanId(planId);
            log.info("주차별 계획 조회 성공: planId: {}, count: {}", planId, weeklyPlan.size());
            
            return ResponseEntity.ok()
                .body(ResponseDTO.createSuccessResponse("주차별 계획 조회 성공", weeklyPlan));
                
        } catch (RuntimeException e) {
            log.error("주차별 계획 조회 실패: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("주차별 계획 조회 중 예상치 못한 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ResponseDTO.createErrorResponse(500, "주차별 계획 조회 실패: " + e.getMessage()));
        }
    }

    // 주차별 계획 등록/수정
    @PostMapping("/{planId}/weeklyplan")
    public ResponseEntity<ResponseDTO<?>> saveWeeklyPlan(
        @PathVariable("planId") String planId,
        @RequestBody Object requestBody
    ) {
        log.info("=== 주차별 계획 등록/수정 요청 시작 === planId: {}", planId);
        
        List<WeeklyPlanRequest> weeklyPlanRequests;
        
        try {
            log.info("요청 데이터 타입: {}", requestBody.getClass().getSimpleName());
            log.info("요청 데이터 내용: {}", requestBody);
            
            // 단일 객체인지 배열인지 확인하여 처리
            if (requestBody instanceof List) {
                log.info("요청 데이터가 List 타입입니다. 크기: {}", ((List<?>) requestBody).size());
                weeklyPlanRequests = objectMapper.convertValue(requestBody, 
                    new TypeReference<List<WeeklyPlanRequest>>() {});
            } else {
                log.info("요청 데이터가 단일 객체 타입입니다.");
                // 단일 객체인 경우 배열로 변환
                WeeklyPlanRequest singleRequest = objectMapper.convertValue(requestBody, WeeklyPlanRequest.class);
                weeklyPlanRequests = List.of(singleRequest);
            }
            
            log.info("파싱된 WeeklyPlanRequest 개수: {}", weeklyPlanRequests.size());
            for (int i = 0; i < weeklyPlanRequests.size(); i++) {
                WeeklyPlanRequest req = weeklyPlanRequests.get(i);
                log.info("WeeklyPlanRequest {}: weekNumber={} (원본: {}), weekTitle='{}' (원본: '{}'), subjectIds={}, subDetailIds={}", 
                    i + 1, req.getWeekNumber(), req.getOriginalWeekNumber(), 
                    req.getWeekTitle(), req.getOriginalWeekTitle(),
                    req.getSubjectIds(), req.getSubDetailIds());
            }
        } catch (Exception e) {
            log.error("요청 데이터 파싱 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseDTO.createErrorResponse(400, "요청 데이터 형식이 올바르지 않습니다."));
        }
        
        try {
            // planId 유효성 검사
            if (planId == null || planId.trim().isEmpty()) {
                log.error("주차별 계획 등록/수정 실패: planId가 비어있습니다.");
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "planId는 필수입니다."));
            }
            log.info("planId 유효성 검사 통과: {}", planId);
            
            // 요청 데이터 유효성 검사
            if (weeklyPlanRequests == null || weeklyPlanRequests.isEmpty()) {
                log.error("주차별 계획 등록/수정 실패: 주차별 계획 데이터가 없습니다.");
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "주차별 계획 데이터가 없습니다."));
            }
            log.info("주차별 계획 데이터 유효성 검사 통과: {}개 항목", weeklyPlanRequests.size());
            
            // WeeklyPlanRequest를 WeeklyPlanDTO로 변환
            List<WeeklyPlanDTO> weeklyPlanData = weeklyPlanRequests.stream()
                .map(request -> convertToWeeklyPlanDTO(request, planId))
                .collect(Collectors.toList());
            
                         // 각 WeeklyPlanDTO의 내용을 로깅
             for (int i = 0; i < weeklyPlanData.size(); i++) {
                 WeeklyPlanDTO dto = weeklyPlanData.get(i);
                 log.info("주차별 계획 {}: weekNumber={} (타입: {}), weekTitle='{}' (타입: {}), weekContent={}, subjectId={}, subDetailId={}", 
                     i + 1, dto.getWeekNumber(), 
                     dto.getWeekNumber() != null ? dto.getWeekNumber().getClass().getSimpleName() : "null",
                     dto.getWeekTitle(), 
                     dto.getWeekTitle() != null ? dto.getWeekTitle().getClass().getSimpleName() : "null",
                     dto.getWeekContent() != null ? dto.getWeekContent().substring(0, Math.min(50, dto.getWeekContent().length())) + "..." : "null",
                     dto.getSubjectId(), dto.getSubDetailId());
             }
            
            log.info("WeeklyPlanService.saveWeeklyPlans 호출 시작...");
            List<WeeklyPlanDTO> savedWeeklyPlan = weeklyPlanService.saveWeeklyPlans(planId, weeklyPlanData);
            log.info("주차별 계획 등록/수정 성공: planId: {}, savedCount: {}", planId, savedWeeklyPlan.size());
            
            return ResponseEntity.ok()
                .body(ResponseDTO.createSuccessResponse("주차별 계획이 성공적으로 저장되었습니다.", savedWeeklyPlan));
                
        } catch (RuntimeException e) {
            log.error("주차별 계획 등록/수정 실패: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("주차별 계획 등록/수정 중 예상치 못한 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ResponseDTO.createErrorResponse(500, "주차별 계획 등록/수정 실패: " + e.getMessage()));
        }
    }

    // 주차별 계획 삭제
    @DeleteMapping("/{planId}/weeklyplan")
    public ResponseEntity<ResponseDTO<?>> deleteWeeklyPlan(@PathVariable("planId") String planId) {
        log.info("=== 주차별 계획 삭제 요청 === planId: {}", planId);
        
        try {
            // planId 유효성 검사
            if (planId == null || planId.trim().isEmpty()) {
                log.error("주차별 계획 삭제 실패: planId가 비어있습니다.");
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "planId는 필수입니다."));
            }
            
            weeklyPlanService.deleteWeeklyPlansByPlanId(planId);
            log.info("주차별 계획 삭제 성공: planId: {}", planId);
            
            return ResponseEntity.ok()
                .body(ResponseDTO.createSuccessResponse("주차별 계획이 성공적으로 삭제되었습니다.", null));
                
        } catch (RuntimeException e) {
            log.error("주차별 계획 삭제 실패: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("주차별 계획 삭제 중 예상치 못한 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ResponseDTO.createErrorResponse(500, "주차별 계획 삭제 실패: " + e.getMessage()));
        }
    }

    // 강의 계획서 등록
    @PostMapping
    public ResponseEntity<ResponseDTO<?>> createLecturePlan(@RequestBody LecturePlanRequestDTO requestDTO) {
        try {
            log.info("=== 강의 계획서 등록 요청 시작 ===");
            
            // 1. 강의 계획서 저장
            LecturePlanResponseDTO response = lecturePlanService.createLecturePlan(requestDTO);

            // 2. 주차별 계획 저장
            int weeklyPlanCount = 0;
            if (requestDTO.getWeeklyPlan() != null && !requestDTO.getWeeklyPlan().isEmpty()) {
                // WeeklyPlanRequest를 WeeklyPlanDTO로 변환
                List<WeeklyPlanDTO> weeklyPlanDTOs = requestDTO.getWeeklyPlan().stream()
                    .map(request -> convertToWeeklyPlanDTO(request, response.getPlanId()))
                    .collect(Collectors.toList());
                
                weeklyPlanService.saveWeeklyPlans(response.getPlanId(), weeklyPlanDTOs);
                weeklyPlanCount = requestDTO.getWeeklyPlan().size();
            }

            log.info("=== 강의 계획서 등록 완료 === planId: {}, weeklyPlanCount: {}", response.getPlanId(), weeklyPlanCount);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseDTO.createSuccessResponse("강의 계획서 및 주차별 계획이 성공적으로 등록되었습니다. (주차별 계획 " + weeklyPlanCount + "개)", response));
        } catch (RuntimeException e) {
            log.error("강의 계획서 등록 실패: error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("강의 계획서 등록 중 예상치 못한 오류 발생: error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.createErrorResponse(500, "강의 계획서 등록 실패: " + e.getMessage()));
        }
    }

    // 강의 계획서 수정
    @PutMapping("/{planId}")
    public ResponseEntity<ResponseDTO<?>> updateLecturePlan(
            @PathVariable("planId") String planId,
            @RequestBody LecturePlanRequestDTO requestDTO) {
        try {
            log.info("=== 강의 계획서 수정 요청 시작 === planId: {}", planId);
            
            // 1. 강의 계획서 수정
            LecturePlanResponseDTO response = lecturePlanService.updateLecturePlan(planId, requestDTO);

            // 2. 기존 주차별 계획 삭제 후 새로 저장
            int weeklyPlanCount = 0;
            weeklyPlanService.deleteWeeklyPlansByPlanId(planId);
            if (requestDTO.getWeeklyPlan() != null && !requestDTO.getWeeklyPlan().isEmpty()) {
                // WeeklyPlanRequest를 WeeklyPlanDTO로 변환
                List<WeeklyPlanDTO> weeklyPlanDTOs = requestDTO.getWeeklyPlan().stream()
                    .map(request -> convertToWeeklyPlanDTO(request, planId))
                    .collect(Collectors.toList());
                
                weeklyPlanService.saveWeeklyPlans(planId, weeklyPlanDTOs);
                weeklyPlanCount = requestDTO.getWeeklyPlan().size();
            }

            log.info("=== 강의 계획서 수정 완료 === planId: {}, weeklyPlanCount: {}", planId, weeklyPlanCount);
            return ResponseEntity.ok()
                    .body(ResponseDTO.createSuccessResponse("강의 계획서 및 주차별 계획이 성공적으로 수정되었습니다. (주차별 계획 " + weeklyPlanCount + "개)", response));
        } catch (RuntimeException e) {
            log.error("강의 계획서 수정 실패: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("강의 계획서 수정 중 예상치 못한 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ResponseDTO.createErrorResponse(500, "강의 계획서 수정 실패: " + e.getMessage()));
        }
    }

    // 강의 계획서 삭제
    @DeleteMapping("/{planId}")
    public ResponseEntity<ResponseDTO<?>> deleteLecturePlan(@PathVariable("planId") String planId) {
        try {
            // 1. 주차별 계획 삭제
            weeklyPlanService.deleteWeeklyPlansByPlanId(planId);
            // 2. 강의 계획서 삭제
            lecturePlanService.deleteLecturePlan(planId);
            return ResponseEntity.ok()
                    .body(ResponseDTO.createSuccessResponse("강의 계획서 및 주차별 계획이 성공적으로 삭제되었습니다.", "삭제 완료"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        }
    }

    // 전체 강의 계획서 목록 조회
    @GetMapping
    public ResponseEntity<ResponseDTO<?>> getAllLecturePlans() {
        try {
            List<LecturePlanListResponseDTO> response = lecturePlanService.getAllLecturePlans();
            return ResponseEntity.ok()
                    .body(ResponseDTO.createSuccessResponse("강의 계획서 목록 조회 성공", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        }
    }

    // 강의 계획서 제목으로 검색
    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<?>> searchLecturePlansByTitle(@RequestParam String keyword) {
        try {
            List<LecturePlanListResponseDTO> response = lecturePlanService.searchLecturePlansByTitle(keyword);
            return ResponseEntity.ok()
                    .body(ResponseDTO.createSuccessResponse("강의 계획서 검색 성공", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        }
    }

    // 강의 계획서 잠금/해제
    @PatchMapping("/{planId}/toggle-lock")
    public ResponseEntity<ResponseDTO<?>> toggleLockLecturePlan(@PathVariable("planId") String planId) {
        try {
            LecturePlanResponseDTO response = lecturePlanService.toggleLockLecturePlan(planId);
            String message = response.getIsLocked() == 1 ? "강의 계획서가 잠금 처리되었습니다." : "강의 계획서 잠금이 해제되었습니다.";
            return ResponseEntity.ok().body(ResponseDTO.createSuccessResponse(message, response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.createErrorResponse(400, e.getMessage()));
        }
    }

    // JWT에서 사용자 정보 추출
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
    }
    
    // WeeklyPlanRequest를 WeeklyPlanDTO로 변환하는 메서드
    private WeeklyPlanDTO convertToWeeklyPlanDTO(WeeklyPlanRequest request, String planId) {
        log.info("=== WeeklyPlanRequest 변환 시작 ===");
        log.info("원본 요청 데이터: {}", request);
        log.info("요청 데이터 상세: weekNumber={} (타입: {}), weekTitle='{}' (원본: '{}'), weekContent={}, subjectId={}, subDetailId={}, subjectIds={}, subDetailIds={}", 
            request.getOriginalWeekNumber(), 
            request.getOriginalWeekNumber() != null ? request.getOriginalWeekNumber().getClass().getSimpleName() : "null",
            request.getWeekTitle(), request.getOriginalWeekTitle(),
            request.getWeekContent() != null ? request.getWeekContent().substring(0, Math.min(50, request.getWeekContent().length())) + "..." : "null",
            request.getSubjectId(), request.getSubDetailId(), request.getSubjectIds(), request.getSubDetailIds());
        
        WeeklyPlanDTO dto = new WeeklyPlanDTO();
        dto.setPlanId(planId); // URL 경로변수에서 받은 planId 사용
        
        // weekNumber 설정 (기본값 처리 포함)
        dto.setWeekNumber(request.getWeekNumber());
        
        dto.setWeekTitle(request.getWeekTitle());
        dto.setWeekContent(request.getWeekContent());
        
        // subjectId 처리 (배열이면 첫 번째 값 사용)
        if (request.getSubjectId() != null && !request.getSubjectId().trim().isEmpty()) {
            dto.setSubjectId(request.getSubjectId());
        } else if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            dto.setSubjectId(request.getSubjectIds().get(0));
        } else {
            dto.setSubjectId(null);
        }
        
        // subDetailId 처리 (배열이면 첫 번째 값 사용)
        if (request.getSubDetailId() != null && !request.getSubDetailId().trim().isEmpty()) {
            dto.setSubDetailId(request.getSubDetailId());
        } else if (request.getSubDetailIds() != null && !request.getSubDetailIds().isEmpty()) {
            dto.setSubDetailId(request.getSubDetailIds().get(0));
        } else {
            dto.setSubDetailId(null);
        }
        
        log.info("변환된 DTO: weekNumber={} (타입: {}), weekTitle={}", 
            dto.getWeekNumber(), 
            dto.getWeekNumber() != null ? dto.getWeekNumber().getClass().getSimpleName() : "null",
            dto.getWeekTitle());
        log.info("=== WeeklyPlanRequest 변환 완료 ===");
        
        return dto;
    }

    // 내 강의계획서 목록 조회 (users.id → email → member → courseId → lectureplan)
    @GetMapping("/my")
    public ResponseEntity<ResponseDTO<?>> getMyLecturePlans() {
        CustomUserDetails currentUser = getCurrentUser();
        String userId = currentUser.getUserId();
        List<LecturePlanListResponseDTO> response = lecturePlanService.getLecturePlansByUserId(userId);
        return ResponseEntity.ok().body(ResponseDTO.createSuccessResponse("내 강의계획서 목록 조회 성공", response));
    }
} 