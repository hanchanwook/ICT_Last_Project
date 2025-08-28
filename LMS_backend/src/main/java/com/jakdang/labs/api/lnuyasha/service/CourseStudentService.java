package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.SubGroupRepository;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.SubGroupEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseStudentService {
    
    private final KyMemberRepository memberRepository;
    private final MemberService memberService;
    private final SubGroupRepository subGroupRepository;
    
    /**
     * 과정별 학생 목록 조회
     * @param subGroupId 소그룹 ID (실제로는 courseId 파라미터로 전달됨)
     * @param userId 사용자 ID (선택사항)
     * @return 학생 목록
     */
    public List<MemberInfoDTO> getCourseStudents(String subGroupId, String userId) {
        log.info("과정별 학생 목록 조회 요청: subGroupId = {}, userId = {}", subGroupId, userId);
        
        try {
            // userId가 있으면 해당 사용자의 권한 확인
            if (userId != null && !userId.trim().isEmpty()) {
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo == null) {
                    throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다: " + userId);
                }
                log.info("사용자 권한 확인: memberId = {}, educationId = {}", 
                        memberInfo.getMemberId(), memberInfo.getEducationId());
            }
            
            String courseId;
            
            // 1. 먼저 subGroupId로 SubGroupEntity 조회 시도
            SubGroupEntity subGroup = subGroupRepository.findBySubGroupId(subGroupId);
            if (subGroup != null) {
                // subGroupId가 유효한 경우
                courseId = subGroup.getCourseId();
                log.info("SubGroup 조회 성공: subGroupId = {}, courseId = {}", subGroupId, courseId);
            } else {
                // subGroupId가 유효하지 않은 경우, courseId로 직접 조회 시도
                log.warn("SubGroup을 찾을 수 없습니다. courseId로 직접 조회 시도: {}", subGroupId);
                courseId = subGroupId;
                
                // courseId가 유효한지 확인
                List<MemberEntity> testStudents = memberRepository.findActiveStudentsByCourseId(courseId);
                if (testStudents.isEmpty()) {
                    log.warn("courseId로도 학생을 찾을 수 없습니다: {}", courseId);
                    throw new IllegalArgumentException("해당 과정을 찾을 수 없습니다: " + subGroupId);
                }
                log.info("courseId로 직접 조회 성공: courseId = {}", courseId);
            }
            
            // 2. courseId로 학생 목록 조회 (memberExpired가 null인 활성 학생만)
            List<MemberEntity> students = memberRepository.findActiveStudentsByCourseId(courseId);
            
            log.info("조회된 학생 수: {}", students.size());
            
            // 디버깅: 조회된 학생 상세 정보
            if (students.isEmpty()) {
                log.warn("=== 과정별 학생 조회 결과가 없음 ===");
                log.warn("요청된 subGroupId: {}, 조회된 courseId: {}", subGroupId, courseId);
                
                // 전체 학생 중 해당 courseId를 가진 학생 확인
                try {
                    List<MemberEntity> allStudents = memberRepository.findAll();
                    List<MemberEntity> matchingCourseStudents = allStudents.stream()
                            .filter(student -> courseId.equals(student.getCourseId()))
                            .collect(Collectors.toList());
                    
                    log.warn("전체 학생 중 courseId={}인 학생 수: {}", courseId, matchingCourseStudents.size());
                    for (MemberEntity student : matchingCourseStudents) {
                        log.warn("학생 정보 - ID: {}, memberId: {}, courseId: {}, memberExpired: {}, memberRole: {}", 
                                 student.getId(), student.getMemberId(), student.getCourseId(), 
                                 student.getMemberExpired(), student.getMemberRole());
                    }
                } catch (Exception e) {
                    log.error("전체 학생 조회 중 오류: {}", e.getMessage());
                }
            } else {
                log.info("=== 조회된 학생 상세 정보 ===");
                for (MemberEntity student : students) {
                    log.info("학생 정보 - ID: {}, memberId: {}, courseId: {}, memberExpired: {}, memberRole: {}", 
                             student.getId(), student.getMemberId(), student.getCourseId(), 
                             student.getMemberExpired(), student.getMemberRole());
                }
            }
            
            // DTO로 변환
            List<MemberInfoDTO> studentDTOs = students.stream()
                    .map(student -> MemberInfoDTO.builder()
                            .memberId(student.getMemberId())
                            .educationId(student.getEducationId())
                            .id(student.getId())
                            .memberName(student.getMemberName())
                            .memberEmail(student.getMemberEmail())
                            .memberRole(student.getMemberRole())
                            .courseId(student.getCourseId())
                            .memberExpired(student.getMemberExpired())
                            .studentName(student.getMemberName()) // 호환성을 위한 별칭
                            .email(student.getMemberEmail()) // 호환성을 위한 별칭
                            .build())
                    .collect(Collectors.toList());
            
            return studentDTOs;
            
        } catch (IllegalArgumentException e) {
            log.error("과정별 학생 목록 조회 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("과정별 학생 목록 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new RuntimeException("과정별 학생 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 