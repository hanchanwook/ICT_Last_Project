package com.jakdang.labs.api.yongho.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.entity.PermissionEntity;
import com.jakdang.labs.entity.TransferEntity;
import com.jakdang.labs.api.yongho.dto.PermissionManagementDto;
import com.jakdang.labs.api.yongho.repository.PermissionManagementRepository;
import com.jakdang.labs.api.yongho.repository.TransferRepository;

@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {
    
    @Autowired private TransferRepository transferRepo;
    @Autowired private PermissionManagementRepository permRepo;

    @Override
    public List<Integer> getGrantedPmIds(String memberId) {
        
        List<TransferEntity> transfers = transferRepo.findByMemberId(memberId);
        
        List<Integer> pmIds = transfers.stream()
                .map(TransferEntity::getPmId) // 이제 바로 Integer를 반환
                .filter(pmId -> pmId != null) // null 제거
                .collect(Collectors.toList());
        
        return pmIds;
    }

    @Override
    public List<PermissionManagementDto> getMemberPermissions(String memberId) {
        List<Integer> pmIds = getGrantedPmIds(memberId);
        if (pmIds.isEmpty()) {
            return List.of();
        }
        
        return permRepo.findByPmIdIn(pmIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updateMemberPermissions(String memberId, List<String> grantedPmIds) {
        transferRepo.deleteByMemberId(memberId);
        grantedPmIds.forEach(pmIdStr -> {
            try {
                Integer pmId = Integer.parseInt(pmIdStr);
                TransferEntity te = new TransferEntity();
                te.setMemberId(memberId);
                te.setPmId(pmId); // 이제 Integer로 저장
                transferRepo.save(te);
            } catch (NumberFormatException e) {
                
            }
        });
    }

    @Override
    public List<PermissionManagementDto> getAll() {
        return permRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionManagementDto createPermission(String permissionName, String permissionText, String type, String parentPmId) {
        // 중복 체크
        if (permRepo.existsByPermissionName(permissionName)) {
            throw new IllegalArgumentException("이미 존재하는 권한명입니다");
        }

        // type이 1(메인메뉴)이 아니면 상위 권한 존재 확인
        if (!"1".equals(type)) {
            if (parentPmId == null || parentPmId.isBlank()) {
                throw new IllegalArgumentException("상위 권한 ID가 필요합니다");
            }
            try {
                Integer parentId = Integer.parseInt(parentPmId);
                if (!permRepo.existsById(parentId)) {
                    throw new IllegalArgumentException("존재하지 않는 상위 권한입니다");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("유효하지 않은 상위 권한 ID입니다");
            }
        }

        PermissionEntity entity = new PermissionEntity();
        entity.setPmId(generateNextPmId());
        entity.setPermissionName(permissionName);
        entity.setPermissionText(permissionText);
        entity.setType(type);
        entity.setCreatedAt(LocalDate.now());

        // 메인메뉴면 selfFk는 null, 그 외는 parentPmId로 설정
        if ("1".equals(type) || parentPmId == null || parentPmId.isBlank()) {
            entity.setSelfFk(null);
        } else {
            try {
                entity.setSelfFk(Integer.parseInt(parentPmId));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("유효하지 않은 상위 권한 ID입니다");
            }
        }
        
        PermissionEntity saved = permRepo.save(entity);
        return toDto(saved);
    }

    @Override
    public List<PermissionManagementDto> getMainPermissions() {
        return permRepo.findBySelfFkIsNull().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionManagementDto> getSubPermissions() {
        return permRepo.findBySelfFkIsNotNull().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 다음 pmId 생성 (순차적 증가)
     */
    private Integer generateNextPmId() {
        PermissionEntity lastEntity = permRepo.findTopByOrderByPmIdDesc();
        if (lastEntity == null) {
            return 1;
        }
        
        return lastEntity.getPmId() + 1;
    }

    // PermissionEntity → DTO 변환
    private PermissionManagementDto toDto(PermissionEntity e) {
        return new PermissionManagementDto(
                e.getPmId(),
                e.getPermissionName(),
                e.getPermissionText(),
                e.getType(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getSelfFk()
        );
    }

    @Override
    public List<Map<String, Object>> getMyPermissions(String memberId) {
        List<PermissionManagementDto> all = getAll();
        List<Integer> granted = getGrantedPmIds(memberId);
        
        List<Map<String, Object>> permissions = all.stream()
            .map(permission -> {
                Map<String, Object> permissionMap = new HashMap<>();
                permissionMap.put("pmId", permission.getPmId());
                permissionMap.put("permissionName", permission.getPermissionName());
                permissionMap.put("permissionText", permission.getPermissionText());
                permissionMap.put("type", permission.getType());
                permissionMap.put("selfFk", permission.getSelfFk() != null ? permission.getSelfFk() : "");
                permissionMap.put("isGranted", granted.contains(permission.getPmId()));
                return permissionMap;
            })
            .collect(Collectors.toList());
        return permissions;
    }
}