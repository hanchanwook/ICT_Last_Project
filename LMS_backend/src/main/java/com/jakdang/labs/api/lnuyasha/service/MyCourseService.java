package com.jakdang.labs.api.lnuyasha.service;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.lnuyasha.dto.CourseSubjectDTO;
import com.jakdang.labs.api.lnuyasha.dto.CourseSubDetailDTO;

import com.jakdang.labs.api.lnuyasha.repository.KyCourseRepository;
import com.jakdang.labs.api.lnuyasha.repository.KySubjectDetailRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubGroupRepository;

import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.entity.CourseEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCourseService {
    
    private final KyCourseRepository courseRepository;
    private final KySubjectDetailRepository subjectDetailRepository;
    private final MemberService memberService;
    private final SubGroupRepository subGroupRepository;
    
    /**
     * 강사별 과정 목록 조회
     * @param email 강사 이메일
     * @return 해당 강사의 과정 목록
     */
    public List<CourseDTO> getMyCourses(String email) {
        try {
            // 이메일로 사용자 정보 조회
        MemberInfoDTO memberInfo = memberService.getMemberInfoByEmail(email);
        if (memberInfo == null) {
                return new ArrayList<>();
            }
            
            String memberId = memberInfo.getMemberId();
            String educationId = memberInfo.getEducationId();
            log.info("[MY_COURSE] Found memberId: {} and educationId: {} for email: {}", memberId, educationId, email);
            
            // MemberInfoDTO의 모든 정보 로깅
            log.info("[MY_COURSE] Full MemberInfoDTO: memberId={}, educationId={}, memberName={}, memberEmail={}, memberRole={}, id={}", 
                     memberInfo.getMemberId(), 
                     memberInfo.getEducationId(), 
                     memberInfo.getMemberName(), 
                     memberInfo.getMemberEmail(), 
                     memberInfo.getMemberRole(), 
                     memberInfo.getId());
            
            // 강사의 과정 목록 조회 (courseActive = 0이고 memberId와 educationId가 일치하는 것만)
            // DB의 CourseEntity 테이블에 memberId와 educationId 값이 서로 바뀌어 저장되어 있으므로,
            // MemberInfoDTO에서 가져온 educationId를 courseRepository의 memberId 파라미터로,
            // MemberInfoDTO에서 가져온 memberId를 courseRepository의 educationId 파라미터로 전달합니다.
            List<Object[]> courseResults = courseRepository.findMyCoursesByMemberIdAndEducationId(educationId, memberId);
            log.info("[MY_COURSE] Found {} courses for memberId: {} and educationId: {}", courseResults.size(), memberId, educationId);
            
            // 첫 번째 쿼리가 실패하면 다른 쿼리들 시도
            if (courseResults.isEmpty()) {
                log.warn("[MY_COURSE] No courses found with combined query. Trying alternative queries...");
                
                // 1. memberId만으로 조회해보기
                List<Object[]> memberCourses = courseRepository.findMyCoursesByMemberId(memberId);
                log.info("[MY_COURSE] Courses found by memberId only: {}", memberCourses.size());
                if (!memberCourses.isEmpty()) {
                    courseResults = memberCourses;
                    log.info("[MY_COURSE] Using memberId-only query results");
                }
                
                // 2. educationId만으로 조회해보기
                if (courseResults.isEmpty()) {
                    List<Object[]> educationCourses = courseRepository.findCoursesByEducationId(educationId);
                    log.info("[MY_COURSE] Courses found by educationId only: {}", educationCourses.size());
                    if (!educationCourses.isEmpty()) {
                        courseResults = educationCourses;
                        log.info("[MY_COURSE] Using educationId-only query results");
                    }
                }
                
                // 3. 간단한 쿼리로 테스트 (memberId와 educationId를 바꿔서)
                if (courseResults.isEmpty()) {
                    List<CourseEntity> simpleCourses = courseRepository.findSimpleCoursesByMemberAndEducation(educationId, memberId);
                    log.info("[MY_COURSE] Simple query courses found: {}", simpleCourses.size());
                    if (!simpleCourses.isEmpty()) {
                        log.info("[MY_COURSE] First course details: courseId={}, courseName={}, memberId={}, educationId={}, courseActive={}", 
                                 simpleCourses.get(0).getCourseId(), 
                                 simpleCourses.get(0).getCourseName(),
                                 simpleCourses.get(0).getMemberId(),
                                 simpleCourses.get(0).getEducationId(),
                                 simpleCourses.get(0).getCourseActive());
                        
                        // CourseEntity를 Object[]로 변환하여 사용
                        courseResults = convertCourseEntitiesToObjectArray(simpleCourses);
                        log.info("[MY_COURSE] Using simple query results converted to Object[]");
                    }
                }
                
                // 4. 추가 디버깅 정보
                if (courseResults.isEmpty()) {
                    log.warn("[MY_COURSE] === DEBUGGING COURSE DATA ===");
                    
                    // 전체 과정 수 확인
                    List<CourseEntity> allCourses = courseRepository.findAllCourses();
                    log.info("[MY_COURSE] Total courses in database: {}", allCourses.size());
                    
                    // 해당 memberId의 모든 과정 (courseActive 무관) - DB의 memberId 컬럼에 memberInfo의 educationId가 들어있을 가능성
                    List<CourseEntity> memberAllCourses = courseRepository.findAllCoursesByMemberId(educationId);
                    log.info("[MY_COURSE] All courses for memberId (from DB's memberId column) {}: {}", educationId, memberAllCourses.size());
                    for (CourseEntity course : memberAllCourses) {
                        log.info("[MY_COURSE] Member course: courseId={}, courseName={}, educationId={}, courseActive={}", 
                                 course.getCourseId(), course.getCourseName(), course.getEducationId(), course.getCourseActive());
                    }
                    
                    // 해당 educationId의 모든 과정 (courseActive 무관) - DB의 educationId 컬럼에 memberInfo의 memberId가 들어있을 가능성
                    List<CourseEntity> educationAllCourses = courseRepository.findAllCoursesByEducationId(memberId);
                    log.info("[MY_COURSE] All courses for educationId (from DB's educationId column) {}: {}", memberId, educationAllCourses.size());
                    for (CourseEntity course : educationAllCourses) {
                        log.info("[MY_COURSE] Education course: courseId={}, courseName={}, memberId={}, courseActive={}", 
                                 course.getCourseId(), course.getCourseName(), course.getMemberId(), course.getCourseActive());
                    }
                }
            }
            
            // 과정별로 과목 정보를 그룹화하기 위한 Map
            java.util.Map<String, CourseDTO> courseMap = new java.util.HashMap<>();
            
            for (Object[] result : courseResults) {
                String courseId = (String) result[0];
                String courseName = (String) result[3];  // courseName
                String courseCode = (String) result[2];  // courseCode
                
                // 과목 정보 추출
                String subjectId = (String) result[12];  // subjectId
                String subjectName = (String) result[13];  // subjectName
                String subjectInfo = (String) result[14];  // subjectInfo
                
                // 상태 계산 (종료일 기준) - 안전한 타입 캐스팅
                LocalDate courseEndDay = null;
                Object endDayObj = result[9];  // courseEndDay
                if (endDayObj instanceof LocalDate) {
                    courseEndDay = (LocalDate) endDayObj;
                } else if (endDayObj instanceof String) {
                    try {
                        courseEndDay = LocalDate.parse((String) endDayObj);
                    } catch (Exception e) {
                        log.warn("[MY_COURSE] Failed to parse courseEndDay: {}", endDayObj);
                    }
                }
                String status = calculateStatus(courseEndDay);
                
                // 과정이 Map에 없으면 새로 생성
                if (!courseMap.containsKey(courseId)) {
                    CourseDTO course = CourseDTO.builder()
                        .courseId(courseId)
                        .courseName(courseName)
                        .courseCode(courseCode)
                        .status(status)
                        .subjects(new ArrayList<>())
                        .build();
                    courseMap.put(courseId, course);
                }
                
                // 과목 정보가 있으면 추가
                if (subjectId != null && subjectName != null) {
                    // 해당 과목에 연결된 세부과목 목록 조회
                    List<CourseSubDetailDTO> courseSubDetails = new ArrayList<>();
                    
                    // SubDetailGroupEntity 매핑 정보를 통해 해당 과목에 연결된 세부과목만 조회
                    List<Object[]> subDetailMappings = subjectDetailRepository.findSubDetailGroupMappings(educationId);
                    
                    for (Object[] mapping : subDetailMappings) {
                        String mappingSubjectId = (String) mapping[1]; // subjectId
                        String subDetailId = (String) mapping[0]; // subDetailId
                        String subDetailName = (String) mapping[3]; // subDetailName
                        
                        // 현재 과목에 해당하는 세부과목만 필터링
                        if (subjectId.equals(mappingSubjectId)) {
                            CourseSubDetailDTO courseSubDetail = CourseSubDetailDTO.builder()
                                .subDetailId(subDetailId)
                                .subDetailName(subDetailName)
                                .subDetailActive(0) // 활성 상태로 가정
                                .build();
                            courseSubDetails.add(courseSubDetail);
                        }
                    }
                    
                    CourseSubjectDTO subject = CourseSubjectDTO.builder()
                        .subjectId(subjectId)
                        .subjectName(subjectName)
                        .subjectInfo(subjectInfo != null ? subjectInfo : "")
                        .subjectActive(0)  // 기본값으로 활성 상태
                        .subDetails(courseSubDetails)
                        .build();
                    
                    courseMap.get(courseId).getSubjects().add(subject);
                }
                
                log.debug("[MY_COURSE] Processing course: courseId={}, courseName={}, subjectId={}, subjectName={}", 
                         courseId, courseName, subjectId, subjectName);
            }
            
            List<CourseDTO> courses = new ArrayList<>(courseMap.values());
            
            log.info("[MY_COURSE] My courses retrieval completed: {} courses", courses.size());
            return courses;
            
        } catch (Exception e) {
            log.error("[MY_COURSE] Error occurred while retrieving my courses: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 과정 상태 계산 (종료일 기준)
     * @param endDay 과정 종료일
     * @return 상태 문자열
     */
    private String calculateStatus(LocalDate endDay) {
        if (endDay == null) {
            return "미정";
        }
        
        LocalDate today = LocalDate.now();
        
        if (today.isBefore(endDay)) {
            return "진행중";
        } else {
            return "완료";
        }
    }
    
    /**
     * CourseEntity 리스트를 Object[] 배열로 변환
     * @param courses CourseEntity 리스트
     * @return Object[] 배열
     */
    private List<Object[]> convertCourseEntitiesToObjectArray(List<CourseEntity> courses) {
        List<Object[]> result = new ArrayList<>();
        
        for (CourseEntity course : courses) {
            Object[] row = new Object[15]; // 원본 쿼리와 동일한 컬럼 수
            
            row[0] = course.getCourseId();        // courseId
            row[1] = null;                        // memberName (null)
            row[2] = course.getCourseCode();      // courseCode
            row[3] = course.getCourseName();      // courseName
            row[4] = course.getMaxCapacity();     // maxCapacity
            row[5] = course.getMinCapacity();     // minCapacity
            row[6] = null;                        // classNumber (null)
            row[7] = course.getCourseStartDay();  // courseStartDay
            row[8] = course.getCourseEndDay();    // courseEndDay
            row[9] = course.getCourseEndDay();    // courseEndDay (for status calculation) - LocalDate 타입 유지
            row[10] = course.getCourseDays();     // courseDays
            row[11] = course.getStartTime();      // startTime
            row[12] = null;                       // subjectId (null)
            row[13] = null;                       // subjectName (null)
            row[14] = null;                       // subjectInfo (null)
            
            result.add(row);
        }
        
        return result;
    }

    /**
     * subGroupId로 과정 정보 조회
     * @param subGroupId 세부과목 그룹 ID
     * @return 과정 정보
     */
    public CourseDTO getCourseBySubGroupId(String subGroupId) {
        log.info("[COURSE_DETAIL] Request to get course by subGroupId: {}", subGroupId);
        try {
            log.info("[COURSE_DETAIL] Step 1: Finding SubGroupEntity by subGroupId: {}", subGroupId);
            var subGroup = subGroupRepository.findBySubGroupId(subGroupId);
            if (subGroup == null) {
                log.warn("[COURSE_DETAIL] SubGroup not found for subGroupId: {}", subGroupId);
                return null;
            }
            log.info("[COURSE_DETAIL] Step 1 SUCCESS: Found SubGroupEntity - subGroupId: {}, courseId: {}", 
                     subGroup.getSubGroupId(), subGroup.getCourseId());
            
            String courseId = subGroup.getCourseId();
            log.info("[COURSE_DETAIL] Step 2: Finding CourseEntity by courseId: {}", courseId);
            CourseEntity course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                log.warn("[COURSE_DETAIL] Course not found for courseId: {}", courseId);
                return null;
            }
            log.info("[COURSE_DETAIL] Step 2 SUCCESS: Found CourseEntity - courseId: {}, courseName: {}, courseCode: {}", 
                     course.getCourseId(), course.getCourseName(), course.getCourseCode());
            
            CourseDTO result = CourseDTO.builder()
                    .courseId(course.getCourseId())
                    .courseName(course.getCourseName())
                    .courseCode(course.getCourseCode())
                    .subGroupId(subGroupId)
                    .build();
            
            log.info("[COURSE_DETAIL] SUCCESS: Returning CourseDetailDTO - {}", result);
            return result;
        } catch (Exception e) {
            log.error("[COURSE_DETAIL] Error getting course by subGroupId: {}", e.getMessage(), e);
            throw new RuntimeException("과정 정보 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 디버깅용: 모든 subGroup 데이터 조회
     */
    public List<Object> getAllSubGroups() {
        log.info("[DEBUG] Getting all subGroup data");
        try {
            List<Object> result = new ArrayList<>();
            List<com.jakdang.labs.entity.SubGroupEntity> subGroups = subGroupRepository.findAll();
            
            for (com.jakdang.labs.entity.SubGroupEntity subGroup : subGroups) {
                var subGroupData = new java.util.HashMap<String, Object>();
                subGroupData.put("subGroupId", subGroup.getSubGroupId());
                subGroupData.put("courseId", subGroup.getCourseId());
                subGroupData.put("subjectId", subGroup.getSubjectId());
                result.add(subGroupData);
            }
            
            log.info("[DEBUG] Found {} subGroups", subGroups.size());
            return result;
        } catch (Exception e) {
            log.error("[DEBUG] Error getting all subGroups: {}", e.getMessage(), e);
            throw new RuntimeException("subGroup 데이터 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 