package com.jakdang.labs.api.chanwook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.time.LocalDate;
import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.api.chanwook.repository.InstructorCourseRepository;
import com.jakdang.labs.api.chanwook.repository.AttendanceRepository;

import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.AttendanceEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.SubGroupEntity;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InstructorService {

    private final InstructorMemberRepository instructorMemberRepository;
    private final InstructorCourseRepository instructorCourseRepository;
    private final AttendanceRepository attendanceRepository;



    // 강사가 담당하는 과정 목록 조회
    public List<Map<String, Object>> getInstructorCourses(String userId) {
        log.info("강사 담당 과정 목록 조회: {}", userId);
        List<CourseEntity> courses = instructorCourseRepository.findByMemberId(userId);
        return courses.stream()
            .filter(course -> course.getCourseActive() == 0) // 활성화된 과정만
            .map(course -> {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("courseId", course.getCourseId());
                courseMap.put("courseName", course.getCourseName());
                courseMap.put("educationId", course.getEducationId());
                
                // 강의 일정 정보 추가
                courseMap.put("courseStartDay", course.getCourseStartDay());
                courseMap.put("courseEndDay", course.getCourseEndDay());
                courseMap.put("courseDays", course.getCourseDays());
                courseMap.put("startTime", course.getStartTime());
                courseMap.put("endTime", course.getEndTime());
                courseMap.put("maxCapacity", course.getMaxCapacity());
                courseMap.put("minCapacity", course.getMinCapacity());
                
                return courseMap;
            })
            .toList();
    }

    // 강사가 담당하는 학생 목록 조회
    public Map<String, Object> getInstructorStudents(String userId, Map<String, String> params) {
        log.info("강사 담당 학생 목록 조회: {}, params: {}", userId, params);
        
        // 1. userId로 해당하는 MemberEntity 조회 (m.id = userId)
        List<MemberEntity> instructorMembers = instructorMemberRepository.findByIdField(userId);
        if (instructorMembers.isEmpty()) {
            throw new RuntimeException("강사를 찾을 수 없습니다: " + userId);
        }
        MemberEntity instructorMember = instructorMembers.get(0);
        log.info("userId {}로 찾은 MemberEntity: memberId={}", userId, instructorMember.getMemberId());
        
        // 2. 찾은 MemberEntity의 memberId 추출
        String instructorMemberId = instructorMember.getMemberId();
        List<String> memberIds = List.of(instructorMemberId);
        log.info("추출된 memberIds: {}", memberIds);
        
        // 3. memberId들로 해당하는 CourseEntity의 courseId 조회
        List<CourseEntity> courses = new ArrayList<>();
        for (String currentMemberId : memberIds) {
            List<CourseEntity> memberCourses = instructorCourseRepository.findByMemberId(currentMemberId);
            courses.addAll(memberCourses);
        }
        
        List<String> courseIds = courses.stream()
            .filter(course -> course.getCourseActive() == 0)
            .map(CourseEntity::getCourseId)
            .toList();
        log.info("찾은 courseIds: {}", courseIds);
        
        // 해당 과정의 학생들 조회
        List<MemberEntity> students = instructorMemberRepository.findByCourseIdInAndMemberRole(courseIds, "ROLE_STUDENT");
        
        List<Map<String, Object>> studentList = students.stream()
            .map(student -> {
                // 출석률 계산
                List<AttendanceEntity> attendances = attendanceRepository.findByUserId(student.getId());
                double attendanceRate = attendances.stream()
                    .mapToDouble(attendance -> "출석".equals(attendance.getAttendanceStatus()) ? 100.0 : 0.0)
                    .average()
                    .orElse(0.0);
                
                // 학생의 과정 정보 찾기
                String courseStartDay = "";
                String courseEndDay = "";
                String courseDays = "";
                if (student.getCourseId() != null) {
                    CourseEntity studentCourse = courses.stream()
                        .filter(course -> course.getCourseId().equals(student.getCourseId()))
                        .findFirst()
                        .orElse(null);
                    
                    if (studentCourse != null) {
                        courseStartDay = studentCourse.getCourseStartDay() != null ? studentCourse.getCourseStartDay().toString() : "";
                        courseEndDay = studentCourse.getCourseEndDay() != null ? studentCourse.getCourseEndDay().toString() : "";
                        courseDays = studentCourse.getCourseDays() != null ? studentCourse.getCourseDays() : "";
                    }
                }
                
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("userId", student.getId());
                studentMap.put("memberName", student.getMemberName());
                studentMap.put("memberEmail", student.getMemberEmail()); // 이메일 추가
                studentMap.put("courseName", getCourseNameByCourseId(student.getCourseId(), courses));
                studentMap.put("courseStartDay", courseStartDay); // 과정 시작일 추가
                studentMap.put("courseEndDay", courseEndDay); // 과정 종료일 추가
                studentMap.put("courseDays", courseDays); // 과정 요일 추가
                studentMap.put("attendanceRate", Math.round(attendanceRate * 100.0) / 100.0);
                return studentMap;
            })
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("students", studentList);
        response.put("totalCount", studentList.size());
        response.put("page", Integer.parseInt(params.getOrDefault("page", "1")));
        response.put("size", Integer.parseInt(params.getOrDefault("size", "10")));
        
        return response;
    }

    // 학생 상세 정보 조회
    public Map<String, Object> getStudentDetail(String userId) {
        log.info("학생 상세 정보 조회: userId={}", userId);
        
        try {
            // 1. userId로 해당하는 모든 MemberEntity 조회 (여러 memberId가 있을 수 있음)
            List<MemberEntity> studentMembers = instructorMemberRepository.findAll().stream()
                .filter(member -> userId.equals(member.getId()))
                .toList();
            
            if (studentMembers.isEmpty()) {
                throw new RuntimeException("학생을 찾을 수 없습니다: " + userId);
            }
            
            log.info("🔍 userId {}로 찾은 MemberEntity 수: {} 개", userId, studentMembers.size());
            
            // 2. 모든 학생 정보를 courses 배열로 변환
            List<Map<String, Object>> courses = new ArrayList<>();
            
            for (MemberEntity studentMember : studentMembers) {
                log.info("🔍 처리 중인 학생: memberName={}, courseId={}, memberId={}", 
                    studentMember.getMemberName(), studentMember.getCourseId(), studentMember.getMemberId());
                
                // 각 학생의 과정 정보 조회
                String courseName = "";
                String courseStartDay = "";
                String courseEndDay = "";
                String courseDays = "";
                
                if (studentMember.getCourseId() != null) {
                    CourseEntity course = instructorCourseRepository.findById(studentMember.getCourseId()).orElse(null);
                    if (course != null) {
                        courseName = course.getCourseName();
                        courseStartDay = course.getCourseStartDay() != null ? course.getCourseStartDay().toString() : "";
                        courseEndDay = course.getCourseEndDay() != null ? course.getCourseEndDay().toString() : "";
                        courseDays = course.getCourseDays() != null ? course.getCourseDays() : "";
                    }
                }
                
                // 각 과정별 출석률 계산
                List<AttendanceEntity> attendances = attendanceRepository.findByUserId(userId);
                double attendanceRate = attendances.stream()
                    .mapToDouble(attendance -> "출석".equals(attendance.getAttendanceStatus()) ? 100.0 : 0.0)
                    .average()
                    .orElse(0.0);
                
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("courseId", studentMember.getCourseId());
                courseInfo.put("courseName", courseName);
                courseInfo.put("courseStartDay", courseStartDay);
                courseInfo.put("courseEndDay", courseEndDay);
                courseInfo.put("courseDays", courseDays);
                courseInfo.put("memberId", studentMember.getMemberId());
                courseInfo.put("attendanceRate", Math.round(attendanceRate * 100.0) / 100.0);
                
                courses.add(courseInfo);
                log.info("✅ 과정 정보 추가: courseId={}, courseName={}", studentMember.getCourseId(), courseName);
            }
            
            // 3. 기본 학생 정보 (첫 번째 학생 기준)
            MemberEntity firstStudent = studentMembers.get(0);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", firstStudent.getId());
            result.put("memberName", firstStudent.getMemberName());
            result.put("memberEmail", firstStudent.getMemberEmail());
            result.put("memberPhone", firstStudent.getMemberPhone());
            result.put("memberBirthday", firstStudent.getMemberBirthday());
            result.put("memberAddress", firstStudent.getMemberAddress());
            result.put("educationId", firstStudent.getEducationId());
            result.put("courses", courses); // 모든 과정 정보를 배열로 반환
            
            log.info("📊 총 {}개의 과정 정보를 반환합니다", courses.size());
            return result;
            
        } catch (Exception e) {
            log.error("학생 상세 정보 조회 중 에러 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 특정 학생 성적 조회
    public Map<String, Object> getStudentGrades(String userId, Map<String, String> params) {
        log.info("학생 성적 조회: userId={}, params={}", userId, params);
        
        try {
            // 파라미터 추출
            String courseId = params.get("courseId");
            String educationId = params.get("educationId");
            
            log.info("🔍 파라미터 분석:");
            log.info("   - userId: {}", userId);
            log.info("   - courseId: {}", courseId);
            log.info("   - educationId: {}", educationId);
            
            // 1. 프론트에서 받은 userId로 MemberEntity에서 memberId 확인
            List<MemberEntity> studentMembers = instructorMemberRepository.findAll().stream()
                .filter(member -> userId.equals(member.getId()))
                .toList();
            
            if (studentMembers.isEmpty()) {
                throw new RuntimeException("학생을 찾을 수 없습니다: " + userId);
            }
            
            log.info("🔍 학생 성적 조회: userId={}, 찾은 MemberEntity 수={}", userId, studentMembers.size());
            
            // 2. 첫 번째 학생의 memberId 추출
            MemberEntity firstStudent = studentMembers.get(0);
            String memberId = firstStudent.getMemberId();
            log.info("✅ 추출된 memberId: {}", memberId);
            
            // 디버깅: 전체 ScoreStudentEntity 데이터 확인
            log.info("🔍 전체 ScoreStudentEntity 데이터 확인:");
            List<ScoreStudentEntity> allScoreStudents = instructorMemberRepository.findScoreStudentsByMemberId(memberId);
            log.info("   - 전체 ScoreStudentEntity 수: {}", allScoreStudents.size());
            for (ScoreStudentEntity ss : allScoreStudents) {
                log.info("   - ScoreStudent: memberId={}, templateId={}, score={}, isChecked={}", 
                    ss.getMemberId(), ss.getTemplateId(), ss.getScore(), ss.getIsChecked());
            }
            
            // 3. 해당 memberId와 동일한 데이터를 ScoreStudentEntity에서 조회
            List<ScoreStudentEntity> scoreStudents = instructorMemberRepository.findScoreStudentsByMemberId(memberId);
            log.info("🔍 ScoreStudentEntity 조회 결과: {} 건", scoreStudents.size());
            
            // 디버깅: TemplateEntity 데이터 확인
            log.info("🔍 전체 TemplateEntity 데이터 확인:");
            List<TemplateEntity> allTemplates = instructorMemberRepository.findTemplatesByMemberId(memberId);
            log.info("   - 전체 TemplateEntity 수: {}", allTemplates.size());
            for (TemplateEntity t : allTemplates) {
                log.info("   - Template: templateId={}, templateName={}, memberId={}, subGroupId={}", 
                    t.getTemplateId(), t.getTemplateName(), t.getMemberId(), t.getSubGroupId());
                
                // SubGroupEntity 조회하여 courseId 확인
                Optional<SubGroupEntity> subGroupOpt = instructorMemberRepository.findSubGroupById(t.getSubGroupId());
                if (subGroupOpt.isPresent()) {
                    SubGroupEntity subGroup = subGroupOpt.get();
                    log.info("   - SubGroup: subGroupId={}, courseId={}, subjectId={}", 
                        subGroup.getSubGroupId(), subGroup.getCourseId(), subGroup.getSubjectId());
                } else {
                    log.warn("   - SubGroupEntity를 찾을 수 없음: subGroupId={}", t.getSubGroupId());
                }
            }
            
            // 4. ScoreStudentEntity의 score, isChecked, templateId 추출하고 TemplateEntity 조회
            List<Map<String, Object>> gradeList = new ArrayList<>();
            
            for (ScoreStudentEntity scoreStudent : scoreStudents) {
                log.info("🔍 처리 중인 ScoreStudent: score={}, isChecked={}, templateId={}", 
                    scoreStudent.getScore(), scoreStudent.getIsChecked(), scoreStudent.getTemplateId());
                
                // 5. templateId로 TemplateEntity 조회
                Optional<TemplateEntity> templateOpt = instructorMemberRepository.findTemplateById(scoreStudent.getTemplateId());
                
                if (templateOpt.isPresent()) {
                    TemplateEntity template = templateOpt.get();
                    
                    // 6. SubGroupEntity 조회하여 courseId 확인
                    Optional<SubGroupEntity> subGroupOpt = instructorMemberRepository.findSubGroupById(template.getSubGroupId());
                    String templateCourseId = null;
                    String templateSubjectId = null;
                    
                    if (subGroupOpt.isPresent()) {
                        SubGroupEntity subGroup = subGroupOpt.get();
                        templateCourseId = subGroup.getCourseId();
                        templateSubjectId = subGroup.getSubjectId();
                        log.info("✅ SubGroup 조회 성공: courseId={}, subjectId={}", templateCourseId, templateSubjectId);
                    } else {
                        log.warn("⚠️ subGroupId {}에 해당하는 SubGroupEntity를 찾을 수 없음", template.getSubGroupId());
                    }
                    
                    // 7. courseId 매칭 확인 (파라미터로 받은 courseId와 비교)
                    boolean courseMatch = true;
                    if (courseId != null && templateCourseId != null) {
                        courseMatch = courseId.equals(templateCourseId);
                        log.info("🔍 courseId 매칭 확인: 파라미터={}, 템플릿={}, 매칭={}", 
                            courseId, templateCourseId, courseMatch);
                    }
                    
                    // 8. 매칭되는 경우만 성적 정보 추가
                    if (courseMatch) {
                        Map<String, Object> gradeInfo = new HashMap<>();
                        gradeInfo.put("score", scoreStudent.getScore());
                        gradeInfo.put("isChecked", scoreStudent.getIsChecked());
                        gradeInfo.put("templateId", scoreStudent.getTemplateId());
                        gradeInfo.put("templateName", template.getTemplateName());
                        gradeInfo.put("templateOpen", template.getTemplateOpen() != null ? template.getTemplateOpen().toString() : null);
                        gradeInfo.put("courseId", templateCourseId);
                        gradeInfo.put("subjectId", templateSubjectId);
                        
                        gradeList.add(gradeInfo);
                        log.info("✅ 성적 정보 추가: templateName={}, score={}, isChecked={}, courseId={}", 
                            template.getTemplateName(), scoreStudent.getScore(), scoreStudent.getIsChecked(), templateCourseId);
                    } else {
                        log.info("⚠️ courseId 매칭 실패로 성적 정보 제외: templateName={}", template.getTemplateName());
                    }
                } else {
                    log.warn("⚠️ templateId {}에 해당하는 TemplateEntity를 찾을 수 없음", scoreStudent.getTemplateId());
                }
            }
            
            // 6. 응답 데이터 구성
            Map<String, Object> result = new HashMap<>();
            result.put("userId", firstStudent.getId());
            result.put("studentName", firstStudent.getMemberName());
            result.put("studentEmail", firstStudent.getMemberEmail());
            result.put("memberId", memberId);
            result.put("grades", gradeList);
            result.put("totalGrades", gradeList.size());
            
            log.info("✅ 학생 성적 조회 완료: {} 건의 성적 정보 반환", gradeList.size());
            return result;
            
        } catch (Exception e) {
            log.error("학생 성적 조회 중 에러 발생: {}", e.getMessage(), e);
            throw e;
        }
    }


    // 강사 대시보드 통계 조회
    public Map<String, Object> getInstructorDashboard(String userId) {
        log.info("강사 대시보드 통계 조회: {}", userId);
        
        List<CourseEntity> courses = instructorCourseRepository.findByMemberId(userId);
        int totalCourses = (int) courses.stream()
            .filter(course -> course.getCourseActive() == 0)
            .count();
        
        Long totalStudents = instructorMemberRepository.countStudentsByUserId(userId);
        
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        
        List<String> courseIds = courses.stream()
            .filter(course -> course.getCourseActive() == 0)
            .map(CourseEntity::getCourseId)
            .toList();
        
        List<MemberEntity> students = instructorMemberRepository.findByCourseIdInAndMemberRole(courseIds, "ROLE_STUDENT");
        double averageAttendanceRate = students.stream()
            .mapToDouble(student -> {
                List<AttendanceEntity> attendances = attendanceRepository.findByUserId(student.getId());
                return attendances.stream()
                    .filter(attendance -> attendance.getLectureDate().isAfter(thirtyDaysAgo))
                    .mapToDouble(attendance -> "출석".equals(attendance.getAttendanceStatus()) ? 100.0 : 0.0)
                    .average()
                    .orElse(0.0);
            })
            .average()
            .orElse(0.0);
        
        // 최근 출석률 계산 (실제 데이터 기반)
        List<Map<String, Object>> recentAttendance = List.of();
        try {
            // 최근 2일간의 출석률 계산
            double yesterdayRate = students.stream()
                .mapToDouble(student -> {
                    List<AttendanceEntity> attendances = attendanceRepository.findByUserId(student.getId());
                    return attendances.stream()
                        .filter(attendance -> attendance.getLectureDate().equals(today.minusDays(1)))
                        .mapToDouble(attendance -> "출석".equals(attendance.getAttendanceStatus()) ? 100.0 : 0.0)
                        .average()
                        .orElse(0.0);
                })
                .average()
                .orElse(0.0);
                
            double twoDaysAgoRate = students.stream()
                .mapToDouble(student -> {
                    List<AttendanceEntity> attendances = attendanceRepository.findByUserId(student.getId());
                    return attendances.stream()
                        .filter(attendance -> attendance.getLectureDate().equals(today.minusDays(2)))
                        .mapToDouble(attendance -> "출석".equals(attendance.getAttendanceStatus()) ? 100.0 : 0.0)
                        .average()
                        .orElse(0.0);
                })
                .average()
                .orElse(0.0);
                
            recentAttendance = List.of(
                Map.of("date", today.minusDays(1).toString(), "attendanceRate", Math.round(yesterdayRate * 100.0) / 100.0),
                Map.of("date", today.minusDays(2).toString(), "attendanceRate", Math.round(twoDaysAgoRate * 100.0) / 100.0)
            );
        } catch (Exception e) {
            log.warn("최근 출석률 계산 실패: {}", e.getMessage());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalStudents", totalStudents != null ? totalStudents : 0);
        result.put("totalCourses", totalCourses);
        result.put("averageAttendanceRate", Math.round(averageAttendanceRate * 100.0) / 100.0);
        result.put("recentAttendance", recentAttendance);
        return result;
    }
    
    // 헬퍼 메서드: 과정 ID로 과정명 조회
    private String getCourseNameByCourseId(String courseId, List<CourseEntity> courses) {
        return courses.stream()
            .filter(course -> course.getCourseId().equals(courseId) && course.getCourseActive() == 0)
            .findFirst()
            .map(CourseEntity::getCourseName)
            .orElse("");
    }
} 