package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.entity.WeeklyPlanEntity;
import com.jakdang.labs.api.gemjjok.DTO.WeeklyPlanDTO;
import com.jakdang.labs.api.gemjjok.repository.WeeklyPlanRepository;
import com.jakdang.labs.api.gemjjok.repository.LecturePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
@Transactional
public class WeeklyPlanService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyPlanService.class);

    @Autowired
    private WeeklyPlanRepository weeklyPlanRepository;
    
    @Autowired
    private LecturePlanRepository lecturePlanRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // 주차별 계획 목록 조회
    public List<WeeklyPlanDTO> getWeeklyPlansByPlanId(String planId) {
        // log.info("=== 주차별 계획 목록 조회 시작 === planId: {}", planId);
        
        try {
            // planId 유효성 검사
            if (planId == null || planId.trim().isEmpty()) {
                // log.error("planId가 비어있습니다.");
                throw new RuntimeException("planId는 필수입니다.");
            }
            // log.info("planId 유효성 검사 통과");
            
            // 강의 계획서 존재 여부 확인
            if (lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0).isEmpty()) {
                // log.error("존재하지 않는 강의 계획서입니다. planId: {}", planId);
                throw new RuntimeException("존재하지 않는 강의 계획서입니다. planId: " + planId);
            }
            // log.info("강의 계획서 존재 여부 확인 통과");
            
            List<WeeklyPlanEntity> entities = weeklyPlanRepository.findByPlanIdOrderByWeekNumberAsc(planId);
            // log.info("주차별 계획 조회 완료: {}개 항목", entities.size());
            
            return entities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // log.error("주차별 계획 목록 조회 중 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            throw new RuntimeException("주차별 계획 목록 조회 실패: " + e.getMessage());
        }
    }

    // 주차별 계획 등록/수정 (기존 데이터 삭제 후 새로 등록)
    public List<WeeklyPlanDTO> saveWeeklyPlans(String planId, List<WeeklyPlanDTO> weeklyPlanDTOs) {
        log.info("=== 주차별 계획 등록/수정 시작 === planId: {}, 입력 데이터 개수: {}", planId, weeklyPlanDTOs.size());
        
        try {
            // planId 유효성 검사
            if (planId == null || planId.trim().isEmpty()) {
                log.error("planId가 비어있습니다.");
                throw new RuntimeException("planId는 필수입니다.");
            }
            log.info("planId 유효성 검사 통과: {}", planId);
            
            // 강의 계획서 존재 여부 확인
            if (lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0).isEmpty()) {
                log.error("존재하지 않는 강의 계획서입니다. planId: {}", planId);
                throw new RuntimeException("존재하지 않는 강의 계획서입니다. planId: " + planId);
            }
            log.info("강의 계획서 존재 여부 확인 통과");
            
            // 입력 데이터 유효성 검사
            if (weeklyPlanDTOs == null || weeklyPlanDTOs.isEmpty()) {
                log.error("주차별 계획 데이터가 없습니다.");
                throw new RuntimeException("주차별 계획 데이터가 없습니다.");
            }
            log.info("입력 데이터 유효성 검사 통과");
            
            // 중복 주차 번호 검사
            List<Integer> weekNumbers = weeklyPlanDTOs.stream()
                    .map(WeeklyPlanDTO::getWeekNumber)
                    .collect(Collectors.toList());
            if (weekNumbers.size() != weekNumbers.stream().distinct().count()) {
                log.error("중복된 주차 번호가 있습니다: {}", weekNumbers);
                throw new RuntimeException("중복된 주차 번호가 있습니다.");
            }
            log.info("중복 주차 번호 검사 통과: {}", weekNumbers);
            
            // 필수 필드 검사
            for (int i = 0; i < weeklyPlanDTOs.size(); i++) {
                WeeklyPlanDTO dto = weeklyPlanDTOs.get(i);
                log.info("주차별 계획 {} 검증: weekNumber={} (타입: {}), weekTitle='{}' (타입: {}, 길이: {}, null여부: {}, 빈문자열여부: {})", 
                    i + 1, dto.getWeekNumber(), 
                    dto.getWeekNumber() != null ? dto.getWeekNumber().getClass().getSimpleName() : "null",
                    dto.getWeekTitle(), 
                    dto.getWeekTitle() != null ? dto.getWeekTitle().getClass().getSimpleName() : "null",
                    dto.getWeekTitle() != null ? dto.getWeekTitle().length() : 0,
                    dto.getWeekTitle() == null,
                    dto.getWeekTitle() != null && dto.getWeekTitle().trim().isEmpty());
                
                if (dto.getWeekNumber() == null) {
                    log.error("주차별 계획 {}에서 주차 번호가 null입니다.", i + 1);
                    throw new RuntimeException("주차 번호는 필수입니다.");
                }
                if (dto.getWeekTitle() == null || dto.getWeekTitle().trim().isEmpty()) {
                    log.error("주차별 계획 {}에서 주차 제목이 비어있습니다. weekTitle='{}', null여부={}, 빈문자열여부={}", 
                        i + 1, dto.getWeekTitle(), dto.getWeekTitle() == null, 
                        dto.getWeekTitle() != null && dto.getWeekTitle().trim().isEmpty());
                    throw new RuntimeException("주차 제목은 필수입니다.");
                }
            }
            log.info("필수 필드 검사 통과");
            
            // 기존 주차별 계획 삭제
            log.info("기존 주차별 계획 삭제 시작...");
            List<WeeklyPlanEntity> existingEntities = weeklyPlanRepository.findByPlanIdOrderByWeekNumberAsc(planId);
            log.info("기존 주차별 계획 조회 완료: {}개 항목", existingEntities.size());
            
            if (!existingEntities.isEmpty()) {
                weeklyPlanRepository.deleteAll(existingEntities);
                entityManager.flush(); // 삭제 작업을 즉시 반영
                log.info("기존 주차별 계획 삭제 완료");
            } else {
                log.info("삭제할 기존 주차별 계획이 없습니다.");
            }
            
            // 새로운 주차별 계획 등록
            log.info("새로운 주차별 계획 등록 시작...");
            List<WeeklyPlanEntity> entities = weeklyPlanDTOs.stream()
                    .map(dto -> convertToEntity(dto, planId))
                    .collect(Collectors.toList());
            log.info("Entity 변환 완료: {}개 항목", entities.size());
            
            List<WeeklyPlanEntity> savedEntities = weeklyPlanRepository.saveAll(entities);
            log.info("새로운 주차별 계획 저장 완료: {}개 항목", savedEntities.size());
            
            List<WeeklyPlanDTO> result = savedEntities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            log.info("DTO 변환 완료: {}개 항목", result.size());
            
            return result;
        } catch (Exception e) {
            log.error("주차별 계획 등록/수정 중 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            throw new RuntimeException("주차별 계획 등록/수정 실패: " + e.getMessage());
        }
    }

    // 주차별 계획 삭제
    public void deleteWeeklyPlansByPlanId(String planId) {
        // log.info("=== 주차별 계획 삭제 시작 === planId: {}", planId);
        
        try {
            // planId 유효성 검사
            if (planId == null || planId.trim().isEmpty()) {
                // log.error("planId가 비어있습니다.");
                throw new RuntimeException("planId는 필수입니다.");
            }
            
            // 강의 계획서 존재 여부 확인
            if (lecturePlanRepository.findByPlanIdAndPlanActive(planId, 0).isEmpty()) {
                // log.error("존재하지 않는 강의 계획서입니다. planId: {}", planId);
                throw new RuntimeException("존재하지 않는 강의 계획서입니다. planId: " + planId);
            }
            
            // 기존 주차별 계획 조회 후 삭제
            List<WeeklyPlanEntity> existingEntities = weeklyPlanRepository.findByPlanIdOrderByWeekNumberAsc(planId);
            // log.info("삭제할 주차별 계획 조회 완료: {}개 항목", existingEntities.size());
            
            if (!existingEntities.isEmpty()) {
                weeklyPlanRepository.deleteAll(existingEntities);
                // log.info("주차별 계획 삭제 완료");
            } else {
                // log.info("삭제할 주차별 계획이 없습니다.");
            }
        } catch (Exception e) {
            // log.error("주차별 계획 삭제 중 오류 발생: planId: {}, error: {}", planId, e.getMessage(), e);
            throw new RuntimeException("주차별 계획 삭제 실패: " + e.getMessage());
        }
    }

    // 주차별 계획 개수 조회
    public long getWeeklyPlanCountByPlanId(String planId) {
        return weeklyPlanRepository.countByPlanId(planId);
    }

    // DTO를 Entity로 변환
    private WeeklyPlanEntity convertToEntity(WeeklyPlanDTO dto, String planId) {
        // log.debug("DTO를 Entity로 변환: weekNumber={}, weekTitle={}", dto.getWeekNumber(), dto.getWeekTitle());
        
        WeeklyPlanEntity entity = new WeeklyPlanEntity();
        
        // weeklyPlanId는 @PrePersist에서 자동 생성되므로 null로 설정
        entity.setWeeklyPlanId(null);
        entity.setPlanId(planId);
        entity.setWeekNumber(dto.getWeekNumber());
        entity.setWeekTitle(dto.getWeekTitle() != null ? dto.getWeekTitle() : "");
        entity.setWeekContent(dto.getWeekContent());
        
        // 단일 값 데이터 설정
        entity.setSubjectId(dto.getSubjectId());
        entity.setSubDetailId(dto.getSubDetailId());
        
        // log.debug("Entity 생성 완료: planId={}, weekNumber={}, weekTitle={}", 
        //     entity.getPlanId(), entity.getWeekNumber(), entity.getWeekTitle());
        
        return entity;
    }

    // Entity를 DTO로 변환
    private WeeklyPlanDTO convertToDTO(WeeklyPlanEntity entity) {
        WeeklyPlanDTO dto = new WeeklyPlanDTO();
        dto.setWeeklyPlanId(entity.getWeeklyPlanId());
        dto.setPlanId(entity.getPlanId());
        dto.setWeekNumber(entity.getWeekNumber());
        dto.setWeekTitle(entity.getWeekTitle());
        dto.setWeekContent(entity.getWeekContent());
        
        // 단일 값 설정
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubDetailId(entity.getSubDetailId());
        
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
} 