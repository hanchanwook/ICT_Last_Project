package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.gemjjok.DTO.CourseListResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.CourseDetailResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.StudentResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.AttendanceResponseDTO;
import com.jakdang.labs.api.gemjjok.DTO.LectureStatsResponseDTO;
import com.jakdang.labs.api.gemjjok.service.CourseListService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/instructor")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
public class CourseListController {
    private final CourseListService courseService;

    // JWT 토큰에서 사용자 정보 추출
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        throw new RuntimeException("인증된 사용자 정보를 찾을 수 없습니다.");
    }

    // 강사가 담당하는 활성 강의 목록 조회
    @GetMapping("/lectures")
    public ResponseEntity<List<CourseListResponseDTO>> getInstructorActiveLectures() {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String memberId = currentUser.getUserId(); // JWT 토큰에서 사용자 ID 추출
            log.info("강사 {}의 활성 강의 목록 조회", memberId);
            
            List<CourseListResponseDTO> courses = courseService.getActiveCoursesByMemberId(memberId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("강사 활성 강의 목록 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // 강사가 담당하는 모든 강의 목록 조회 (활성/비활성 포함)
    @GetMapping("/lectures/all")
    public ResponseEntity<?> getAllInstructorLectures() {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String memberId = currentUser.getUserId();
            List<CourseListResponseDTO> courses = courseService.getAllCoursesByMemberId(memberId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강의 목록 조회 성공", courses));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "서버 오류: " + e.getMessage()));
        }
    }

    // 강의 상세 정보 조회
    @GetMapping("/lectures/{courseId}")
    public ResponseEntity<?> getLectureDetail(@PathVariable(name = "courseId") String courseId) {
        try {
            CourseDetailResponseDTO courseDetail = courseService.getCourseDetailById(courseId);
            return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강의 상세 조회 성공", courseDetail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ResponseDTO.createErrorResponse(404, "강의 ID " + courseId + "에 해당하는 강의를 찾을 수 없습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "강의 상세 조회 실패: " + e.getMessage()));
        }
    }

    // 강의별 학생 목록 조회
    @GetMapping("/lectures/{courseId}/students")
    public ResponseEntity<List<StudentResponseDTO>> getLectureStudents(@PathVariable(name = "courseId") String courseId) {
        return ResponseEntity.ok(new ArrayList<>());
    }

    // 강의별 출석 현황 조회
    @GetMapping("/lectures/{courseId}/attendance")
    public ResponseEntity<AttendanceResponseDTO> getLectureAttendance(@PathVariable(name = "courseId") String courseId, @RequestParam(name = "date") String date) {
        AttendanceResponseDTO attendance = AttendanceResponseDTO.builder()
                .courseId(courseId)
                .date(date)
                .totalStudents(0)
                .presentStudents(0)
                .absentStudents(0)
                .attendanceRate(0.0)
                .build();
        return ResponseEntity.ok(attendance);
    }

    // 강의 통계 정보 조회
    @GetMapping("/lectures/stats")
    public ResponseEntity<LectureStatsResponseDTO> getLectureStats() {
        try {
            CustomUserDetails currentUser = getCurrentUser();
            String memberId = currentUser.getUserId(); // JWT 토큰에서 사용자 ID 추출
            log.info("강사 {}의 강의 통계 조회", memberId);
            
            List<CourseListResponseDTO> courses = courseService.getAllCoursesByMemberId(memberId);
            
            int totalCourses = courses.size();
            int completedCourses = (int) courses.stream()
                    .filter(course -> course.getCourseEndDay().isBefore(java.time.LocalDate.now()))
                    .count();
            int ongoingCourses = (int) courses.stream()
                    .filter(course -> course.getCourseStartDay().isBefore(java.time.LocalDate.now()) 
                            && course.getCourseEndDay().isAfter(java.time.LocalDate.now()))
                    .count();
            
            LectureStatsResponseDTO stats = LectureStatsResponseDTO.builder()
                    .totalCourses(totalCourses)
                    .completedCourses(completedCourses)
                    .ongoingCourses(ongoingCourses)
                    .upcomingCourses(totalCourses - completedCourses - ongoingCourses)
                    .build();
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("강사 강의 통계 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // courseId와 memberId가 모두 일치하는 강의 리스트 조회
    // 사용하지 않는 메서드 - users.id → email → member → courseId → course 체인으로 대체됨
    // @GetMapping("/lectures/by-course-and-member")
    // public ResponseEntity<?> getCoursesByCourseIdAndMemberId(@RequestParam String courseId, @RequestParam String memberId) {
    //     try {
    //         log.info("courseId={}, memberId={}로 강의 리스트 조회", courseId, memberId);
    //         List<CourseListResponseDTO> courses = courseService.getCoursesByCourseIdAndMemberId(courseId, memberId);
    //         if (courses.isEmpty()) {
    //             String msg = "courseId(" + courseId + "), memberId(" + memberId + ")로 등록된 강의가 없습니다.";
    //             log.warn(msg);
    //             return ResponseEntity.ok(ResponseDTO.createSuccessResponse(msg, courses));
    //         }
    //         return ResponseEntity.ok(ResponseDTO.createSuccessResponse("강의 목록 조회 성공", courses));
    //     } catch (Exception e) {
    //         log.error("courseId, memberId로 강의 리스트 조회 실패", e);
    //         return ResponseEntity.status(500).body(ResponseDTO.createErrorResponse(500, "서버 오류: " + e.getMessage()));
    //     }
    // }

    // 테스트용 엔드포인트 - 모든 강의 조회
    @GetMapping("/test/all-courses")
    public ResponseEntity<String> testAllCourses() {
        try {
            List<CourseListResponseDTO> courses = courseService.getAllActiveCourses();
            return ResponseEntity.ok("강의 수: " + courses.size() + ", 강의 목록: " + courses.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("에러 발생: " + e.getMessage());
        }
    }

    // 테스트용 엔드포인트 - 특정 강사 강의 조회
    @GetMapping("/test/courses-by-member")
    public ResponseEntity<String> testCoursesByMember(@RequestParam String memberId) {
        try {
            log.info("=== 테스트: 특정 memberId로 강의 조회 ===");
            log.info("테스트 memberId: '{}'", memberId);
            log.info("memberId 타입: {}", memberId.getClass().getSimpleName());
            log.info("memberId 길이: {}", memberId.length());
            
            // Native Query 디버깅 실행
            courseService.debugMemberIdQuery(memberId);
            
            List<CourseListResponseDTO> courses = courseService.getAllCoursesByMemberId(memberId);
            log.info("조회된 강의 개수: {}", courses.size());
            
            if (!courses.isEmpty()) {
                log.info("첫 번째 강의 정보:");
                CourseListResponseDTO firstCourse = courses.get(0);
                log.info("  - courseId: '{}'", firstCourse.getCourseId());
                log.info("  - memberId: '{}'", firstCourse.getMemberId());
                log.info("  - courseName: '{}'", firstCourse.getCourseName());
            }
            
            String result = String.format("강사 %s의 강의 수: %d, 강의 목록: %s", 
                memberId, courses.size(), courses.toString());
            log.info("테스트 결과: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("테스트 중 에러 발생", e);
            return ResponseEntity.ok("에러 발생: " + e.getMessage());
        }
    }
} 