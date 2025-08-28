package com.jakdang.labs.api.yongho.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.yongho.dto.PermissionManagementDto;
import com.jakdang.labs.api.yongho.dto.TransferDto;
import com.jakdang.labs.api.yongho.service.MemberService;
import com.jakdang.labs.api.yongho.service.PermissionService;
import com.jakdang.labs.api.yongho.service.TransferService;


@RestController
@RequestMapping("/api/staff")
public class StaffPermissionController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private TransferService transferService;

    public StaffPermissionController(MemberService memberService, PermissionService permissionService, TransferService transferService) {
        this.memberService = memberService;
        this.permissionService = permissionService;
        this.transferService = transferService;
    }

    // 직원 목록 조회 (현재 로그인된 학원장과 동일한 educationId를 가진 멤버만)
    @GetMapping("/members")
    public ResponseEntity<List<MemberEntity>> getStaffAndInstructors() {
        try {
            
            // 현재 로그인된 사용자의 인증 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // CustomUserDetails에서 educationId 추출
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String currentEducationId = userDetails.getEducationId();
            
            if (currentEducationId == null || currentEducationId.isEmpty()) {
                System.out.println("❌ educationId가 null 또는 빈 문자열");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 직원과 강사 역할을 가진 사용자 중 동일한 educationId를 가진 사용자만 조회
            List<String> memberRole = Arrays.asList("ROLE_STAFF", "ROLE_INSTRUCTOR");
            List<MemberEntity> members = memberService.findByMemberRoleInAndEducationId(memberRole, currentEducationId);
            
            for (MemberEntity member : members) {
                System.out.println("- " + member.getMemberName() + " (" + member.getMemberRole() + ", educationId: " + member.getEducationId() + ")");
            }
            
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 디버깅용: 전체 직원 목록 조회 (educationId 필터링 없음)
    @GetMapping("/members/all")
    public ResponseEntity<List<MemberEntity>> getAllStaffAndInstructors() {
        try {
            List<String> memberRole = Arrays.asList("ROLE_STAFF", "ROLE_INSTRUCTOR");
            List<MemberEntity> allMembers = memberService.findByMemberRoleIn(memberRole);
            
            for (MemberEntity member : allMembers) {
                System.out.println("- " + member.getMemberName() + " (역할: " + member.getMemberRole() + ", educationId: " + member.getEducationId() + ")");
            }
            
            return ResponseEntity.ok(allMembers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 직원 권한 조회 (체크박스용)
    @GetMapping("/members/{memberId}/permissions")
    public ResponseEntity<Map<String, Object>> getMemberPermissions(@PathVariable("memberId") String memberId) {
        try {
            List<PermissionManagementDto> allPermissions = permissionService.getAll().stream()
                .filter(p -> !"3".equals(p.getType()))    // type=3 은 숨김
                .collect(Collectors.toList());
            List<Integer> grantedPmIds = permissionService.getGrantedPmIds(memberId);
            
            // 프론트엔드에서 사용하기 쉽도록 각 권한에 체크 상태를 포함
            List<Map<String, Object>> permissionsWithCheckStatus = allPermissions.stream()
                .map(permission -> {
                    Map<String, Object> permissionMap = new HashMap<>();
                    permissionMap.put("pmId", permission.getPmId());
                    permissionMap.put("permissionName", permission.getPermissionName());
                    permissionMap.put("permissionText", permission.getPermissionText());
                    permissionMap.put("type", permission.getType());
                    permissionMap.put("selfFk", permission.getSelfFk() != null ? permission.getSelfFk() : "");
                    permissionMap.put("isGranted", grantedPmIds.contains(permission.getPmId()));
                    return permissionMap;
                })
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "memberId", memberId,
                    "permissions", permissionsWithCheckStatus,
                    "totalPermissions", allPermissions.size(),
                    "grantedCount", grantedPmIds.size()
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "권한 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

     // 직원 권한 업데이트 (부여/회수)
     @PutMapping("/members/{memberId}/permissions")
     public ResponseEntity<Map<String, Object>> updateMemberPermissions(
             @PathVariable("memberId") String memberId,
             @RequestBody Map<String, List<String>> request
     ) {
         try {
             // 요청 데이터 검증
             List<String> grantedPmIds = request.get("grantedPmIds");
             if (grantedPmIds == null) {
                 return ResponseEntity.badRequest().body(Map.of(
                     "success", false,
                     "message", "grantedPmIds 필드가 필요합니다"
                 ));
             }
 
             // 현재 권한 조회 (로깅용)
             List<Integer> beforePermissions = permissionService.getGrantedPmIds(memberId);
             
             // String pmId를 Integer로 변환
             List<Integer> grantedPmIdsInt = grantedPmIds.stream()
                 .map(pmIdStr -> {
                     try {
                         return Integer.parseInt(pmIdStr);
                     } catch (NumberFormatException e) {
                         return null;
                     }
                 })
                 .filter(pmId -> pmId != null)
                 .collect(Collectors.toList());

             List<TransferDto> transfers = transferService.updatePermissions(memberId, grantedPmIdsInt);
             
             // 업데이트 후 권한 조회
             List<PermissionManagementDto> updatedPermissions = permissionService.getMemberPermissions(memberId);
             
             return ResponseEntity.ok(Map.of(
                 "success", true,
                 "message", "권한이 성공적으로 업데이트되었습니다",
                 "data", Map.of(
                     "memberId", memberId,
                     "beforePermissionCount", beforePermissions.size(),
                     "afterPermissionCount", grantedPmIds.size(),
                     "updatedPermissions", updatedPermissions
                 )
             ));
             
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                 "success", false,
                 "message", "권한 업데이트 중 오류가 발생했습니다: " + e.getMessage()
             ));
         }
     }



    /**
     * 간단한 권한 생성 API
     */
    @PostMapping("/permissions")
    public ResponseEntity<Map<String, Object>> createPermission(
            @RequestParam String permissionName,
            @RequestParam String permissionText,
            @RequestParam String type,
            @RequestParam(required = false) String parentPmId) {
                try{
            // 🚫 type이 1인데 parentPmId가 들어온 경우 예외
            if ("1".equals(type) && parentPmId != null && !parentPmId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "메인메뉴(type=1)는 상위 권한을 가질 수 없습니다"
                ));
            }

            PermissionManagementDto created = permissionService.createPermission(
                permissionName, permissionText, type, parentPmId
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "권한이 성공적으로 생성되었습니다",
                "data", created
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 대분류 권한 목록 조회 (드롭다운용)
     */
    @GetMapping("/permissions/main")
    public List<PermissionManagementDto> getMainPermissions() {
        return permissionService.getMainPermissions();
    }

    /**
     * 소분류 권한 목록 조회 (드롭다운용)
     */
    @GetMapping("/permissions/sub")
    public List<PermissionManagementDto> getSubPermissions() {
        return permissionService.getSubPermissions();
    }


    /**
     * 현재 로그인한 직원의 권한 조회 (편의 메서드)
     * memberId를 URL에 포함하지 않고 현재 인증된 사용자의 권한을 바로 조회
     */
    @GetMapping("/my-permissions")
    public ResponseEntity<Map<String, Object>> getCurrentMemberPermissions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "인증되지 않은 사용자입니다"
                ));
            }

            // CustomUserDetails에서 member 정보 가져오기
            Object principal = authentication.getPrincipal();
            com.jakdang.labs.api.auth.dto.CustomUserDetails memberDetails = 
                (com.jakdang.labs.api.auth.dto.CustomUserDetails) principal;
            
            String userEntityId = memberDetails.getUserId(); // CustomUserDetails의 getUserId() 메서드 사용
            String email = memberDetails.getEmail(); // CustomUserDetails의 getEmail() 메서드 사용
            
            // userEntityId/email로 Member 테이블에서 실제 memberId 찾기
            List<MemberEntity> allMembers = memberService.findByMemberRoleIn(
                Arrays.asList("ROLE_STAFF", "ROLE_INSTRUCTOR", "ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_STUDENT"));
            
            MemberEntity member = allMembers.stream()
                .filter(m -> userEntityId.equals(m.getId()) || email.equals(m.getMemberEmail()))
                .findFirst()
                .orElse(null);
                
            if (member == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "해당하는 member 정보를 찾을 수 없습니다"
                ));
            }
            
            // ⭐ 핵심: Member 테이블의 실제 PK 사용
            String memberId = member.getMemberId(); // ad3c6a1f-22bf-4021-9202-38d5789b7f92
            
            List<PermissionManagementDto> all = permissionService.getAll().stream()
                .filter(p -> !"3".equals(p.getType()))    // type=3 은 숨김
                .collect(Collectors.toList());
            List<Integer> granted = permissionService.getGrantedPmIds(memberId); // 올바른 memberId 전달
            
            // 권한을 계층별로 분류
            List<Map<String, Object>> mainPermissions = all.stream()
                .filter(permission -> "1".equals(permission.getType()))
                .map(permission -> createPermissionMap(permission, granted))
                .collect(Collectors.toList());
                
            List<Map<String, Object>> subPermissions = all.stream()
                .filter(permission -> !"1".equals(permission.getType()))
                .map(permission -> createPermissionMap(permission, granted))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "권한 조회 성공",
                "data", Map.of(
                    "memberId", memberId,
                    "mainPermissions", mainPermissions,
                    "subPermissions", subPermissions,
                    "totalPermissions", all.size(),
                    "grantedCount", granted.size()
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "권한 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 권한 정보를 Map으로 변환하는 헬퍼 메서드
     */
    private Map<String, Object> createPermissionMap(PermissionManagementDto permission, List<Integer> grantedPmIds) {
        Map<String, Object> permissionMap = new HashMap<>();
        permissionMap.put("pmId", permission.getPmId());
        permissionMap.put("permissionName", permission.getPermissionName());
        permissionMap.put("permissionText", permission.getPermissionText());
        permissionMap.put("type", permission.getType());
        permissionMap.put("selfFk", permission.getSelfFk() != null ? permission.getSelfFk() : "");
        permissionMap.put("isGranted", grantedPmIds.contains(permission.getPmId()));
        return permissionMap;
    }
}
