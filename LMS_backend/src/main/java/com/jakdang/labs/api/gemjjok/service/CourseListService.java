package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.api.gemjjok.repository.CourseListRepository;
import com.jakdang.labs.api.gemjjok.DTO.CourseListResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.CourseDetailResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.StudentResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.StudentCourseResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.jakdang.labs.api.auth.repository.UserRepository;
import com.jakdang.labs.api.gemjjok.repository.MemberRepository;
import com.jakdang.labs.entity.MemberEntity;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseListService {
    private final CourseListRepository courseRepository;
    private final UserRepository userRepository;
    @Qualifier("gemjjokMemberRepository")
    private final MemberRepository memberRepository;
    private final com.jakdang.labs.api.chanwook.service.ClassroomService classroomService;

    // 모든 활성 강의 목록 조회
    public List<CourseListResponseDTO> getAllActiveCourses() {
        List<CourseEntity> courses = courseRepository.findAllActiveCourses();
        return courses.stream()
                .map(this::convertToCourseListResponseDTO)
                .collect(Collectors.toList());
    }

    // 강사별 모든 강의 목록 조회 (users.id → email → member.memberEmail/ROLE_INSTRUCTOR → member.memberId → course.memberId)
    public List<CourseListResponseDTO> getAllCoursesByMemberId(String userId) {
        // log.info("[강의조회] userId: {}", userId);
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        // log.info("[강의조회] email: {}", email);
        if (email == null) return List.of();
        // 1. memberEmail, ROLE_INSTRUCTOR로 member row 조회
        Optional<MemberEntity> memberOpt = memberRepository.findByMemberEmailIgnoreCase(email)
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()));
        // log.info("[강의조회] memberOpt present: {}", memberOpt.isPresent());
        if (memberOpt.isEmpty()) return List.of();
        String memberId = memberOpt.get().getMemberId();
        // log.info("[강의조회] memberId: {}", memberId);
        // 2. course.memberId와 일치하는 강의만 반환 (쿼리로 바로 조회)
        List<CourseEntity> courses = courseRepository.findByMemberId(memberId);
        // log.info("[강의조회] courses.size: {}", courses.size());
        return courses.stream().map(this::convertToCourseListResponseDTO).collect(Collectors.toList());
    }
    // 강사별 활성 강의 목록 조회 (users.id → email → member → courseId → course)
    public List<CourseListResponseDTO> getActiveCoursesByMemberId(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        List<String> courseIds = memberRepository.findByMemberEmailIgnoreCase(email)
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()) && m.getCourseId() != null)
            .map(m -> List.of(m.getCourseId()))
            .orElse(List.of());
        if (courseIds.isEmpty()) return List.of();
        List<CourseEntity> courses = courseRepository.findAllById(courseIds).stream()
            .filter(c -> c.getCourseActive() == 1)
            .collect(Collectors.toList());
        return courses.stream().map(this::convertToCourseListResponseDTO).collect(Collectors.toList());
    }

    // 강의 상세 정보 조회
    public CourseDetailResponseDTO getCourseDetailById(String courseId) {
        // 1차: 그대로 조회
        Optional<CourseEntity> courseOpt = courseRepository.findByCourseIdAndCourseActive(courseId, 0);
        // 2차: 숫자라면 문자열로 변환해서도 조회 시도
        if (courseOpt.isEmpty()) {
            try {
                Integer intId = Integer.valueOf(courseId);
                courseOpt = courseRepository.findByCourseIdAndCourseActive(intId.toString(), 0);
            } catch (NumberFormatException e) {
                // 무시
            }
        }
        // 3차: Native Query fallback (숫자 courseId를 문자열로 변환해서 비교)
        if (courseOpt.isEmpty()) {
            List<CourseEntity> nativeResult = courseRepository.findByCourseIdNative(courseId);
            if (!nativeResult.isEmpty()) {
                return convertToCourseDetailResponseDTO(nativeResult.get(0));
            }
        }
        CourseEntity course = courseOpt.orElseThrow(() -> new IllegalArgumentException("해당 강의를 찾을 수 없습니다."));
        return convertToCourseDetailResponseDTO(course);
    }

    // 학생이 수강하는 강의 목록 조회 (강의실 정보와 자료 개수 포함)
    public List<StudentCourseResponseDTO> getStudentCourses(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        
        List<CourseEntity> courses = courseRepository.findCoursesByStudentId(userId);
        return courses.stream().map(this::convertToStudentCourseResponseDTO).collect(Collectors.toList());
    }

    // courseId와 memberId가 모두 일치하는 강의 리스트 조회
    // public List<CourseListResponseDTO> getCoursesByCourseIdAndMemberId(String courseId, String memberId) {
    //     List<CourseEntity> courses = courseRepository.findByCourseIdAndMemberId(courseId, memberId);
    //     return courses.stream().map(this::convertToCourseListResponseDTO).collect(Collectors.toList());
    // }
    
    // Native Query로 실제 SQL 실행 결과 확인 (테스트용)
    public void debugMemberIdQuery(String memberId) {
        // log.info("=== Native Query 디버깅 ===");
        // log.info("테스트 memberId: '{}'", memberId);
        
        // Native Query로 개수 확인
        Long count = courseRepository.countByMemberIdNative(memberId);
        // log.info("Native Query COUNT 결과: {}", count);
        
        // Native Query로 전체 데이터 확인
        List<CourseEntity> courses = courseRepository.findByMemberIdNative(memberId);
        // log.info("Native Query SELECT 결과 개수: {}", courses.size());
        
        if (!courses.isEmpty()) {
            // log.info("Native Query 첫 번째 결과:");
            // CourseEntity firstCourse = courses.get(0);
            // log.info("  - courseId: '{}'", firstCourse.getCourseId());
            // log.info("  - memberId: '{}'", firstCourse.getMemberId());
            // log.info("  - courseName: '{}'", firstCourse.getCourseName());
        }
        
        // JPA Query와 비교 - 사용하지 않는 메서드로 주석 처리
        // List<CourseEntity> jpaCourses = courseRepository.findByMemberId(memberId);
        // log.info("JPA Query 결과 개수: {}", jpaCourses.size());
        // log.info("=== Native Query 디버깅 완료 ===");
    }

    // CourseEntity를 CourseListResponseDTO로 변환
    private CourseListResponseDTO convertToCourseListResponseDTO(CourseEntity course) {
        return CourseListResponseDTO.builder()
                .courseId(course.getCourseId())
                .memberId(course.getMemberId())
                .educationId(course.getEducationId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .maxCapacity(course.getMaxCapacity())
                .minCapacity(course.getMinCapacity())
                .classId(course.getClassId())
                .courseStartDay(course.getCourseStartDay())
                .courseEndDay(course.getCourseEndDay())
                .courseDays(course.getCourseDays())
                .startTime(String.valueOf(course.getStartTime()))
                .endTime(String.valueOf(course.getEndTime()))
                .courseActive(course.getCourseActive())
                .build();
    }

    // CourseEntity를 CourseDetailResponseDTO로 변환
    private CourseDetailResponseDTO convertToCourseDetailResponseDTO(CourseEntity course) {
        List<StudentResponseDTO> students = new ArrayList<>();
        int currentStudentCount = 0; // TODO: 실제 학생 수 조회
        double attendanceRate = 0.0; // TODO: 실제 출석률 계산
        
        return CourseDetailResponseDTO.builder()
                .courseId(course.getCourseId())
                .memberId(course.getMemberId())
                .educationId(course.getEducationId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .maxCapacity(course.getMaxCapacity())
                .minCapacity(course.getMinCapacity())
                .classId(course.getClassId())
                .courseStartDay(course.getCourseStartDay())
                .courseEndDay(course.getCourseEndDay())
                .courseDays(course.getCourseDays())
                .startTime(String.valueOf(course.getStartTime()))
                .endTime(String.valueOf(course.getEndTime()))
                .courseActive(course.getCourseActive())
                .createdAt(course.getCreatedAt() )
                .updatedAt(course.getUpdatedAt() )
                .students(students)
                .currentStudentCount(currentStudentCount)
                .attendanceRate(attendanceRate)
                .build();
    }

    // CourseEntity를 StudentCourseResponseDTO로 변환 (강의실 정보와 자료 개수 포함)
    private StudentCourseResponseDTO convertToStudentCourseResponseDTO(CourseEntity course) {
        // 강의실 정보 조회
        String classCode = null;
        if (course.getClassId() != null) {
            try {
                com.jakdang.labs.entity.ClassroomEntity classroom = classroomService.findByClassId(course.getClassId());
                if (classroom != null) {
                    classCode = classroom.getClassCode();
                }
                    } catch (Exception e) {
            // log.warn("강의실 정보 조회 실패: classId={}, error={}", course.getClassId(), e.getMessage());
        }
        }
        
        // 자료 개수 조회 (임시로 0으로 설정)
        Integer materialsCount = 0;
        
        return StudentCourseResponseDTO.builder()
                .courseId(course.getCourseId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .maxCapacity(course.getMaxCapacity())
                .minCapacity(course.getMinCapacity())
                .classId(course.getClassId())
                .classCode(classCode) // 강의실 코드 추가
                .courseStartDay(course.getCourseStartDay())
                .courseEndDay(course.getCourseEndDay())
                .courseDays(course.getCourseDays())
                .startTime(String.valueOf(course.getStartTime()))
                .endTime(String.valueOf(course.getEndTime()))
                .courseActive(course.getCourseActive())
                .memberId(course.getMemberId())
                .educationId(course.getEducationId())
                .materialsCount(materialsCount) // 자료 개수 추가
                .build();
    }
} 