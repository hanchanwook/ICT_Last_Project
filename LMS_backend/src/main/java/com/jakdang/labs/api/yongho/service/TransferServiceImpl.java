package com.jakdang.labs.api.yongho.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.entity.PermissionEntity;
import com.jakdang.labs.entity.TransferEntity;
import com.jakdang.labs.api.yongho.dto.TransferDto;
import com.jakdang.labs.api.yongho.repository.PermissionManagementRepository;
import com.jakdang.labs.api.yongho.repository.TransferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final PermissionManagementRepository permissionRepo;

    @Override
    @Transactional(readOnly = true)
    public List<TransferDto> getByMemberId(String memberId) {
        List<TransferEntity> entities = transferRepository.findByMemberId(memberId);
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TransferDto> updatePermissions(String memberId, List<Integer> requestedPmIds) {
        // 기존 권한 삭제
        transferRepository.deleteByMemberId(memberId);
        
        // 1) toGrant 초기화 (사용자가 요청한 것)
        Set<Integer> toGrant = new HashSet<>(requestedPmIds);
        
        // 2) 모든 부모(type=1) 권한도 추가 (기존 로직)
        for (Integer pmId : new HashSet<>(toGrant)) {
            addAllParentPermissions(pmId, toGrant);
        }
        
        // 3) type=2 권한에 딸린 type=3 권한을 자동 추가
        List<PermissionEntity> allPerms = permissionRepo.findAll();
        for (PermissionEntity p2 : allPerms) {
            if ("2".equals(p2.getType()) && toGrant.contains(p2.getPmId())) {
                allPerms.stream()
                    .filter(p3 -> "3".equals(p3.getType()) && p2.getPmId().equals(p3.getSelfFk()))
                    .map(PermissionEntity::getPmId)
                    .forEach(toGrant::add);
            }
        }

        // 4) toGrant에 담긴 최종 권한을 저장
        List<TransferEntity> entities = toGrant.stream()
            .map(pmId -> {
                TransferEntity te = new TransferEntity();
                te.setMemberId(memberId);
                te.setPmId(pmId);
                return te;
            })
            .toList();

        List<TransferEntity> saved = transferRepository.saveAll(entities);
        return saved.stream()
            .map(this::convertToDto)
            .toList();
    }

    /** 
     * 재귀적으로 selfFk → 부모를 따라 type=1 권한까지 모두 수집 
     */
    private void addAllParentPermissions(Integer pmId, Set<Integer> collected) {
        permissionRepo.findById(pmId).ifPresent(perm -> {
            Integer parent = perm.getSelfFk();
            if (parent != null && !collected.contains(parent)) {
                collected.add(parent);
                addAllParentPermissions(parent, collected);
            }
        });
    }

    @Override
    public Integer findPmIdByUri(String uri) {
        // URI에 따른 권한 ID 매핑 로직 구현
        // 실제 구현은 요구사항에 따라 다를 수 있습니다
        return null;
    }

    @Override
    public boolean hasPermission(String memberId, Integer pmId) {
        if (pmId == null) {
            return false;
        }
        
        List<Integer> userPermissions = getByMemberId(memberId).stream()
                .map(TransferDto::getPmId)
                .collect(Collectors.toList());
        
        return userPermissions.contains(pmId);
    }

    private TransferDto convertToDto(TransferEntity entity) {
        return new TransferDto(
                entity.getTId(),
                entity.getPmId(),
                entity.getMemberId(),
                entity.getCreatedAt()
        );
    }

}
