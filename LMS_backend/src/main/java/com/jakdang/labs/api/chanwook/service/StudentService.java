package com.jakdang.labs.api.chanwook.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.jakdang.labs.api.chanwook.repository.MemberManagementRepository;
import com.jakdang.labs.api.chanwook.repository.AttendanceRepository;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.AttendanceEntity;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudentService {

    private final MemberManagementRepository memberManagementRepository;
    private final AttendanceRepository attendanceRepository;

    // 1. 전체 학생 목록 조회 (활성 학생만)
    public List<Map<String, Object>> getAllStudents(Map<String, String> params) {
        log.info("전체 학생 목록 조회: params={}", params);
        
        // 활성 학생만 조회 (과정명 포함)
        List<Object[]> studentsWithCourse = memberManagementRepository.findActiveMembersWithCourseName();
        
        return studentsWithCourse.stream()
        .map(this::toStudentMapWithCourse)
        .collect(Collectors.toList());
    }
    
    // Object[]를 Map으로 변환 (과정명 포함)
    private Map<String, Object> toStudentMapWithCourse(Object[] result) {
        // 안전한 타입 캐스팅
        if (result == null || result.length < 2) {
            throw new RuntimeException("학생 정보를 찾을 수 없습니다.");
        }
        Object memberObj = result[0];
        Object courseNameObj = result[1];
        if (!(memberObj instanceof MemberEntity)) {
            throw new RuntimeException("학생 정보 형식이 올바르지 않습니다.");
        }
        MemberEntity entity = (MemberEntity) memberObj;
        String courseName = courseNameObj != null ? courseNameObj.toString() : null;
        Map<String, Object> map = new HashMap<>();
        map.put("userId", entity.getId());
        map.put("studentName", entity.getMemberName());
        map.put("courseId", entity.getCourseId());
        map.put("courseName", courseName != null ? courseName : "과정명 없음");
        map.put("phone", entity.getMemberPhone());
        map.put("email", entity.getMemberEmail());
        map.put("enrollmentDate", entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now()); // 입학일 추가
        map.put("status", entity.getMemberExpired() == null || entity.getMemberExpired().isAfter(LocalDateTime.now()) ? "재학중" : "퇴학");
        return map;
    }
    
    // 2. 특정 학생 상세 정보 조회
    public Map<String, Object> getStudentDetail(String userId) {
        log.info("특정 학생 상세 정보 조회: userId={}", userId);
        
        MemberEntity student = memberManagementRepository.findByIdAndMemberRole(userId, "학생")
                .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다: " + userId));
        
        // 과정명 직접 조회
        String courseName = null;
        if (student.getCourseId() != null) {
            courseName = memberManagementRepository.findCourseNameByCourseId(student.getCourseId())
                    .orElse(null);
            log.info("과정ID: {}, 조회된 과정명: {}", student.getCourseId(), courseName);
        }
        
        return toStudentDetailMap(student, courseName);
    }
    
    // Entity를 Map으로 변환 (상세용)
    private Map<String, Object> toStudentDetailMap(MemberEntity entity, String courseName) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", entity.getId());
        map.put("studentName", entity.getMemberName());
        map.put("courseId", entity.getCourseId());
        map.put("courseName", courseName != null ? courseName : "과정명 없음");
        map.put("phone", entity.getMemberPhone());
        map.put("email", entity.getMemberEmail());
        map.put("address", entity.getMemberAddress());
        map.put("birthDate", entity.getMemberBirthday());
        map.put("enrollmentDate", entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
        map.put("status", entity.getMemberExpired() == null || entity.getMemberExpired().isAfter(LocalDateTime.now()) ? "재학중" : "퇴학");
        map.put("emergencyContact", entity.getMemberPhone()); 
        return map;
    }
    
    // Object[]를 Map으로 변환 (상세용, 과정명 포함)
    private Map<String, Object> toStudentDetailMapWithCourse(Object[] result) { 
        // 안전한 타입 캐스팅
        if (result == null || result.length < 2) {
            throw new RuntimeException("학생 정보를 찾을 수 없습니다.");
        }
        
        Object memberObj = result[0];
        Object courseNameObj = result[1];
        
        if (!(memberObj instanceof MemberEntity)) {
            throw new RuntimeException("학생 정보 형식이 올바르지 않습니다.");
        }
        
        MemberEntity entity = (MemberEntity) memberObj;
        String courseName = courseNameObj != null ? courseNameObj.toString() : null;
        
        return toStudentDetailMap(entity, courseName);
    }

    // 4. 학생 정보 수정
    public Map<String, Object> updateStudent(String userId, Map<String, Object> studentData) {
        log.info("학생 정보 수정: userId={}, data={}", userId, studentData);
        
        MemberEntity student = memberManagementRepository.findByIdAndMemberRole(userId, "학생")
                .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다: " + userId));
        
        log.info("업데이트 전 학생 정보 - 이름: {}, 전화: {}, 이메일: {}, 주소: {}, 생일: {}, 과정ID: {}", 
                student.getMemberName(), student.getMemberPhone(), student.getMemberEmail(), 
                student.getMemberAddress(), student.getMemberBirthday(), student.getCourseId());
        
        // 데이터 업데이트
        boolean hasChanges = false;
        
        if (studentData.get("memberName") != null && !studentData.get("memberName").equals(student.getMemberName())) {
            log.info("이름 변경: {} -> {}", student.getMemberName(), studentData.get("memberName"));
            student.setMemberName((String) studentData.get("memberName"));
            hasChanges = true;
        }
        if (studentData.get("phone") != null && !studentData.get("phone").equals(student.getMemberPhone())) {
            log.info("전화번호 변경: {} -> {}", student.getMemberPhone(), studentData.get("phone"));
            student.setMemberPhone((String) studentData.get("phone"));
            hasChanges = true;
        }
        if (studentData.get("email") != null && !studentData.get("email").equals(student.getMemberEmail())) {
            log.info("이메일 변경: {} -> {}", student.getMemberEmail(), studentData.get("email"));
            student.setMemberEmail((String) studentData.get("email"));
            hasChanges = true;
        }
        if (studentData.get("address") != null && !studentData.get("address").equals(student.getMemberAddress())) {
            log.info("주소 변경: {} -> {}", student.getMemberAddress(), studentData.get("address"));
            student.setMemberAddress((String) studentData.get("address"));
            hasChanges = true;
        }
        if (studentData.get("birthDate") != null && !studentData.get("birthDate").equals(student.getMemberBirthday())) {
            log.info("생일 변경: {} -> {}", student.getMemberBirthday(), studentData.get("birthDate"));
            student.setMemberBirthday((String) studentData.get("birthDate"));
            hasChanges = true;
        }
        if (studentData.get("courseId") != null && !studentData.get("courseId").equals(student.getCourseId())) {
            log.info("과정ID 변경: {} -> {}", student.getCourseId(), studentData.get("courseId"));
            student.setCourseId((String) studentData.get("courseId"));
            hasChanges = true;
        }
        
        // 비활성화 처리 (memberExpired 필드가 있는 경우 현재 시간으로 설정)
        if (studentData.get("memberExpired") != null) {
            if (student.getMemberExpired() == null || student.getMemberExpired().isAfter(LocalDateTime.now())) {
                LocalDateTime now = LocalDateTime.now();
                log.info("학생 비활성화: {} -> {}", student.getMemberExpired(), now);
                student.setMemberExpired(now);
                hasChanges = true;
            }
        }
        
        if (!hasChanges) {
            log.info("변경사항이 없습니다.");
            // 과정명을 포함한 상세 정보 반환
            Object[] studentWithCourse = memberManagementRepository.findByUserIdAndMemberRoleWithCourse(userId, "학생")
                    .orElse(new Object[]{student, null});
            Map<String, Object> result = toStudentDetailMapWithCourse(studentWithCourse);
            result.put("updatedAt", student.getUpdatedAt());
            result.put("message", "변경사항이 없습니다.");
            return result;
        }
        
        log.info("업데이트 후 학생 정보 - 이름: {}, 전화: {}, 이메일: {}, 주소: {}, 생일: {}, 과정ID: {}", 
                student.getMemberName(), student.getMemberPhone(), student.getMemberEmail(), 
                student.getMemberAddress(), student.getMemberBirthday(), student.getCourseId());
        
        MemberEntity savedStudent = memberManagementRepository.save(student);
        log.info("DB 저장 완료: {}", savedStudent.getId());
        
        // 과정명을 포함한 상세 정보 반환
        Object[] updatedStudentWithCourse = memberManagementRepository.findByUserIdAndMemberRoleWithCourse(userId, "학생")
                .orElse(new Object[]{savedStudent, null});
        Map<String, Object> result = toStudentDetailMapWithCourse(updatedStudentWithCourse);
        result.put("updatedAt", LocalDateTime.now());
        result.put("message", "학생 정보가 성공적으로 수정되었습니다.");
        
        return result;
    }

    // 6. 학생별 출석 기록 조회
    public Map<String, Object> getStudentAttendance(String userId, Map<String, String> params) {
        log.info("학생별 출석 기록 조회: userId={}, params={}", userId, params);
        
        MemberEntity student = memberManagementRepository.findByIdAndMemberRole(userId, "ROLE_STUDENT")
                .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다: " + userId));
        
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", student.getId());
        result.put("memberName", student.getMemberName());
        result.put("attendanceRecords", List.of()); // 빈 리스트로 초기화
        result.put("totalCount", 0);
        result.put("attendanceRate", 0.0);
        result.put("message", "출석 기록 조회 기능이 준비 중입니다.");
        
        return result;
    }
} 