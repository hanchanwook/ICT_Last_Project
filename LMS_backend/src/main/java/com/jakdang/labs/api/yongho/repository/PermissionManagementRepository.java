package com.jakdang.labs.api.yongho.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.PermissionEntity;

@Repository
public interface PermissionManagementRepository extends JpaRepository<PermissionEntity, Integer> {
    
    // 특정 권한 ID들로 조회
    List<PermissionEntity> findByPmIdIn(List<Integer> pmIds);
    
    // 모든 권한 조회
    List<PermissionEntity> findAll();
    
    // 대분류 권한들만 조회 (selfFk가 null인 것들)
    List<PermissionEntity> findBySelfFkIsNull();
    
    // 하위 권한들만 조회 (selfFk가 null이 아닌 것들)
    List<PermissionEntity> findBySelfFkIsNotNull();
    
    // 특정 대분류의 소분류들 조회 (selfFk가 특정 값인 것들)
    List<PermissionEntity> findBySelfFk(Integer parentPmId);
    
    // 특정 권한명으로 조회 (중복 체크용)
    boolean existsByPermissionName(String permissionName);
    
    // 다음 pmId 생성을 위한 최대값 조회
    PermissionEntity findTopByOrderByPmIdDesc();

    // 특정 pmId로 조회
    Optional<PermissionEntity> findByPmId(Integer pmId);
    
    // 특정 타입의 권한들만 조회
    List<PermissionEntity> findByType(String type);

}