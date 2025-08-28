package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.entity.LecturePlanEntity;
import com.jakdang.labs.api.gemjjok.DTO.LecturePlanRequestDTO;
import com.jakdang.labs.api.gemjjok.DTO.LecturePlanResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.LecturePlanListResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.WeeklyPlanDTO;
import com.jakdang.labs.api.gemjjok.repository.LecturePlanRepository;
import com.jakdang.labs.api.gemjjok.service.WeeklyPlanService;
import com.jakdang.labs.api.auth.repository.UserRepository;
import com.jakdang.labs.api.gemjjok.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LecturePlanService {

    @Autowired
    private LecturePlanRepository lecturePlanRepository;
    
    @Autowired
    private WeeklyPlanService weeklyPlanService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    @Qualifier("gemjjokMemberRepository")
    private MemberRepository memberRepository;

    // 강의 계획서 등록
    public LecturePlanResponseDTO createLecturePlan(LecturePlanRequestDTO requestDTO) {
        // 중복 확인
        if (lecturePlanRepository.existsByCourseIdAndPlanTitleAndPlanActive(
                requestDTO.getCourseId(), requestDTO.getPlanTitle(), 0)) {
            throw new RuntimeException("이미 존재하는 강의 계획서 제목입니다.");
        }

        LecturePlanEntity entity = new LecturePlanEntity();
        
        // planId 생성 (UUID 사용)
        entity.setPlanId(UUID.randomUUID().toString());
        entity.setCourseId(requestDTO.getCourseId());
        entity.setPlanTitle(requestDTO.getPlanTitle());
        entity.setPlanContent(requestDTO.getPlanContent());
        entity.setCourseGoal(requestDTO.getCourseGoal());
        entity.setLearningMethod(requestDTO.getLearningMethod());
        entity.setEvaluationMethod(requestDTO.getEvaluationMethod());
        entity.setTextbook(requestDTO.getTextbook());
        entity.setWeekCount(requestDTO.getWeekCount() != null ? requestDTO.getWeekCount() : 15);
        entity.setAssignmentPolicy(requestDTO.getAssignmentPolicy());
        entity.setLatePolicy(requestDTO.getLatePolicy());
        entity.setEtcNote(requestDTO.getEtcNote());
        entity.setIsLocked(requestDTO.getIsLocked() != null ? requestDTO.getIsLocked() : (byte) 0);
        entity.setPlanActive(0); // 0: 활성, 1: 삭제

        LecturePlanEntity savedEntity = lecturePlanRepository.save(entity);
        return convertToResponseDTO(savedEntity);
    }

    // 강의 계획서 수정
    public LecturePlanResponseDTO updateLecturePlan(String planId, LecturePlanRequestDTO requestDTO) {
        Optional<LecturePlanEntity> optionalEntity = lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0);
        
        if (optionalEntity.isEmpty()) {
            throw new RuntimeException("강의 계획서를 찾을 수 없습니다.");
        }

        LecturePlanEntity entity = optionalEntity.get();
        
        // 잠금 상태 확인
        if (entity.getIsLocked() != null && entity.getIsLocked() == 1) {
            throw new RuntimeException("잠긴 강의 계획서는 수정할 수 없습니다.");
        }

        // 제목 변경 시 중복 확인
        if (!entity.getPlanTitle().equals(requestDTO.getPlanTitle()) &&
            lecturePlanRepository.existsByCourseIdAndPlanTitleAndPlanActive(
                requestDTO.getCourseId(), requestDTO.getPlanTitle(), 0)) {
            throw new RuntimeException("이미 존재하는 강의 계획서 제목입니다.");
        }

        // 엔티티 업데이트
        entity.setPlanTitle(requestDTO.getPlanTitle());
        entity.setPlanContent(requestDTO.getPlanContent());
        entity.setCourseGoal(requestDTO.getCourseGoal());
        entity.setLearningMethod(requestDTO.getLearningMethod());
        entity.setEvaluationMethod(requestDTO.getEvaluationMethod());
        entity.setTextbook(requestDTO.getTextbook());
        entity.setWeekCount(requestDTO.getWeekCount() != null ? requestDTO.getWeekCount() : 15);
        entity.setAssignmentPolicy(requestDTO.getAssignmentPolicy());
        entity.setLatePolicy(requestDTO.getLatePolicy());
        entity.setIsLocked(requestDTO.getIsLocked() != null ? requestDTO.getIsLocked() : (byte) 0);

        LecturePlanEntity savedEntity = lecturePlanRepository.save(entity);
        return convertToResponseDTO(savedEntity);
    }

    // 강의 계획서 삭제 (논리적 삭제)
    public void deleteLecturePlan(String planId) {
        Optional<LecturePlanEntity> optionalEntity = lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0);
        
        if (optionalEntity.isEmpty()) {
            throw new RuntimeException("강의 계획서를 찾을 수 없습니다.");
        }

        LecturePlanEntity entity = optionalEntity.get();
        entity.setPlanActive(1); // 삭제 상태로 변경
        lecturePlanRepository.save(entity);
    }

    // 강의 계획서 상세 조회
    public LecturePlanResponseDTO getLecturePlan(String planId) {
        Optional<LecturePlanEntity> optionalEntity = lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0);
        
        if (optionalEntity.isEmpty()) {
            throw new RuntimeException("강의 계획서를 찾을 수 없습니다.");
        }

        return convertToResponseDTO(optionalEntity.get());
    }

    // 강의 계획서 등록 (주차별 계획 포함)
    public LecturePlanResponseDTO createLecturePlanWithWeeklyPlan(LecturePlanRequestDTO requestDTO, List<WeeklyPlanDTO> weeklyPlanDTOs) {
        // 강의 계획서 등록
        LecturePlanResponseDTO response = createLecturePlan(requestDTO);
        
        // 주차별 계획이 있으면 저장
        if (weeklyPlanDTOs != null && !weeklyPlanDTOs.isEmpty()) {
            weeklyPlanService.saveWeeklyPlans(response.getPlanId(), weeklyPlanDTOs);
        }
        
        return response;
    }

    // 강의 계획서 수정 (주차별 계획 포함)
    public LecturePlanResponseDTO updateLecturePlanWithWeeklyPlan(String planId, LecturePlanRequestDTO requestDTO, List<WeeklyPlanDTO> weeklyPlanDTOs) {
        // 강의 계획서 수정
        LecturePlanResponseDTO response = updateLecturePlan(planId, requestDTO);
        
        // 주차별 계획이 있으면 저장
        if (weeklyPlanDTOs != null && !weeklyPlanDTOs.isEmpty()) {
            weeklyPlanService.saveWeeklyPlans(planId, weeklyPlanDTOs);
        }
        
        return response;
    }

    // 강의 계획서 삭제 (주차별 계획 포함)
    public void deleteLecturePlanWithWeeklyPlan(String planId) {
        // 주차별 계획 삭제
        weeklyPlanService.deleteWeeklyPlansByPlanId(planId);
        
        // 강의 계획서 삭제
        deleteLecturePlan(planId);
    }

    // 강의 계획서 목록 조회 (전체)
    public List<LecturePlanListResponseDTO> getAllLecturePlans() {
        List<LecturePlanEntity> entities = lecturePlanRepository.findByPlanActiveOrderByCreatedAtDesc(0);
        return entities.stream()
                .map(this::convertToListResponseDTO)
                .collect(Collectors.toList());
    }

    // 특정 과정의 강의 계획서 목록 조회
    public List<LecturePlanListResponseDTO> getLecturePlansByCourseId(String courseId) {
        List<LecturePlanEntity> entities = lecturePlanRepository.findByCourseIdAndPlanActiveOrderByCreatedAtDesc(courseId, 0);
        return entities.stream()
                .map(this::convertToListResponseDTO)
                .collect(Collectors.toList());
    }

    // 강의 계획서 제목으로 검색
    public List<LecturePlanListResponseDTO> searchLecturePlansByTitle(String keyword) {
        List<LecturePlanEntity> entities = lecturePlanRepository.findByPlanTitleContainingAndPlanActive(keyword, 0);
        return entities.stream()
                .map(this::convertToListResponseDTO)
                .collect(Collectors.toList());
    }

    // 강의 계획서 잠금/해제
    public LecturePlanResponseDTO toggleLockLecturePlan(String planId) {
        Optional<LecturePlanEntity> optionalEntity = lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0);
        
        if (optionalEntity.isEmpty()) {
            throw new RuntimeException("강의 계획서를 찾을 수 없습니다.");
        }

        LecturePlanEntity entity = optionalEntity.get();
        entity.setIsLocked(entity.getIsLocked() == 1 ? (byte) 0 : (byte) 1);
        lecturePlanRepository.save(entity);
        return convertToResponseDTO(entity);
    }

    // 특정 사용자의 강의계획서 목록 조회 (users.id → email → member → courseId → lectureplan)
    public List<LecturePlanListResponseDTO> getLecturePlansByUserId(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        List<String> courseIds = memberRepository.findByMemberEmailIgnoreCase(email)
            .stream()
            .map(m -> m.getCourseId())
            .filter(cid -> cid != null)
            .collect(Collectors.toList());
        if (courseIds.isEmpty()) return List.of();
        List<LecturePlanEntity> entities = courseIds.stream()
            .flatMap(cid -> lecturePlanRepository.findByCourseIdAndPlanActiveOrderByCreatedAtDesc(cid, 0).stream())
            .collect(Collectors.toList());
        return entities.stream().map(this::convertToListResponseDTO).collect(Collectors.toList());
    }

    // planId 존재 여부 확인
    public boolean existsByPlanId(String planId) {
        return lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0).isPresent();
    }

    // Entity -> ResponseDTO 변환
    private LecturePlanResponseDTO convertToResponseDTO(LecturePlanEntity entity) {
        LecturePlanResponseDTO dto = new LecturePlanResponseDTO();
        dto.setPlanId(entity.getPlanId());
        dto.setCourseId(entity.getCourseId());
        dto.setPlanTitle(entity.getPlanTitle());
        dto.setPlanContent(entity.getPlanContent());
        dto.setCourseGoal(entity.getCourseGoal());
        dto.setLearningMethod(entity.getLearningMethod());
        dto.setEvaluationMethod(entity.getEvaluationMethod());
        dto.setTextbook(entity.getTextbook());
        dto.setWeekCount(entity.getWeekCount());
        dto.setAssignmentPolicy(entity.getAssignmentPolicy());
        dto.setLatePolicy(entity.getLatePolicy());
        dto.setEtcNote(entity.getEtcNote());
        dto.setIsLocked(entity.getIsLocked());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setPlanActive(entity.getPlanActive());
        return dto;
    }

    // Entity -> ListResponseDTO 변환
    private LecturePlanListResponseDTO convertToListResponseDTO(LecturePlanEntity entity) {
        LecturePlanListResponseDTO dto = new LecturePlanListResponseDTO();
        dto.setPlanId(entity.getPlanId());
        dto.setCourseId(entity.getCourseId());
        dto.setPlanTitle(entity.getPlanTitle());
        dto.setCourseGoal(entity.getCourseGoal());
        dto.setWeekCount(entity.getWeekCount());
        dto.setIsLocked(entity.getIsLocked());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setPlanActive(entity.getPlanActive());
        return dto;
    }
} 