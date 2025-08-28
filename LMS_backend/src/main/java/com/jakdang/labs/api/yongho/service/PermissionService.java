package com.jakdang.labs.api.yongho.service;

import java.util.List;
import java.util.Map;

import com.jakdang.labs.api.yongho.dto.PermissionManagementDto;

public interface PermissionService {
    
    //특정 직원이 가진 권한 ID 리스트 조회
    List<Integer> getGrantedPmIds(String memberId);

    //특정 직원이 가진 PermissionManagementDto 리스트 조회
    List<PermissionManagementDto> getMemberPermissions(String memberId);

    //특정 직원의 권한 관계(transfer 테이블) 갱신
    void updateMemberPermissions(String memberId, List<String> grantedPmIds);

    //모든 권한 조회
    List<PermissionManagementDto> getAll();
    
    //새로운 권한 생성 (간단 버전)
    PermissionManagementDto createPermission(String permissionName, String permissionText, String type, String parentPmId);
    
    //대분류 권한들만 조회
    List<PermissionManagementDto> getMainPermissions();

    //소분류 권한들만 조회
    List<PermissionManagementDto> getSubPermissions();

    //특정 직원의 권한 조회
    List<Map<String, Object>> getMyPermissions(String memberId);
} 