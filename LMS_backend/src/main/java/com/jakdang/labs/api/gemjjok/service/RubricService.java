package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.entity.RubricItemEntity;
import com.jakdang.labs.api.gemjjok.DTO.RubricDTO;
import com.jakdang.labs.api.gemjjok.DTO.RubricItemDTO;
import com.jakdang.labs.api.gemjjok.repository.RubricItemRepository;
import com.jakdang.labs.api.gemjjok.repository.AssignmentRepository;
import com.jakdang.labs.api.gemjjok.util.AssignmentUuidMapper;
import com.jakdang.labs.api.auth.repository.UserRepository;
import com.jakdang.labs.api.gemjjok.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.jakdang.labs.entity.AssignmentEntity;

@Service
@RequiredArgsConstructor
public class RubricService {
    
    private final RubricItemRepository rubricItemRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    @Qualifier("gemjjokMemberRepository")
    private final MemberRepository memberRepository;
    
    // 과제별 루브릭 조회
    public RubricDTO getRubricByAssignmentId(String assignmentId) {
        // UUID 패턴 확인
        boolean isUuid = assignmentId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        
        String actualAssignmentId = assignmentId;
        
        if (isUuid) {
            // UUID인 경우, 실제 데이터베이스의 ID로 매핑
            actualAssignmentId = AssignmentUuidMapper.mapUuidToActualId(assignmentId, assignmentRepository);
        }
        
        List<RubricItemEntity> items = rubricItemRepository.findByAssignmentIdOrderByItemOrderAsc(actualAssignmentId);
        if (items.isEmpty()) {
            return null;
        }
        
        return convertToRubricDTO(assignmentId, items);
    }
    
    // 사용자별 루브릭 조회 (users.id → email → member → courseId → assignment → rubric)
    public RubricDTO getRubricByUserIdAndAssignmentId(String userId, String assignmentId) {
        // 1. users.id로 email 조회
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return null;
        
        // 2. memberEmail로 courseId 조회
        List<String> courseIds = memberRepository.findByMemberEmailIgnoreCase(email)
            .stream()
            .map(m -> m.getCourseId())
            .filter(cid -> cid != null)
            .collect(Collectors.toList());
        if (courseIds.isEmpty()) return null;
        
        // 3. assignmentId로 과제 조회하여 courseId 확인
        Optional<com.jakdang.labs.entity.AssignmentEntity> assignmentOpt = assignmentRepository.findByAssignmentId(assignmentId);
        if (assignmentOpt.isEmpty()) return null;
        
        String assignmentCourseId = assignmentOpt.get().getCourseId();
        if (!courseIds.contains(assignmentCourseId)) return null;
        
        // 4. 루브릭 조회
        return getRubricByAssignmentId(assignmentId);
    }
    
    // assignmentId + userId(강사 소유권)로 루브릭 조회
    public RubricDTO getRubricByAssignmentIdAndUserId(String assignmentId, String userId) {
        // 1. users.id → email → member.memberId
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return null;
        String memberId = memberRepository.findAll().stream()
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()))
            .filter(m -> email.equalsIgnoreCase(m.getMemberEmail()))
            .map(m -> m.getMemberId())
            .findFirst().orElse(null);
        if (memberId == null) return null;
        // 2. assignmentId + memberId로 assignment 소유권 확인
        Optional<AssignmentEntity> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty() || !memberId.equals(assignmentOpt.get().getMemberId())) {
            return null; // 소유하지 않은 과제의 루브릭은 조회 불가
        }
        // 3. 루브릭 조회
        return getRubricByAssignmentId(assignmentId);
    }
    
    // 루브릭 생성/수정
    @Transactional
    public RubricDTO saveRubric(String assignmentId, String rubricTitle, List<RubricItemDTO> rubricitem) {
        // 유효성 검사: itemOrder가 null이거나 1 미만인 항목이 있으면 예외 발생
        for (RubricItemDTO item : rubricitem) {
            if (item.getItemOrder() == null || item.getItemOrder() < 1) {
                throw new IllegalArgumentException("루브릭 항목의 itemOrder(채점 기준 순서)는 1 이상의 값이어야 하며, null일 수 없습니다.");
            }
        }
        // 기존 루브릭 아이템들 삭제
        rubricItemRepository.deleteByAssignmentId(assignmentId);
        // 새로운 루브릭 아이템들 저장
        List<RubricItemEntity> savedItems = saveRubricItems(assignmentId, rubricitem);
        return convertToRubricDTO(assignmentId, savedItems);
    }
    
    // 루브릭 삭제
    @Transactional
    public void deleteRubric(String assignmentId) {
        // UUID 패턴 확인
        boolean isUuid = assignmentId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        
        String actualAssignmentId = assignmentId;
        
        if (isUuid) {
            // UUID인 경우, 실제 데이터베이스의 ID로 매핑
            actualAssignmentId = AssignmentUuidMapper.mapUuidToActualId(assignmentId, assignmentRepository);
        }
        
        rubricItemRepository.deleteByAssignmentId(actualAssignmentId);
    }
    
    // 루브릭 존재 여부 확인
    public boolean hasRubric(String assignmentId) {
        // UUID 패턴 확인
        boolean isUuid = assignmentId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        
        String actualAssignmentId = assignmentId;
        
        if (isUuid) {
            // UUID인 경우, 실제 데이터베이스의 ID로 매핑
            actualAssignmentId = AssignmentUuidMapper.mapUuidToActualId(assignmentId, assignmentRepository);
        }
        
        return rubricItemRepository.existsByAssignmentId(actualAssignmentId);
    }
    
    // 루브릭 아이템들 저장
    private List<RubricItemEntity> saveRubricItems(String assignmentId, List<RubricItemDTO> rubricitem) {
        List<RubricItemEntity> entities = rubricitem.stream()
                .map(item -> RubricItemEntity.builder()
                        .assignmentId(assignmentId)
                        .itemTitle(item.getItemTitle())
                        .maxScore(item.getMaxScore())
                        .description(item.getDescription())
                        .itemOrder(item.getItemOrder())
                        .build())
                .collect(Collectors.toList());
        
        return rubricItemRepository.saveAll(entities);
    }
    
    // 총점 계산
    private Integer calculateTotalScore(List<RubricItemDTO> rubricitem) {
        return rubricitem.stream()
                .mapToInt(RubricItemDTO::getMaxScore)
                .sum();
    }
    
    // DTO 변환
    private RubricDTO convertToRubricDTO(String assignmentId, List<RubricItemEntity> items) {
        List<RubricItemDTO> itemDTOs = items.stream()
                .map(this::convertToRubricItemDTO)
                .collect(Collectors.toList());
        
        return RubricDTO.builder()
                .assignmentId(assignmentId)
                .rubricTitle("채점 기준") // 기본 제목
                .totalScore(calculateTotalScore(itemDTOs))
                .rubricitem(itemDTOs)
                .build();
    }
    
    private RubricItemDTO convertToRubricItemDTO(RubricItemEntity item) {
        return RubricItemDTO.builder()
                .rubricItemId(item.getRubricItemId())
                .itemTitle(item.getItemTitle())
                .maxScore(item.getMaxScore())
                .description(item.getDescription())
                .itemOrder(item.getItemOrder())
                .build();
    }
} 