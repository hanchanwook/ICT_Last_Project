package com.jakdang.labs.config.interceptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.yongho.service.MemberService;
import com.jakdang.labs.api.yongho.service.TransferService;
import com.jakdang.labs.entity.MemberEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final TransferService transferService;
    private final MemberService memberService;
    
    // URI별 필요한 권한 매핑
    private static final Map<String, Integer> URI_PERMISSION_MAP = new HashMap<>();
    
    static {
        // 프론트엔드 permissionMap과 동일하게 설정
        // 계정 등록
        URI_PERMISSION_MAP.put("/api/registration", 1);

        // 학적부
        URI_PERMISSION_MAP.put("/api/academic", 2);
        // 전체 학생 출석 목록
        URI_PERMISSION_MAP.put("/api/attendance/management/all", 10);

        // 과정 관리
        URI_PERMISSION_MAP.put("/api/course", 3);
        // 과정 목록 조회
        URI_PERMISSION_MAP.put("/api/course/list", 14);
        // 과정 생성
        URI_PERMISSION_MAP.put("/api/course/create", 7);
        // 과목 리스트
        URI_PERMISSION_MAP.put("/api/subject/list", 13);
        // 과목 등록
        URI_PERMISSION_MAP.put("/api/subject/create", 15);

        // 세부 과목 리스트
        URI_PERMISSION_MAP.put("/api/subDetail/list", 16);
        // 세부 과목 등록
        URI_PERMISSION_MAP.put("/api/subDetail/create", 15);

        // 강의실 관리
        URI_PERMISSION_MAP.put("/api/classroom", 4);
        // 강의실 목록 조회
        URI_PERMISSION_MAP.put("/api/classroom/all", 17);
        // 강의실 생성
        URI_PERMISSION_MAP.put("/api/classroom/create", 18);

        // 설문 평가 관리
        URI_PERMISSION_MAP.put("/api/evaluations", 5);
        // 평가 질문 리스트
        URI_PERMISSION_MAP.put("/api/evaluation/list", 22);
        // 템플릿 목록
        URI_PERMISSION_MAP.put("/api/questiontemplate/list", 23);
        // 강의 리스트 
        URI_PERMISSION_MAP.put("/api/templategroup/list", 24);

        // 시험 및 성적
        URI_PERMISSION_MAP.put("/api/exam", 6);
        // 시험 문제 리스트
        URI_PERMISSION_MAP.put("/api/questions/subdetail", 25);
        // 시험 문제 생성
        URI_PERMISSION_MAP.put("/api/staff/exam/courses", 26);

    }

    public PermissionInterceptor(TransferService transferService, MemberService memberService) {
        this.transferService = transferService;
        this.memberService = memberService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (uri.equals("/api/staff/my-permissions")) {
            // 인증만 되어 있으면 모든 역할의 사용자가 접근 가능
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = auth!=null
                           && auth.getPrincipal() instanceof CustomUserDetails;
            if (!isAuthenticated) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("인증이 필요합니다.");
                return false;
            }
            return true;
        }
        
        // 권한 체크가 필요 없는 경로들
        if (isPublicPath(uri)) {
            return true;
        }
        
        // JWT에서 memberId 추출
         String memberId = getMemberIdFromAuthentication();
        if (memberId == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("인증이 필요합니다.");
            return false;
        }
        
        // URI에 필요한 권한 ID 찾기
        Integer requiredPmId = findRequiredPermissionForUri(uri);
        if (requiredPmId == null) {
            return true; // 권한이 필요하지 않은 API
        }
        
                 // 권한 체크는 ROLE_STAFF일 때만 수행
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         boolean needPermissionCheck = false;
         if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
             CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
             String role = userDetails.getUserEntity().getRole().name();
             needPermissionCheck = "ROLE_STAFF".equals(role);
             
             // ROLE_STAFF가 아닌 모든 역할은 권한 체크 없이 통과
             if (!needPermissionCheck) {
                 System.out.println("권한 체크 생략: " + role + " 역할은 모든 메뉴 접근 가능");
                 return true;
             }
         }
        
        // ROLE_STAFF인 경우에만 권한 체크 수행
        boolean hasPerm = transferService.hasPermission(
            memberId,
            Integer.valueOf(requiredPmId)
        );
        

        if (!hasPerm) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("권한이 없습니다.");
            return false;
        }

    return true;
}
    
    // 공개 경로인지 확인
    private boolean isPublicPath(String uri) {
        return uri.startsWith("/api/auth/") ||           // 인증 관련
               uri.startsWith("/api/public/") ||         // 공개 API
               uri.startsWith("/api/chat/") ||           // 채팅 관련
               uri.equals("/api/classroom/all") ||       // 교실 목록 조회
               uri.equals("/api/classroom/education-id") || // 교육기관 ID 조회
               uri.equals("/api/attendance/management/all") || // 출석 관리 전체 조회
               uri.equals("/api/instructor/students") || // 강사 담당 학생 목록 조회
               uri.matches("/api/classroom/[^/]+") ||    // 교실 상세 조회
               uri.matches("/api/classroom/update/[^/]+") || // 교실 수정
               uri.matches("/api/attendance/my-attendances/[^/]+") || // 학생 본인 출석 기록 조회
               uri.matches("/api/instructor/students/[^/]+"); // 학생 상세 정보 조회
    }
    
    // Authentication에서 memberId 추출
    private String getMemberIdFromAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof CustomUserDetails)) {
                return null;
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            String userEntityId = userDetails.getUserEntity().getId();
            String email = userDetails.getUserEntity().getEmail();
            
            // 방법 1: userId를 직접 사용 (새로운 방식)
            String directUserId = userEntityId;
                         // 방법 2: 기존 방식 - UserEntity 정보로 MemberEntity 찾기
             List<MemberEntity> allMembers = memberService.findByMemberRoleIn(
                 Arrays.asList("ROLE_STAFF", "ROLE_INSTRUCTOR", "ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_STUDENT"));
            
            MemberEntity member = allMembers.stream()
                .filter(m -> {
                    boolean idMatch = userEntityId.equals(m.getId()) || userEntityId.equals(m.getMemberId());
                    boolean emailMatch = email.equals(m.getMemberEmail());

                    return idMatch || emailMatch;
                })
                .findFirst()
                .orElse(null);
                
            String existingMemberId = member != null ? member.getMemberId() : null;
            // 우선순위: 기존 방식이 있으면 사용, 없으면 새로운 방식 사용
            String finalMemberId = existingMemberId != null ? existingMemberId : directUserId;
            return finalMemberId;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    
    // URI에 필요한 권한 ID 찾기
     
    private Integer findRequiredPermissionForUri(String uri) {
        for (Map.Entry<String, Integer> entry : URI_PERMISSION_MAP.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
