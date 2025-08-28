package com.jakdang.labs.api.chanwook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;
import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.api.chanwook.repository.MemberManagementRepository;
import com.jakdang.labs.api.auth.repository.UserRepository;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.auth.entity.UserEntity;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberManagementService {

    private final InstructorMemberRepository instructorMemberRepository;
    private final MemberManagementRepository memberManagementRepository;
    private final UserRepository userRepository;

    // 특정 학생 정보 조회
    public Map<String, Object> getMemberInfo(String userId) {
        log.info("특정 학생 정보 조회: {}", userId);
        
        try {
            // 실제 학생 정보 조회
            MemberEntity member = instructorMemberRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + userId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원 정보 조회 성공");
            response.put("memberInfo", Map.of(
                "userId", member.getId(),
                "memberName", member.getMemberName(),
                "memberEmail", member.getMemberEmail(),
                "memberPhone", member.getMemberPhone(),
                "memberRole", member.getMemberRole(),
                "courseId", member.getCourseId(),
                "educationId", member.getEducationId()
            ));
            return response;
            
        } catch (Exception e) {
            log.error("회원 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("회원 정보 조회에 실패했습니다: " + e.getMessage());
        }
    }

    // 1. 전체 회원 목록 조회 (활성 회원만)
    public List<Map<String, Object>> getAllMembers(Map<String, String> params) {
        log.info("전체 회원 목록 조회: params={}", params);
        
        // userId 파라미터 추출
        String userId = params.get("userId");
        log.info("추출된 userId: {}", userId);
        
        // userId가 없으면 에러 처리
        if (userId == null || userId.trim().isEmpty()) {
            log.error("userId가 제공되지 않았습니다. params: {}", params);
            throw new RuntimeException("userId가 제공되지 않았습니다. 사용자 ID를 입력해주세요.");
        }
        
        // userId로 educationId 조회
        final String educationId;
        try {
            List<MemberEntity> userMembers = instructorMemberRepository.findByIdField(userId);
            if (!userMembers.isEmpty()) {
                educationId = userMembers.get(0).getEducationId();
                log.info("userId {}로 조회된 educationId: {}", userId, educationId);
            } else {
                educationId = null;
            }
        } catch (Exception e) {
            log.error("userId로 educationId 조회 실패: {}", e.getMessage());
            throw new RuntimeException("사용자 정보 조회에 실패했습니다: " + e.getMessage());
        }
        
        // educationId가 없으면 에러 처리
        if (educationId == null || educationId.trim().isEmpty()) {
            log.error("userId {}에 해당하는 educationId를 찾을 수 없습니다.", userId);
            throw new RuntimeException("해당 사용자의 교육기관 정보를 찾을 수 없습니다.");
        }
        
        List<Map<String, Object>> allMembers = new ArrayList<>();
        
        // 각 역할별로 조회
        String[] roles = {"ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_STAFF"};
        
        for (String role : roles) {
            List<MemberEntity> members;
            
            // 해당 교육기관의 회원만 조회
            log.info("{} 역할 회원 조회 (educationId: {})", role, educationId);
            members = memberManagementRepository.findByMemberRole(role)
                .stream()
                .filter(member -> {
                    // 1. educationId가 일치하는지 확인
                    boolean educationMatch = educationId.equals(member.getEducationId());
                    
                    // 2. memberExpired가 null인지 확인 (MemberEntity)
                    boolean memberActive = member.getMemberExpired() == null;
                    
                    // educationId와 memberExpired 조건만 확인 (activated는 필터링하지 않음)
                    boolean isActive = educationMatch && memberActive;
                    
                    if (!isActive) {
                        log.debug("회원 필터링 제외: userId={}, role={}, educationMatch={}, memberActive={}", 
                                 member.getId(), role, educationMatch, memberActive);
                    }
                    
                    return isActive;
                })
                .collect(Collectors.toList());
            
            // 학생인 경우 수용인원 정보를 배치로 미리 조회
            Map<String, Object[]> courseCapacityInfo = new HashMap<>();
            if ("ROLE_STUDENT".equals(role)) {
                try {
                    // 학생들의 courseId 수집
                    Set<String> courseIds = members.stream()
                        .map(MemberEntity::getCourseId)
                        .filter(id -> id != null && !id.isEmpty())
                        .collect(Collectors.toSet());
                    
                    // 배치로 수용인원 정보 조회
                    for (String courseId : courseIds) {
                        Optional<Object[]> courseInfoOpt = memberManagementRepository.findCourseInfoByCourseId(courseId);
                        if (courseInfoOpt.isPresent()) {
                            Object[] courseInfo = courseInfoOpt.get();
                            if (courseInfo.length >= 2) {
                                Integer maxCapacity = (Integer) courseInfo[1];
                                Long currentStudentCount = memberManagementRepository.countStudentsByCourseId(courseId);
                                courseCapacityInfo.put(courseId, new Object[]{maxCapacity, currentStudentCount});
                            }
                        }
                    }
                    log.debug("학생 수용인원 정보 배치 조회 완료: {} 개 과정", courseCapacityInfo.size());
                } catch (Exception e) {
                    log.warn("학생 수용인원 정보 배치 조회 실패: {}", e.getMessage());
                }
            }
            
            // 각 회원을 Map으로 변환하고 memberRole 추가
            for (MemberEntity member : members) {
                Map<String, Object> memberMap = toMemberMap(member, courseCapacityInfo);
                memberMap.put("memberRole", role);
                allMembers.add(memberMap);
            }
        }
        
        log.info("전체 회원 조회 완료: {} 건 (userId: {}, educationId: {})", allMembers.size(), userId, educationId);
        return allMembers;
    }
    
    // MemberEntity를 Map으로 변환
    private Map<String, Object> toMemberMap(MemberEntity entity) {
        return toMemberMap(entity, null);
    }
    
    // MemberEntity를 Map으로 변환 (수용인원 정보 포함)
    private Map<String, Object> toMemberMap(MemberEntity entity, Map<String, Object[]> courseCapacityInfo) {
        // 과정명 조회 (학생과 강사는 다른 로직)
        String courseName = null;
        String courseId = null;
        
        if ("ROLE_STUDENT".equals(entity.getMemberRole())) {
            // 학생: courseId를 가지고 있음
            if (entity.getCourseId() != null) {
                courseId = entity.getCourseId();
                courseName = memberManagementRepository.findCourseNameByCourseId(courseId)
                        .orElse(null);
                log.debug("학생 과정명 조회: courseId={}, courseName={}", courseId, courseName);
            }
        } else if ("ROLE_INSTRUCTOR".equals(entity.getMemberRole())) {
            // 강사: memberId를 가지고 있음 → CourseEntity에서 조회
            if (entity.getMemberId() != null) {
                try {
                    // memberId로 해당 강사가 담당하는 과정 조회
                    List<Map<String, Object>> instructorCourses = memberManagementRepository.findCoursesByMemberId(entity.getMemberId());
                    if (!instructorCourses.isEmpty()) {
                        Map<String, Object> course = instructorCourses.get(0); // 첫 번째 과정
                        courseId = (String) course.get("courseId");
                        courseName = (String) course.get("courseName");
                        log.debug("강사 과정명 조회: memberId={}, courseId={}, courseName={}", 
                                 entity.getMemberId(), courseId, courseName);
                    }
                } catch (Exception e) {
                    log.warn("강사 과정 조회 실패: memberId={}, error={}", entity.getMemberId(), e.getMessage());
                }
            }
        }
        // 수용인원 정보 조회 (학생만)
        Integer maxCapacity = null;
        Long currentStudentCount = null;
        Long remainingCapacity = null;
        
        if ("ROLE_STUDENT".equals(entity.getMemberRole()) && courseId != null) {
            // 배치로 조회한 정보가 있으면 사용, 없으면 개별 조회
            if (courseCapacityInfo != null && courseCapacityInfo.containsKey(courseId)) {
                Object[] capacityInfo = courseCapacityInfo.get(courseId);
                maxCapacity = (Integer) capacityInfo[0];
                currentStudentCount = (Long) capacityInfo[1];
                remainingCapacity = maxCapacity != null ? maxCapacity - currentStudentCount : null;
            } else {
                try {
                    // 과정 정보 조회 (maxCapacity 포함)
                    Optional<Object[]> courseInfoOpt = memberManagementRepository.findCourseInfoByCourseId(courseId);
                    if (courseInfoOpt.isPresent()) {
                        Object[] courseInfo = courseInfoOpt.get();
                        if (courseInfo.length >= 2) {
                            maxCapacity = (Integer) courseInfo[1];
                            
                            // 현재 등록된 학생 수 조회
                            currentStudentCount = memberManagementRepository.countStudentsByCourseId(courseId);
                            remainingCapacity = maxCapacity != null ? maxCapacity - currentStudentCount : null;
                        }
                    }
                } catch (Exception e) {
                    log.warn("학생 목록에서 과정 수용 인원 정보 조회 실패: courseId={}, error={}", courseId, e.getMessage());
                    // 에러가 발생해도 기본 정보는 제공
                }
            }
        }
        
        // UserEntity에서 activated 값 조회
        Integer activated = null;
        try {
            Optional<UserEntity> userOpt = userRepository.findById(entity.getId());
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                activated = user.getActivated() != null && user.getActivated() ? 1 : 0;
                log.debug("UserEntity activated 조회: userId={}, activated={}", entity.getId(), activated);
            } else {
                log.warn("UserEntity를 찾을 수 없음: userId={}", entity.getId());
                activated = 0; // 기본값
            }
        } catch (Exception e) {
            log.error("UserEntity activated 조회 실패: userId={}, error={}", entity.getId(), e.getMessage());
            activated = 0; // 기본값
        }
        
        // 역할별 상태 결정
        String status = getStatusByRole(entity);
        
        Map<String, Object> map = new HashMap<>();
        map.put("userId", entity.getId());
        map.put("memberName", entity.getMemberName());
        map.put("courseId", courseId != null ? courseId : entity.getCourseId()); // 강사의 경우 조회된 courseId 사용
        map.put("courseName", courseName != null ? courseName : "과정명 없음");
        map.put("phone", entity.getMemberPhone());
        map.put("email", entity.getMemberEmail());
        map.put("memberAddress", entity.getMemberAddress());
        map.put("memberBirthday", entity.getMemberBirthday());
        map.put("enrollmentDate", entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        map.put("status", status);
        map.put("activated", activated); // activated 값 추가
        
        // 수용인원 정보 추가 (학생만)
        if ("ROLE_STUDENT".equals(entity.getMemberRole())) {
            map.put("maxCapacity", maxCapacity);
            map.put("currentStudentCount", currentStudentCount);
            map.put("remainingCapacity", remainingCapacity);
            
            // 추가 과정 신청 가능 여부 계산
            boolean canRegisterMore = false;
            try {
                // 해당 학생이 이미 신청한 과정 수 확인
                List<MemberEntity> existingMembers = memberManagementRepository.findByMemberRole("ROLE_STUDENT")
                    .stream()
                    .filter(member -> entity.getId().equals(member.getId()))
                    .collect(Collectors.toList());
                
                // 현재 과정 외에 추가 신청 가능한 과정이 있는지 확인
                if (!existingMembers.isEmpty()) {
                    String educationId = existingMembers.get(0).getEducationId();
                    List<Map<String, Object>> allCourses = memberManagementRepository.findAllCourses();
                    
                    Set<String> existingCourseIds = existingMembers.stream()
                        .map(MemberEntity::getCourseId)
                        .filter(id -> id != null && !id.isEmpty())
                        .collect(Collectors.toSet());
                    
                    long availableCourseCount = allCourses.stream()
                        .filter(courseMap -> {
                            String courseEducationId = (String) courseMap.get("educationId");
                            String availableCourseId = (String) courseMap.get("courseId");
                            return educationId.equals(courseEducationId) && !existingCourseIds.contains(availableCourseId);
                        })
                        .count();
                    
                    canRegisterMore = availableCourseCount > 0;
                }
            } catch (Exception e) {
                log.warn("추가 과정 신청 가능 여부 확인 실패: userId={}, error={}", entity.getId(), e.getMessage());
            }
            
            map.put("canRegisterMore", canRegisterMore);
        }
        
        return map;
    }
    
    // Object[]를 Map으로 변환 (과정명 포함)
    private Map<String, Object> toMemberMapWithCourse(Object[] result) {
        // 안전한 타입 캐스팅
        if (result == null || result.length < 2) {
            throw new RuntimeException("회원 정보를 찾을 수 없습니다.");
        }
        Object memberObj = result[0];
        Object courseNameObj = result[1];
        if (!(memberObj instanceof MemberEntity)) {
            throw new RuntimeException("회원 정보 형식이 올바르지 않습니다.");
        }
        MemberEntity entity = (MemberEntity) memberObj;
        String courseName = courseNameObj != null ? courseNameObj.toString() : null;
        
        // UserEntity에서 activated 값 조회
        Integer activated = null;
        try {
            Optional<UserEntity> userOpt = userRepository.findById(entity.getId());
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                activated = user.getActivated() != null && user.getActivated() ? 1 : 0;
                log.debug("UserEntity activated 조회: userId={}, activated={}", entity.getId(), activated);
            } else {
                log.warn("UserEntity를 찾을 수 없음: userId={}", entity.getId());
                activated = 0; // 기본값
            }
        } catch (Exception e) {
            log.error("UserEntity activated 조회 실패: userId={}, error={}", entity.getId(), e.getMessage());
            activated = 0; // 기본값
        }
        
        // 역할별 상태 결정
        String status = getStatusByRole(entity);
        
        Map<String, Object> map = new HashMap<>();
        map.put("userId", entity.getId());
        map.put("memberName", entity.getMemberName());
        map.put("memberRole", entity.getMemberRole());
        map.put("courseId", entity.getCourseId());
        map.put("courseName", courseName != null ? courseName : "과정명 없음");
        map.put("phone", entity.getMemberPhone());
        map.put("email", entity.getMemberEmail());
        map.put("memberAddress", entity.getMemberAddress());
        map.put("memberBirthday", entity.getMemberBirthday());
        map.put("enrollmentDate", entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        map.put("status", status);
        map.put("activated", activated); // activated 값 추가
        return map;
    }
    
    // 역할별 상태 결정
    private String getStatusByRole(MemberEntity entity) {
        boolean isActive = entity.getMemberExpired() == null || entity.getMemberExpired().isAfter(LocalDateTime.now());
        
        switch (entity.getMemberRole()) {
            case "ROLE_STUDENT":
                return isActive ? "재학중" : "퇴학";
            case "ROLE_INSTRUCTOR":
                return isActive ? "재직중" : "퇴직";
            case "ROLE_STAFF":
                return isActive ? "재직중" : "퇴직";
            default:
                return isActive ? "활성" : "비활성";
        }
    }

    // 2. 특정 회원 상세 정보 조회 (모든 역할)
    public Map<String, Object> getMemberDetail(String userId) {
        log.info("특정 회원 상세 정보 조회: userId={}", userId);
        
        // undefined 체크 추가
        if (userId == null || userId.isEmpty() || "undefined".equals(userId)) {
            log.error("유효하지 않은 userId: {}", userId);
            throw new RuntimeException("유효하지 않은 회원 ID입니다: " + userId);
        }
        
        // 모든 역할에서 해당 userId로 회원 조회
        String[] roles = {"ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_STAFF"};
        List<MemberEntity> allMembers = new ArrayList<>();
        
        for (String role : roles) {
            List<MemberEntity> members = memberManagementRepository.findByMemberRole(role)
                    .stream()
                    .filter(member -> userId.equals(member.getId()))
                    .collect(Collectors.toList());
            allMembers.addAll(members);
        }
        
        if (allMembers.isEmpty()) {
            throw new RuntimeException("회원을 찾을 수 없습니다: " + userId);
        }
        
        log.info("조회된 회원 역할 수: {} 개", allMembers.size());
        
        // 첫 번째 회원의 기본 정보 (이름, 이메일 등은 동일)
        MemberEntity firstMember = allMembers.get(0);
        
        // 모든 과정 정보 수집 (역할별로 다른 로직)
        List<Map<String, Object>> courseList = new ArrayList<>();
        for (MemberEntity member : allMembers) {
            String courseName = null;
            String courseId = null;
            
            if ("ROLE_STUDENT".equals(member.getMemberRole())) {
                // 학생: courseId를 가지고 있음
                if (member.getCourseId() != null) {
                    courseId = member.getCourseId();
                    courseName = memberManagementRepository.findCourseNameByCourseId(courseId)
                            .orElse(null);
                    log.debug("학생 과정명 조회: courseId={}, courseName={}", courseId, courseName);
                }
            } else if ("ROLE_INSTRUCTOR".equals(member.getMemberRole())) {
                // 강사: memberId를 가지고 있음 → CourseEntity에서 조회
                if (member.getMemberId() != null) {
                    try {
                        // memberId로 해당 강사가 담당하는 과정 조회
                        List<Map<String, Object>> instructorCourses = memberManagementRepository.findCoursesByMemberId(member.getMemberId());
                        if (!instructorCourses.isEmpty()) {
                            Map<String, Object> course = instructorCourses.get(0); // 첫 번째 과정
                            courseId = (String) course.get("courseId");
                            courseName = (String) course.get("courseName");
                            log.debug("강사 과정명 조회: memberId={}, courseId={}, courseName={}", 
                                     member.getMemberId(), courseId, courseName);
                        }
                    } catch (Exception e) {
                        log.warn("강사 과정 조회 실패: memberId={}, error={}", member.getMemberId(), e.getMessage());
                    }
                }
            }
            // ROLE_STAFF는 과정명 조회하지 않음
            
            // 수용 인원 정보 조회 (학생과 강사만)
            Integer maxCapacity = null;
            Long currentStudentCount = null;
            Long remainingCapacity = null;
            
            if (courseId != null && ("ROLE_STUDENT".equals(member.getMemberRole()) || "ROLE_INSTRUCTOR".equals(member.getMemberRole()))) {
                try {
                    // 과정 정보 조회 (maxCapacity 포함)
                    Optional<Object[]> courseInfoOpt = memberManagementRepository.findCourseInfoByCourseId(courseId);
                    if (courseInfoOpt.isPresent()) {
                        Object[] courseInfo = courseInfoOpt.get();
                        if (courseInfo.length >= 2) {
                            maxCapacity = (Integer) courseInfo[1];
                            
                            // 현재 등록된 학생 수 조회
                            currentStudentCount = memberManagementRepository.countStudentsByCourseId(courseId);
                            remainingCapacity = maxCapacity != null ? maxCapacity - currentStudentCount : null;
                        } else {
                            // 배열 길이가 2보다 작은 경우 (maxCapacity가 null인 경우)
                            maxCapacity = null;
                            currentStudentCount = 0L;
                            remainingCapacity = null;
                        }
                    }
                } catch (Exception e) {
                    log.warn("과정 수용 인원 정보 조회 실패: courseId={}, error={}", courseId, e.getMessage());
                    maxCapacity = null;
                    currentStudentCount = 0L;
                    remainingCapacity = null;
                }
            }
            
            Map<String, Object> courseInfo = new HashMap<>();
            courseInfo.put("courseId", courseId != null ? courseId : member.getCourseId()); // 강사의 경우 조회된 courseId 사용
            courseInfo.put("courseName", courseName != null ? courseName : "과정명 없음");
            courseInfo.put("educationId", member.getEducationId());
            courseInfo.put("memberRole", member.getMemberRole());
            courseInfo.put("memberExpired", member.getMemberExpired());
            courseInfo.put("status", getStatusByRole(member));
            courseInfo.put("maxCapacity", maxCapacity);
            courseInfo.put("currentStudentCount", currentStudentCount);
            courseInfo.put("remainingCapacity", remainingCapacity);
            
            courseList.add(courseInfo);
        }
        
        // UserEntity에서 activated 값 조회
        Integer activated = null;
        try {
            Optional<UserEntity> userOpt = userRepository.findById(firstMember.getId());
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                activated = user.getActivated() != null && user.getActivated() ? 1 : 0;
                log.debug("UserEntity activated 조회: userId={}, activated={}", firstMember.getId(), activated);
            } else {
                log.warn("UserEntity를 찾을 수 없음: userId={}", firstMember.getId());
                activated = 0; // 기본값
            }
        } catch (Exception e) {
            log.error("UserEntity activated 조회 실패: userId={}, error={}", firstMember.getId(), e.getMessage());
            activated = 0; // 기본값
        }
        
        // 기본 정보와 과정 목록을 포함한 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("userId", firstMember.getId());
        response.put("memberName", firstMember.getMemberName());
        response.put("phone", firstMember.getMemberPhone());
        response.put("email", firstMember.getMemberEmail());
        response.put("emergencyContact", firstMember.getMemberPhone());
        response.put("memberAddress", firstMember.getMemberAddress());
        response.put("memberBirthday", firstMember.getMemberBirthday());
        response.put("courses", courseList);
        response.put("totalCourses", courseList.size());
        response.put("enrollmentDate", firstMember.getCreatedAt() != null ? firstMember.getCreatedAt() : LocalDateTime.now());
        response.put("activated", activated); // activated 값 추가
        
        // 기본 과정 정보 (첫 번째 과정)
        if (!courseList.isEmpty()) {
            Map<String, Object> primaryCourse = courseList.get(0);
            response.put("courseId", primaryCourse.get("courseId"));
            response.put("courseName", primaryCourse.get("courseName"));
            response.put("memberRole", primaryCourse.get("memberRole"));
            response.put("status", primaryCourse.get("status"));
        }
        
        log.info("회원 상세 정보 조회 완료: userId={}, 과정 수={}", userId, courseList.size());
        return response;
    }
    
    // Entity를 Map으로 변환 (상세용)
    private Map<String, Object> toMemberDetailMap(MemberEntity entity, String courseName) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", entity.getId());
        map.put("memberName", entity.getMemberName());
        map.put("courseId", entity.getCourseId());
        map.put("courseName", courseName != null ? courseName : "과정명 없음");
        map.put("phone", entity.getMemberPhone());
        map.put("email", entity.getMemberEmail());
        map.put("memberAddress", entity.getMemberAddress());
        map.put("memberBirthday", entity.getMemberBirthday());
        map.put("status", entity.getMemberExpired() == null || entity.getMemberExpired().isAfter(LocalDateTime.now()) ? "재학중" : "퇴학");
        map.put("emergencyContact", entity.getMemberPhone()); 
        map.put("enrollmentDate", entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        return map;
    }

    // 3. 회원 정보 수정 (기본 정보 + 비활성화 처리)
    public Map<String, Object> updateMember(String userId, Map<String, Object> memberData) {
        log.info("회원 정보 수정: userId={}, data={}", userId, memberData);
        
        // courseId가 있으면 특정 과정만, 없으면 모든 과정 처리
        String targetCourseId = (String) memberData.get("courseId");
        
        List<MemberEntity> members = new ArrayList<>();
        
        if (targetCourseId != null && !targetCourseId.isEmpty()) {
            // 특정 과정만 조회 (역할별로 다른 로직)
            String[] roles = {"ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_STAFF"};
            for (String role : roles) {
                List<MemberEntity> roleMembers = memberManagementRepository.findByMemberRole(role)
                    .stream()
                    .filter(member -> {
                        if (!userId.equals(member.getId())) {
                            return false;
                        }
                        
                        if ("ROLE_STUDENT".equals(role)) {
                            // 학생: MemberEntity의 courseId와 직접 비교
                            return targetCourseId.equals(member.getCourseId());
                        } else if ("ROLE_INSTRUCTOR".equals(role)) {
                            // 강사: memberId로 실제 담당하는 과정 조회하여 비교
                            try {
                                List<Map<String, Object>> instructorCourses = memberManagementRepository.findCoursesByMemberId(member.getMemberId());
                                return instructorCourses.stream()
                                    .anyMatch(course -> targetCourseId.equals(course.get("courseId")));
                            } catch (Exception e) {
                                log.warn("강사 과정 조회 실패: memberId={}, error={}", member.getMemberId(), e.getMessage());
                                return false;
                            }
                        } else {
                            // ROLE_STAFF: courseId가 없으므로 모든 과정에 대해 처리
                            return true;
                        }
                    })
                    .collect(Collectors.toList());
                members.addAll(roleMembers);
            }
            log.info("특정 과정 수정: courseId={}", targetCourseId);
        } else {
            // 모든 과정 조회 (모든 역할에서)
            String[] roles = {"ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_STAFF"};
            for (String role : roles) {
                List<MemberEntity> roleMembers = memberManagementRepository.findByMemberRole(role)
                    .stream()
                    .filter(member -> userId.equals(member.getId()))
                    .collect(Collectors.toList());
                members.addAll(roleMembers);
            }
            log.info("모든 과정 수정");
        }
        
        if (members.isEmpty()) {
            throw new RuntimeException("회원을 찾을 수 없습니다: userId=" + userId + 
                    (targetCourseId != null ? ", courseId=" + targetCourseId : ""));
        }
        
        log.info("수정할 회원 과정 수: {} 개", members.size());
        
        // 첫 번째 회원의 기본 정보 (이름, 이메일 등은 동일)
        MemberEntity firstMember = members.get(0);
        
        log.info("업데이트 전 회원 정보 - 이름: {}, 전화: {}, 이메일: {}, 주소: {}, 생일: {}", 
                firstMember.getMemberName(), firstMember.getMemberPhone(), firstMember.getMemberEmail(), 
                firstMember.getMemberAddress(), firstMember.getMemberBirthday());
        
        // 받은 데이터 로그 출력
        log.info("받은 업데이트 데이터: {}", memberData);
        
        // 데이터 업데이트
        boolean hasChanges = false;
        
        // 기본 정보 업데이트 (모든 과정에 동일하게 적용)
        for (MemberEntity member : members) {
            boolean memberChanged = false;
            
            if (memberData.get("memberName") != null && !memberData.get("memberName").equals(member.getMemberName())) {
                log.info("이름 변경: {} -> {}", member.getMemberName(), memberData.get("memberName"));
                member.setMemberName((String) memberData.get("memberName"));
                memberChanged = true;
            }
                if (memberData.get("phone") != null && !memberData.get("phone").equals(member.getMemberPhone())) {
                String newPhone = (String) memberData.get("phone");
                
                // 전화번호 중복 검증
                Optional<UserEntity> existingUserWithPhone = userRepository.findByPhone(newPhone);
                if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(userId)) {
                    throw new RuntimeException("이미 사용 중인 전화번호입니다: " + newPhone);
                }
                
                log.info("전화번호 변경: {} -> {}", member.getMemberPhone(), newPhone);
                member.setMemberPhone(newPhone);
                memberChanged = true;
                
                // UserEntity의 phone 값도 업데이트
                try {
                    Optional<UserEntity> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        UserEntity user = userOpt.get();
                        if (!newPhone.equals(user.getPhone())) {
                            log.info("UserEntity phone 변경: {} -> {}", user.getPhone(), newPhone);
                            user.setPhone(newPhone);
                            userRepository.save(user);
                            log.info("UserEntity phone 업데이트 완료");
                        }
                    } else {
                        log.warn("UserEntity를 찾을 수 없음: userId={}", userId);
                    }
                } catch (Exception e) {
                    log.error("UserEntity phone 업데이트 실패: userId={}, error={}", userId, e.getMessage());
                }
            }
            if (memberData.get("email") != null && !memberData.get("email").equals(member.getMemberEmail())) {
                String newEmail = (String) memberData.get("email");
                
                // 이메일 중복 검증
                Optional<UserEntity> existingUserWithEmail = userRepository.findByEmail(newEmail);
                if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(userId)) {
                    throw new RuntimeException("이미 사용 중인 이메일입니다: " + newEmail);
                }
                
                log.info("이메일 변경: {} -> {}", member.getMemberEmail(), newEmail);
                member.setMemberEmail(newEmail);
                memberChanged = true;
                
                // UserEntity의 email 값도 업데이트
                try {
                    Optional<UserEntity> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        UserEntity user = userOpt.get();
                        if (!newEmail.equals(user.getEmail())) {
                            log.info("UserEntity email 변경: {} -> {}", user.getEmail(), newEmail);
                            user.setEmail(newEmail);
                            userRepository.save(user);
                            log.info("UserEntity email 업데이트 완료");
                        }
                    } else {
                        log.warn("UserEntity를 찾을 수 없음: userId={}", userId);
                    }
                } catch (Exception e) {
                    log.error("UserEntity email 업데이트 실패: userId={}, error={}", userId, e.getMessage());
                }
            }
            // 주소 업데이트 (여러 가능한 키 이름 처리)
            String addressValue = (String) memberData.get("address");
            if (addressValue == null) addressValue = (String) memberData.get("memberAddress");
            if (addressValue == null) addressValue = (String) memberData.get("addr");
            
            if (addressValue != null && !addressValue.equals(member.getMemberAddress())) {
                log.info("주소 변경: {} -> {}", member.getMemberAddress(), addressValue);
                member.setMemberAddress(addressValue);
                memberChanged = true;
            }
            
            // 생년월일 업데이트 (여러 가능한 키 이름 처리)
            String birthDateValue = (String) memberData.get("birthDate");
            if (birthDateValue == null) birthDateValue = (String) memberData.get("memberBirthday");
            if (birthDateValue == null) birthDateValue = (String) memberData.get("birthday");
            if (birthDateValue == null) birthDateValue = (String) memberData.get("birth");
            
            if (birthDateValue != null && !birthDateValue.equals(member.getMemberBirthday())) {
                log.info("생일 변경: {} -> {}", member.getMemberBirthday(), birthDateValue);
                member.setMemberBirthday(birthDateValue);
                memberChanged = true;
            }
            
            // 비활성화/활성화 처리 (학생만 memberExpired 사용, 강사/직원은 UserEntity.activated만 사용)
            if (memberData.containsKey("memberExpired")) {
                Object memberExpiredObj = memberData.get("memberExpired");
                
                // 학생인 경우에만 memberExpired 처리
                if ("ROLE_STUDENT".equals(member.getMemberRole())) {
                    // 활성화 처리 (null, 빈 문자열, "null" 문자열인 경우)
                    if (memberExpiredObj == null || 
                        (memberExpiredObj instanceof String && 
                         (((String) memberExpiredObj).isEmpty() || "null".equals(memberExpiredObj)))) {
                        
                        log.info("학생 활성화 처리: {} -> null", member.getMemberExpired());
                        member.setMemberExpired(null);
                        memberChanged = true;
                    } else {
                        // 비활성화 처리 (시간 설정)
                        LocalDateTime memberExpired;
                        
                        // memberExpired를 LocalDateTime으로 변환
                        if (memberExpiredObj instanceof String) {
                            String dateStr = (String) memberExpiredObj;
                            try {
                                // Z(UTC 시간대)가 포함된 경우 제거
                                if (dateStr.endsWith("Z")) {
                                    dateStr = dateStr.substring(0, dateStr.length() - 1);
                                }
                                memberExpired = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (Exception e) {
                                log.error("날짜 파싱 실패: {}, 에러: {}", dateStr, e.getMessage());
                                // 파싱 실패 시 현재 시간으로 설정
                                memberExpired = LocalDateTime.now();
                            }
                        } else if (memberExpiredObj instanceof LocalDateTime) {
                            memberExpired = (LocalDateTime) memberExpiredObj;
                        } else {
                            // 현재 시간으로 설정 (기존 로직)
                            memberExpired = LocalDateTime.now();
                        }
                        
                        log.info("학생 비활성화 처리: {} -> {}", member.getMemberExpired(), memberExpired);
                        member.setMemberExpired(memberExpired);
                        memberChanged = true;
                    }
                } else {
                    // 강사/직원인 경우 memberExpired는 무시하고 로그만 출력
                    log.info("강사/직원은 memberExpired를 사용하지 않습니다. role={}, userId={}", member.getMemberRole(), userId);
                }
            }
            
            if (memberChanged) {
                hasChanges = true;
                memberManagementRepository.save(member);
                log.info("과정 {} 업데이트 완료", member.getCourseId());
            }
        }
        
        // UserEntity의 activated 값 업데이트 (모든 역할에 대해 처리)
        if (memberData.containsKey("activated")) {
            Object activatedObj = memberData.get("activated");
            if (activatedObj != null) {
                try {
                    Optional<UserEntity> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        UserEntity user = userOpt.get();
                        Boolean newActivated = null;
                        
                        // activated 값을 Boolean으로 변환
                        if (activatedObj instanceof Integer) {
                            newActivated = ((Integer) activatedObj) == 1;
                        } else if (activatedObj instanceof String) {
                            String activatedStr = (String) activatedObj;
                            if ("1".equals(activatedStr) || "true".equalsIgnoreCase(activatedStr)) {
                                newActivated = true;
                            } else if ("0".equals(activatedStr) || "false".equalsIgnoreCase(activatedStr)) {
                                newActivated = false;
                            }
                        } else if (activatedObj instanceof Boolean) {
                            newActivated = (Boolean) activatedObj;
                        }
                        
                        if (newActivated != null && !newActivated.equals(user.getActivated())) {
                            String role = firstMember.getMemberRole();
                            if ("ROLE_STUDENT".equals(role)) {
                                log.info("학생 UserEntity activated 변경: {} -> {}", user.getActivated(), newActivated);
                            } else if ("ROLE_INSTRUCTOR".equals(role)) {
                                log.info("강사 UserEntity activated 변경: {} -> {}", user.getActivated(), newActivated);
                            } else if ("ROLE_STAFF".equals(role)) {
                                log.info("직원 UserEntity activated 변경: {} -> {}", user.getActivated(), newActivated);
                            }
                            user.setActivated(newActivated);
                            userRepository.save(user);
                            hasChanges = true;
                            log.info("UserEntity activated 업데이트 완료");
                        }
                    } else {
                        log.warn("UserEntity를 찾을 수 없음: userId={}", userId);
                    }
                } catch (Exception e) {
                    log.error("UserEntity activated 업데이트 실패: userId={}, error={}", userId, e.getMessage());
                }
            }
        }
        
        if (!hasChanges) {
            log.info("변경사항이 없습니다.");
            // 업데이트된 정보로 응답 생성
            return getMemberDetail(userId);
        }
        
        log.info("업데이트 후 회원 정보 - 이름: {}, 전화: {}, 이메일: {}, 주소: {}, 생일: {}", 
                firstMember.getMemberName(), firstMember.getMemberPhone(), firstMember.getMemberEmail(), 
                firstMember.getMemberAddress(), firstMember.getMemberBirthday());
        
        log.info("DB 저장 완료: {} 개 과정 업데이트", members.size());
        
        // 업데이트된 정보로 응답 생성
        Map<String, Object> result = getMemberDetail(userId);
        result.put("updatedAt", LocalDateTime.now());
        result.put("message", "회원 정보가 성공적으로 수정되었습니다. (" + members.size() + "개 과정)");
        
        return result;
    }
    
    // 4. 학생의 추가 수강 신청 가능 과정 목록 조회
    public Map<String, Object> getAvailableCourses(String userId) {
        log.info("학생의 추가 수강 신청 가능 과정 목록 조회: userId={}", userId);
        
        try {
            // 1. 해당 학생이 이미 신청한 과정 목록 조회
            List<MemberEntity> existingMembers = memberManagementRepository.findByMemberRole("ROLE_STUDENT")
                .stream()
                .filter(member -> userId.equals(member.getId()))
                .collect(Collectors.toList());
            
            Set<String> existingCourseIds = existingMembers.stream()    
                .map(MemberEntity::getCourseId)
                .filter(courseId -> courseId != null && !courseId.isEmpty())
                .collect(Collectors.toSet());
            
            log.info("학생이 이미 신청한 과정 수: {} 개, courseIds: {}", existingCourseIds.size(), existingCourseIds);
            
            // 2. 해당 학생의 educationId 조회
            final String educationId;
            if (!existingMembers.isEmpty()) {
                educationId = existingMembers.get(0).getEducationId();
            } else {
                // 학생이 아직 과정을 신청하지 않은 경우, UserEntity에서 educationId 조회
                Optional<UserEntity> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    // UserEntity에서 educationId를 가져오는 로직이 필요할 수 있음
                    // 현재는 기본값 사용
                    educationId = "default_education_id"; // 실제 로직에 맞게 수정 필요
                } else {
                    throw new RuntimeException("학생의 교육기관 정보를 찾을 수 없습니다: " + userId);
                }
            }
            
            // 3. 해당 교육기관의 모든 과정 조회 (이미 신청한 과정 제외)
            List<Map<String, Object>> allCourses = memberManagementRepository.findAllCourses();
            log.info("조회된 전체 과정 수: {} 개", allCourses.size());
            
            List<Map<String, Object>> availableCourses = allCourses
                .stream()
                .filter(courseMap -> {
                    // Map에서 데이터 추출
                    String courseId = (String) courseMap.get("courseId");
                    String courseEducationId = (String) courseMap.get("educationId");
                    
                    // 같은 교육기관이고, 아직 신청하지 않은 과정만 필터링
                    return educationId.equals(courseEducationId) && !existingCourseIds.contains(courseId);
                })
                .map(courseMap -> {
                    // Map을 새로운 Map으로 변환
                    Map<String, Object> course = new HashMap<>();
                    String courseId = (String) courseMap.get("courseId");
                    String courseName = (String) courseMap.get("courseName");
                    String courseEducationId = (String) courseMap.get("educationId");
                    
                    course.put("courseId", courseId);
                    course.put("courseName", courseName);
                    course.put("educationId", courseEducationId);
                    
                    // 수용인원 정보 조회를 안전하게 처리
                    try {
                        // 과정 정보 조회 (maxCapacity 포함)
                        Optional<Object[]> courseInfoOpt = memberManagementRepository.findCourseInfoByCourseId(courseId);
                        if (courseInfoOpt.isPresent()) {
                            Object[] courseInfo = courseInfoOpt.get();
                            if (courseInfo.length >= 2) {
                                String infoCourseName = (String) courseInfo[0];
                                Integer maxCapacity = (Integer) courseInfo[1];
                                
                                // 현재 등록된 학생 수 조회
                                Long currentStudentCount = memberManagementRepository.countStudentsByCourseId(courseId);
                                
                                // 수용 인원 정보 추가
                                course.put("courseName", infoCourseName);
                                course.put("maxCapacity", maxCapacity);
                                course.put("currentStudentCount", currentStudentCount);
                                course.put("remainingCapacity", maxCapacity != null ? maxCapacity - currentStudentCount : null);
                            } else {
                                // 배열 길이가 2보다 작은 경우 (maxCapacity가 null인 경우)
                                String infoCourseName = (String) courseInfo[0];
                                course.put("courseName", infoCourseName);
                                course.put("maxCapacity", null);
                                course.put("currentStudentCount", 0L);
                                course.put("remainingCapacity", null);
                            }
                        } else {
                            // 과정 정보를 찾을 수 없는 경우
                            course.put("courseName", courseName); // 원래 조회된 과정명 사용
                            course.put("maxCapacity", null);
                            course.put("currentStudentCount", 0L);
                            course.put("remainingCapacity", null);
                        }
                    } catch (Exception e) {
                        // 수용인원 정보 조회 중 에러가 발생해도 기본 정보는 제공
                        log.warn("과정 {}의 수용인원 정보 조회 실패: {}", courseId, e.getMessage());
                        course.put("courseName", courseName);
                        course.put("maxCapacity", null);
                        course.put("currentStudentCount", null);
                        course.put("remainingCapacity", null);
                        course.put("capacityError", "수용인원 정보를 조회할 수 없습니다.");
                    }
                    
                    return course;
                })
                .collect(Collectors.toList());
            
            log.info("신청 가능한 과정 수: {} 개", availableCourses.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "수강 신청 가능 과정 목록 조회 성공");
            response.put("availableCourses", availableCourses);
            response.put("courseCount", availableCourses.size());
            response.put("existingCourseCount", existingCourseIds.size());
            response.put("userId", userId);
            response.put("educationId", educationId);
            
            return response;
            
        } catch (Exception e) {
            log.error("학생의 추가 수강 신청 가능 과정 목록 조회 실패: {}", e.getMessage());
            throw new RuntimeException("수강 신청 가능 과정 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    // 5. 학생의 추가 과정 신청
    public Map<String, Object> registerCourse(Map<String, Object> courseData) {
        log.info("학생의 추가 과정 신청: courseData={}", courseData);
        
        try {
            String userId = (String) courseData.get("userId");
            String courseId = (String) courseData.get("courseId");
            
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("사용자 ID가 누락되었습니다.");
            }
            
            if (courseId == null || courseId.isEmpty()) {
                throw new RuntimeException("과정 ID가 누락되었습니다.");
            }
            
            // 1. 중복 신청 확인
            List<MemberEntity> existingMembers = memberManagementRepository.findByMemberRole("ROLE_STUDENT")
                .stream()
                .filter(member -> userId.equals(member.getId()) && courseId.equals(member.getCourseId()))
                .collect(Collectors.toList());
            
            if (!existingMembers.isEmpty()) {
                throw new RuntimeException("이미 신청한 과정입니다: " + courseId);
            }
            
            // 2. 과정 정보 조회 (과정명, 최대 수용 인원 포함)
            String courseName = null;
            Integer maxCapacity = null;
            Long currentStudentCount = null;
            Boolean capacityValidationPassed = false;
            
            try {
                Optional<Object[]> courseInfoOpt = memberManagementRepository.findCourseInfoByCourseId(courseId);
                if (courseInfoOpt.isPresent()) {
                    Object[] courseInfo = courseInfoOpt.get();
                    courseName = (String) courseInfo[0];
                    maxCapacity = (Integer) courseInfo[1];
                    
                    // 2-1. 수용 인원 검증 (선택적)
                    if (maxCapacity != null) {
                        currentStudentCount = memberManagementRepository.countStudentsByCourseId(courseId);
                        log.info("과정 수용 인원 검증: courseId={}, maxCapacity={}, currentCount={}", 
                                courseId, maxCapacity, currentStudentCount);
                        
                        if (currentStudentCount >= maxCapacity) {
                            log.warn("과정이 최대 수용 인원에 도달했습니다. (최대: {}명, 현재: {}명)", 
                                    maxCapacity, currentStudentCount);
                            // 수용인원 초과 시에도 신청 허용 (프론트엔드에서 처리)
                            capacityValidationPassed = false;
                        } else {
                            capacityValidationPassed = true;
                        }
                    } else {
                        // maxCapacity가 null인 경우 검증 통과로 간주
                        capacityValidationPassed = true;
                    }
                } else {
                    log.warn("과정 정보를 찾을 수 없습니다: {}", courseId);
                    courseName = "과정명 없음";
                }
            } catch (Exception e) {
                log.warn("과정 정보 조회 중 에러 발생: courseId={}, error={}", courseId, e.getMessage());
                courseName = "과정명 없음";
                capacityValidationPassed = false;
            }
            
            // 3. 기존 학생 정보 조회 (educationId 등 기본 정보 가져오기)
            List<MemberEntity> existingStudentMembers = memberManagementRepository.findByMemberRole("ROLE_STUDENT")
                .stream()
                .filter(member -> userId.equals(member.getId()))
                .collect(Collectors.toList());
            
            if (existingStudentMembers.isEmpty()) {
                throw new RuntimeException("학생 정보를 찾을 수 없습니다: " + userId);
            }
            
            MemberEntity existingMember = existingStudentMembers.get(0);
            
            // 4. 새로운 MemberEntity 생성
            MemberEntity newMember = new MemberEntity();
            newMember.setId(userId); // 기존 userId 사용
            newMember.setMemberName(existingMember.getMemberName());
            newMember.setMemberEmail(existingMember.getMemberEmail());
            newMember.setMemberPhone(existingMember.getMemberPhone());
            newMember.setMemberAddress(existingMember.getMemberAddress());
            newMember.setMemberBirthday(existingMember.getMemberBirthday());
            newMember.setMemberRole("ROLE_STUDENT");
            newMember.setCourseId(courseId);
            newMember.setEducationId(existingMember.getEducationId());
            newMember.setCreatedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
            newMember.setUpdatedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
            
            // 5. DB에 저장
            memberManagementRepository.save(newMember);
            
            log.info("학생의 추가 과정 신청 성공: userId={}, courseId={}, courseName={}", 
                userId, courseId, courseName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "과정 신청이 완료되었습니다");
            response.put("userId", userId);
            response.put("courseId", courseId);
            response.put("courseName", courseName);
            response.put("registeredAt", LocalDateTime.now());
            
            // 수용 인원 정보 추가 (경고 메시지 포함)
            response.put("maxCapacity", maxCapacity);
            response.put("currentStudentCount", currentStudentCount);
            response.put("remainingCapacity", maxCapacity != null && currentStudentCount != null ? maxCapacity - currentStudentCount : null);
            response.put("capacityValidationPassed", capacityValidationPassed);
            
            if (!capacityValidationPassed) {
                response.put("capacityWarning", "과정이 최대 수용 인원에 도달했을 수 있습니다. 관리자에게 문의하세요.");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("학생의 추가 과정 신청 실패: {}", e.getMessage());
            throw new RuntimeException("과정 신청에 실패했습니다: " + e.getMessage());
        }
    }
}   