package com.jakdang.labs.api.cottonCandy.course.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.api.common.ResponseGetEducationDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.RequestCourseDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseCourseDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseStudentsListDTO;
import com.jakdang.labs.api.cottonCandy.course.service.CourseService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.entity.MemberEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
public class CourseController {
    private final CourseService courseService;
    private final EducationId education;
    
    // 과정 리스트 조회
    @GetMapping("/list")
    public ResponseDTO<List<ResponseCourseDTO>> getCourseList(@RequestParam(required = false) String userId) {
        // JWT에서 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = 
                (CustomUserDetails) authentication.getPrincipal();
            userEmail = userDetails.getUserEntity().getEmail();
        }
        
        // userId 파라미터가 있으면 해당 userId 사용, 없으면 JWT의 이메일로 멤버 조회
        String targetUserId = userId;
        if (userId == null || userId.isEmpty()) {
            if (userEmail != null) {
                // 이메일로 멤버 테이블에서 educationId 조회
                try {
                    Optional<ResponseGetEducationDTO> educationId = education.findByEmail(userEmail);
                    if (educationId.isPresent()) {
                        targetUserId = educationId.get().getUserId();
                    } else {
                        return ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다.");
                    }
                } catch (Exception e) {
                    return ResponseDTO.createErrorResponse(500, "사용자 정보 조회 실패: " + e.getMessage());
                }
            } else {
                return ResponseDTO.createErrorResponse(400, "사용자 정보를 찾을 수 없습니다.");
            }
        }
        
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(targetUserId);
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(400, "사용자 ID에 해당하는 교육기관 정보를 찾을 수 없습니다.");
            }
            
            List<ResponseCourseDTO> courseList = courseService.getCourseList(educationId.get().getEducationId());
            return ResponseDTO.createSuccessResponse("강의 리스트 조회 성공", courseList);
        } catch (Exception e) {
            return ResponseDTO.<List<ResponseCourseDTO>>createErrorResponse(500, "강의 리스트 조회 실패: " + e.getMessage());
        }
    }

    // 과정 생성
    @PostMapping("/create")
    public ResponseDTO<ResponseCourseDTO> createCourse(@RequestBody RequestCourseDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isEmpty()) {
            return ResponseDTO.createErrorResponse(400, "userId 파라미터가 필요합니다.");
        }
        try {
            Optional<ResponseGetEducationDTO> educationId = education.findById(dto.getUserId());
            if (educationId.isEmpty()) {
                return ResponseDTO.createErrorResponse(404, "해당 유저의 교육 ID를 찾을 수 없습니다.");
            }
            ResponseCourseDTO response = courseService.createCourse(dto, educationId.get().getEducationId());
            return ResponseDTO.createSuccessResponse("강의 생성 성공", response);
        } catch (Exception e) {
            return ResponseDTO.<ResponseCourseDTO>createErrorResponse(500, "강의 생성 실패: " + e.getMessage());
        }
    }

    // 과정 수정
    @Transactional
    @PostMapping("/update/{id}")
    public ResponseDTO<ResponseCourseDTO> updateCourse(@PathVariable String id, @RequestBody RequestCourseDTO dto) {
        try {
            // educationId를 올바르게 가져오기
            String educationId;
            if (dto.getEducationId() != null && !dto.getEducationId().isEmpty()) {
                educationId = dto.getEducationId();
            } else if (dto.getUserId() != null && !dto.getUserId().isEmpty()) {
                Optional<ResponseGetEducationDTO> educationInfo = education.findById(dto.getUserId());
                if (educationInfo.isEmpty()) {
                    return ResponseDTO.createErrorResponse(404, "해당 유저의 교육 ID를 찾을 수 없습니다.");
                }
                educationId = educationInfo.get().getEducationId();
            } else {
                return ResponseDTO.createErrorResponse(400, "educationId 또는 userId가 필요합니다.");
            }
            
            Boolean changeActive = courseService.updateCourse(id);
            if (changeActive) {
                ResponseCourseDTO response = courseService.createCourse(dto, educationId);
                return ResponseDTO.createSuccessResponse("강의 수정 성공", response);
            } else {
                return ResponseDTO.createSuccessResponse("강의 수정 실패. Active 변경 불가", null);
            }
        } catch (Exception e) {
            return ResponseDTO.<ResponseCourseDTO>createErrorResponse(500, "강의 수정 실패: " + e.getMessage());
        }
    }

    // 과정 삭제
    @PostMapping("/delete/{id}")
    public ResponseDTO<ResponseCourseDTO> deleteCourse (@PathVariable String id) {
        try {
                Boolean changeActive = courseService.updateCourse(id);
                if (changeActive) {                    
                    return ResponseDTO.createSuccessResponse("강의 삭제 성공", null);
                } else {
                    return ResponseDTO.createSuccessResponse("강의 삭제 실패. Active 변경 불가", null);
                }            
        } catch (Exception e) {
            return ResponseDTO.<ResponseCourseDTO>createErrorResponse(500, "강의 삭제 실패: " + e.getMessage());
        }
    }

    // educationId, classId, courseDays(요일), 시작/끝 날짜로 공통 예약 가능한 시간대 조회
    @GetMapping("/classroom")
    public ResponseDTO<Map<String, Object>> getCommonAvailableTimesByEducationId(
            @RequestParam("userId") String userId,
            @RequestParam("classId") String classId,
            @RequestParam("courseStartDay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate courseStartDay,
            @RequestParam("courseEndDay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate courseEndDay,
            @RequestParam("courseDays") List<String> courseDays) {
        Optional<ResponseGetEducationDTO> educationId = education.findById(userId);

        if (educationId.isEmpty()) {
            return ResponseDTO.createErrorResponse(404, "해당 유저의 교육 ID를 찾을 수 없습니다.");
        }

        if (classId == null || classId.isEmpty() || courseStartDay == null || courseEndDay == null || courseDays == null || courseDays.isEmpty()) {
            return ResponseDTO.createErrorResponse(400, "필수 파라미터 누락");
        }
        List<String> availableSlots = courseService.getCommonAvailableTimesByEducationId(educationId.get().getEducationId(), classId, courseStartDay, courseEndDay, courseDays);
        Map<String, Object> result = new HashMap<>();
        result.put("availableSlots", availableSlots);
        return ResponseDTO.createSuccessResponse("공통 예약 가능한 시간대 전체 조회 성공", result);
    }

  
    
    // 해당 기관 강사 조회
    @GetMapping("/teachers")
    public ResponseDTO<List<MemberEntity>> getTeachersByEducationId(@RequestParam("userId") String userId) {
        Optional<ResponseGetEducationDTO> educationId = education.findById(userId);

        if (educationId.isEmpty()) {
            return ResponseDTO.createErrorResponse(404, "해당 유저의 교육 ID를 찾을 수 없습니다.");
        }

        try {
            List<MemberEntity> teachers = courseService.getTeachersByEducationId(educationId.get().getEducationId());
            return ResponseDTO.createSuccessResponse("강사 조회 성공", teachers);
        } catch (Exception e) {
            return ResponseDTO.<List<MemberEntity>>createErrorResponse(500, "강사 조회 실패: " + e.getMessage());
        }
    }

    // 해당 강의 수강 학생 리스트
    @GetMapping("/students/{courseId}")
    public ResponseDTO<List<ResponseStudentsListDTO>> getStudentsByCourseId(@PathVariable String courseId) {
        List<ResponseStudentsListDTO> students = courseService.getStudentsByCourseId(courseId);
        return ResponseDTO.createSuccessResponse("학생 조회 성공", students);
    }


}
