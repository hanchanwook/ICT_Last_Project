package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.*;
import com.jakdang.labs.api.lnuyasha.repository.LectureHistoryRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyCourseRepository;
import com.jakdang.labs.api.lnuyasha.repository.SubGroupRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import com.jakdang.labs.api.lnuyasha.util.TimeZoneUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureHistoryService {
    
    private final LectureHistoryRepository lectureHistoryRepository;
    // private final TemplateQuestionRepository templateQuestionRepository;
    private final KyCourseRepository courseRepository;
    @Qualifier("lnuyashaSubGroupRepository")
    private final SubGroupRepository subGroupRepository;
    private final MemberRepository memberRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    
    /**
     * userId로 memberId 조회
     */
    private String getMemberIdByUserId(String userId) {

        
        try {
            MemberEntity member = memberRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId: " + userId));
            
            String memberId = member.getMemberId();
    
            
            return memberId;
        } catch (Exception e) {
            log.error("memberId 조회 실패 - userId: {}", userId, e);
            throw new IllegalArgumentException("사용자 정보 조회 실패: " + e.getMessage());
        }
    }
    
    /**
     * 강사가 담당한 모든 강의 이력 조회
     */
    public List<LectureHistoryDTO> getLecturesHistory(String instructorId) {

        
        // userId를 memberId로 변환
        String memberId = getMemberIdByUserId(instructorId);
        
        List<CourseEntity> courses = lectureHistoryRepository.findByInstructorId(memberId);
        
        return courses.stream()
                .map(this::convertToLectureHistoryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 필터링된 강의 목록 조회
     */
    public List<LectureHistoryDTO> getFilteredLectures(String instructorId, String searchTerm, String status) {

        
        // userId를 memberId로 변환
        String memberId = getMemberIdByUserId(instructorId);
        
        // 모든 강의 조회 후 Service에서 필터링
        List<CourseEntity> allCourses = lectureHistoryRepository.findByInstructorId(memberId);
        
        return allCourses.stream()
                .filter(course -> {
                    // 검색어 필터링
                    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                        String searchLower = searchTerm.toLowerCase();
                        boolean matchesSearch = (course.getCourseName() != null && course.getCourseName().toLowerCase().contains(searchLower)) ||
                                               (course.getCourseCode() != null && course.getCourseCode().toLowerCase().contains(searchLower));
                        if (!matchesSearch) return false;
                    }
                    
                    // 상태 필터링 (날짜 기준)
                    if (status != null && !status.equals("all")) {
                        String courseStatus = determineLectureStatus(course);
                        if (!courseStatus.equals(status)) return false;
                    }
                    
                    return true;
                })
                .map(this::convertToLectureHistoryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 강의 통계 정보 조회
     */
    public LectureStatisticsDTO getLecturesStatistics(String instructorId) {

        
        // userId를 memberId로 변환
        String memberId = getMemberIdByUserId(instructorId);
        
        Long totalLectures = lectureHistoryRepository.countTotalLecturesByInstructorId(memberId);
        Long completedLectures = lectureHistoryRepository.countCompletedLecturesByInstructorId(memberId);
        
        // 전체 평균 성적 계산 (실제 구현에서는 시험 결과 테이블에서 계산)
        Double overallAverageScore = calculateOverallAverageScore(memberId);
        
        // 총 수강생 수 계산 (실제 구현에서는 수강생 테이블에서 계산)
        Integer totalStudents = calculateTotalStudents(memberId);
        
        return LectureStatisticsDTO.builder()
                .totalLectures(totalLectures.intValue())
                .completedLectures(completedLectures.intValue())
                .totalStudents(totalStudents)
                .overallAverageScore(overallAverageScore)
                .build();
    }
    
    /**
     * 개별 강의 상세 정보 조회
     */
    public LectureDetailDTO getLectureDetail(String lectureId, String instructorId) {

        
        // userId를 memberId로 변환
        String memberId = getMemberIdByUserId(instructorId);
        
        CourseEntity course = lectureHistoryRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));
        
        // 강사 권한 확인
        if (!course.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 강의에 대한 접근 권한이 없습니다.");
        }
        
        return convertToLectureDetailDTO(course);
    }
    
    /**
     * 시험 강의 상세 정보 조회 (새로운 요구사항)
     */
    public ExamLectureDetailDTO getExamLectureDetail(String lectureId, String instructorId) {

        
        // userId를 memberId로 변환
        String memberId = getMemberIdByUserId(instructorId);
        
        CourseEntity course = lectureHistoryRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));
        
        // 강사 권한 확인
        if (!course.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 강의에 대한 접근 권한이 없습니다.");
        }
        
        return convertToExamLectureDetailDTO(course);
    }
    
    /**
     * CourseEntity를 LectureHistoryDTO로 변환
     */
    private LectureHistoryDTO convertToLectureHistoryDTO(CourseEntity course) {
        // 강의 상태 결정
        String status = determineLectureStatus(course);
        
        // 수강생 정보 계산 (실제 구현에서는 수강생 테이블에서 조회)
        Integer totalStudents = calculateTotalStudentsForLecture(course.getCourseId());
        Integer completedStudents = calculateCompletedStudentsForLecture(course.getCourseId());
        Integer dropoutStudents = totalStudents - completedStudents;
        
        // 평균 성적 계산 (실제 구현에서는 시험 결과 테이블에서 계산)
        Double averageScore = calculateAverageScoreForLecture(course.getCourseId());
        
        // 통과율 계산
        Integer passRate = calculatePassRateForLecture(course.getCourseId());
        
        // 총 강의 수 계산 (실제 구현에서는 강의 일정 테이블에서 계산)
        Integer totalClasses = calculateTotalClassesForLecture(course.getCourseId());
        Integer completedClasses = calculateCompletedClassesForLecture(course.getCourseId());
        
        // 성적 분포 계산
        LectureHistoryDTO.ScoreDistributionDTO scoreDistribution = calculateScoreDistribution(course.getCourseId());
        
        return LectureHistoryDTO.builder()
                .id(course.getCourseId()) // UUID의 해시코드를 양수 Long으로 변환
                .title(course.getCourseName())
                .code(course.getCourseCode())
                .courseStartDay(course.getCourseStartDay() != null ? 
                    course.getCourseStartDay().format(DATE_FORMATTER) : "날짜 미정")
                .courseEndDay(course.getCourseEndDay() != null ? 
                    course.getCourseEndDay().format(DATE_FORMATTER) : "날짜 미정")
                .year(course.getCourseStartDay() != null ? 
                    String.valueOf(course.getCourseStartDay().getYear()) : "2024")
                .status(status)
                .totalStudents(totalStudents)
                .completedStudents(completedStudents)
                .dropoutStudents(dropoutStudents)
                .averageScore(averageScore)
                .passRate(passRate)
                .totalClasses(totalClasses)
                .completedClasses(completedClasses)
                .scoreDistribution(scoreDistribution)
                .build();
    }
    
    /**
     * TemplateEntity를 LectureDetailDTO로 변환
     */
    private LectureDetailDTO convertToLectureDetailDTO(CourseEntity course) {
        // 기본 정보는 LectureHistoryDTO와 동일하게 계산
        LectureHistoryDTO basicInfo = convertToLectureHistoryDTO(course);
        
        return LectureDetailDTO.builder()
                .id(basicInfo.getId())
                .title(basicInfo.getTitle())
                .code(basicInfo.getCode())
                .description("강의 설명") // 실제 구현에서는 별도 필드에서 조회
                .courseStartDay(basicInfo.getCourseStartDay())
                .courseEndDay(basicInfo.getCourseEndDay())
                .year(basicInfo.getYear())
                .status(basicInfo.getStatus())
                .instructorName("강사명") // 실제 구현에서는 사용자 테이블에서 조회
                .instructorId(course.getMemberId()) // memberId 사용
                .totalStudents(basicInfo.getTotalStudents())
                .completedStudents(basicInfo.getCompletedStudents())
                .dropoutStudents(basicInfo.getDropoutStudents())
                .averageScore(basicInfo.getAverageScore())
                .passRate(basicInfo.getPassRate())
                .totalClasses(basicInfo.getTotalClasses())
                .completedClasses(basicInfo.getCompletedClasses())
                .createdAt(TimeZoneUtil.toKoreanTime(course.getCreatedAt()))
                .updatedAt(TimeZoneUtil.toKoreanTime(course.getUpdatedAt()))
                .students(getStudentInfoList(course.getCourseId()))
                .classes(getClassInfoList(course.getCourseId()))
                .scoreDistribution(convertToDetailScoreDistribution(basicInfo.getScoreDistribution()))
                .build();
    }
    
    /**
     * 강의 상태 결정 (실제 구현)
     */
    private String determineLectureStatus(CourseEntity course) {
        LocalDate now = LocalDate.now();
        LocalDate startDate = course.getCourseStartDay();
        LocalDate endDate = course.getCourseEndDay();
        
        if (startDate != null && endDate != null) {
            if (now.isBefore(startDate)) {
                return "예정";
            } else if (now.isAfter(endDate)) {
                return "완료";
            } else {
                return "진행중";
            }
        } else {
            // 날짜 정보가 없으면 기본값
            return "예정";
        }
    }
    

    
    /**
     * 강의별 총 수강생 수 계산 (실제 구현)
     */
    private Integer calculateTotalStudentsForLecture(String courseId) {
        try {
            return lectureHistoryRepository.countStudentsByCourseId(courseId);
        } catch (Exception e) {
            log.warn("수강생 수 조회 실패 - courseId: {}, 기본값 사용", courseId, e);
            return 0;
        }
    }
    
    /**
     * 강의별 완료 수강생 수 계산 (실제 구현)
     */
    private Integer calculateCompletedStudentsForLecture(String courseId) {
        try {
            return lectureHistoryRepository.countCompletedStudentsByCourseId(courseId);
        } catch (Exception e) {
            log.warn("완료 수강생 수 조회 실패 - courseId: {}, 기본값 사용", courseId, e);
            return 0;
        }
    }
    
    /**
     * 강의별 평균 성적 계산 (실제 구현)
     */
    private Double calculateAverageScoreForLecture(String courseId) {
        try {
            // 학생별 평균 점수를 조회하여 전체 평균 계산
            List<Object[]> studentAverageScores = lectureHistoryRepository.findStudentAverageScoresByCourseId(courseId);
            
            if (studentAverageScores.isEmpty()) {
                return null; // 시험을 본 학생이 없으면 null 반환
            }
            
            double totalScore = 0.0;
            int studentCount = 0;
            
            for (Object[] result : studentAverageScores) {
                Double averageScore = (Double) result[1];
                if (averageScore != null) {
                    totalScore += averageScore;
                    studentCount++;
                }
            }
            
            if (studentCount == 0) {
                return null; // 유효한 점수가 없으면 null 반환
            }
            
            return totalScore / studentCount;
        } catch (Exception e) {
            log.warn("평균 성적 조회 실패 - courseId: {}, null 반환", courseId, e);
            return null;
        }
    }
    
    /**
     * 강의별 통과율 계산 (실제 구현)
     */
    private Integer calculatePassRateForLecture(String courseId) {
        try {
            // 학생별 평균 점수를 조회하여 합격률 계산
            List<Object[]> studentAverageScores = lectureHistoryRepository.findStudentAverageScoresByCourseId(courseId);
            
            if (studentAverageScores.isEmpty()) {
                return null; // 시험을 본 학생이 없으면 null 반환
            }
            
            int passedStudents = 0;
            int totalStudents = 0;
            
            for (Object[] result : studentAverageScores) {
                Double averageScore = (Double) result[1];
                if (averageScore != null) {
                    totalStudents++;
                    if (averageScore >= 60) { // 60점 이상을 합격으로 간주
                        passedStudents++;
                    }
                }
            }
            
            if (totalStudents == 0) {
                return null; // 유효한 점수가 없으면 null 반환
            }
            
            return (int) Math.round((double) passedStudents / totalStudents * 100);
        } catch (Exception e) {
            log.warn("통과율 계산 실패 - courseId: {}, null 반환", courseId, e);
            return null;
        }
    }
    
    /**
     * 강의별 총 시험 수 계산 (실제 구현)
     */
    private Integer calculateTotalClassesForLecture(String courseId) {
        try {
            return lectureHistoryRepository.countTotalExamsByCourseId(courseId);
        } catch (Exception e) {
            log.warn("총 시험 수 조회 실패 - courseId: {}, 기본값 사용", courseId, e);
            return 0;
        }
    }
    
    /**
     * 강의별 완료된 시험 수 계산 (실제 구현)
     */
    private Integer calculateCompletedClassesForLecture(String courseId) {
        try {
            return lectureHistoryRepository.countCompletedExamsByCourseId(courseId);
        } catch (Exception e) {
            log.warn("완료된 시험 수 조회 실패 - courseId: {}, 기본값 사용", courseId, e);
            return 0;
        }
    }
    
    /**
     * 성적 분포 계산 (실제 구현)
     */
    private LectureHistoryDTO.ScoreDistributionDTO calculateScoreDistribution(String courseId) {
        try {
            // 학생별 평균 점수를 계산하여 성적 분포를 구함
            List<Object[]> studentAverageScores = lectureHistoryRepository.findStudentAverageScoresByCourseId(courseId);
            
            int outstanding = 0;    // 90점 이상
            int excellent = 0;      // 80-89점
            int good = 0;          // 70-79점
            int average = 0;       // 60-69점
            int needsImprovement = 0; // 60점 미만
            
            for (Object[] result : studentAverageScores) {
                String memberId = (String) result[0];
                Double averageScore = (Double) result[1];
                
                if (averageScore != null) {
                    if (averageScore >= 90) {
                        outstanding++;
                    } else if (averageScore >= 80) {
                        excellent++;
                    } else if (averageScore >= 70) {
                        good++;
                    } else if (averageScore >= 60) {
                        average++;
                    } else {
                        needsImprovement++;
                    }
                }
            }
            
            // 검증: 분포 합계가 총 학생수와 일치하는지 확인
            int totalDistribution = outstanding + excellent + good + average + needsImprovement;
            int totalStudents = calculateTotalStudentsForLecture(courseId);
            
            if (totalDistribution != totalStudents) {
                log.warn("Score distribution sum ({}) doesn't match total students ({}) for courseId: {}", 
                         totalDistribution, totalStudents, courseId);
            }
            
            return LectureHistoryDTO.ScoreDistributionDTO.builder()
                    .outstanding(outstanding)
                    .excellent(excellent)
                    .good(good)
                    .average(average)
                    .needsImprovement(needsImprovement)
                    .build();
                    
        } catch (Exception e) {
            log.warn("성적 분포 계산 실패 - courseId: {}, 기본값 사용", courseId, e);
            return LectureHistoryDTO.ScoreDistributionDTO.builder()
                    .outstanding(0)
                    .excellent(0)
                    .good(0)
                    .average(0)
                    .needsImprovement(0)
                    .build();
        }
    }
    
    /**
     * 전체 평균 성적 계산 (실제 구현)
     */
    private Double calculateOverallAverageScore(String instructorId) {
        try {
            // 실제 ScoreStudentEntity에서 전체 평균 성적 계산
            // 강사가 담당한 모든 과정의 시험 점수 평균
            return lectureHistoryRepository.calculateOverallAverageScoreByInstructorId(instructorId);
        } catch (Exception e) {
            log.warn("전체 평균 성적 계산 실패 - instructorId: {}, 기본값 사용", instructorId, e);
            return 0.0;
        }
    }
    
    /**
     * 총 수강생 수 계산 (실제 구현)
     */
    private Integer calculateTotalStudents(String instructorId) {
        try {
            // 실제 MemberEntity에서 강사가 담당한 모든 과정의 수강생 수 계산
            return lectureHistoryRepository.countTotalStudentsByInstructorId(instructorId);
        } catch (Exception e) {
            log.warn("총 수강생 수 계산 실패 - instructorId: {}, 기본값 사용", instructorId, e);
            return 0;
        }
    }
    
    /**
     * 수강생 정보 목록 조회 (실제 구현)
     */
    private List<LectureDetailDTO.StudentInfoDTO> getStudentInfoList(String courseId) {
        try {
            // CourseEntity 조회
            CourseEntity course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                log.warn("과정 정보를 찾을 수 없음 - courseId: {}", courseId);
                return new ArrayList<>();
            }
            
            // 실제 MemberEntity에서 과정의 학생 목록 조회
            List<MemberEntity> students = lectureHistoryRepository.findStudentsByCourseId(courseId);
            List<LectureDetailDTO.StudentInfoDTO> studentInfoList = new ArrayList<>();
            
            for (MemberEntity student : students) {
                // 학생별 평균 점수 계산
                Double score = lectureHistoryRepository.calculateAverageScoreByStudentAndCourse(student.getMemberId(), courseId);
                // 학생별 출석률 (실제 출석 데이터는 별도 테이블에서 조회 필요)
                int attendanceRate = 0;
                // 학생별 등록일
                LocalDateTime enrolledAt = TimeZoneUtil.toKoreanTime(student.getCreatedAt());
                
                // 학생 상태 판단 로직
                String studentStatus = determineStudentStatus(student, course);
                
                studentInfoList.add(LectureDetailDTO.StudentInfoDTO.builder()
                        .studentId(student.getMemberId())
                        .studentName(student.getMemberName())
                        .status(studentStatus)
                        .score(score) // null이면 null로 전송
                        .attendanceRate(attendanceRate)
                        .enrolledAt(enrolledAt)
                        .build());
            }
            
            return studentInfoList;
        } catch (Exception e) {
            log.warn("수강생 정보 목록 조회 실패 - courseId: {}, 빈 목록 반환", courseId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 강의 일정 정보 목록 조회 (실제 구현)
     */
    private List<LectureDetailDTO.ClassInfoDTO> getClassInfoList(String courseId) {
        try {
            // 과정별 활성화된 템플릿 목록 조회 (templateActive = 0인 것만)
            List<TemplateEntity> activeTemplates = lectureHistoryRepository.findActiveTemplatesByCourseId(courseId);
            List<LectureDetailDTO.ClassInfoDTO> classInfoList = new ArrayList<>();
            
            for (int i = 0; i < activeTemplates.size(); i++) {
                TemplateEntity template = activeTemplates.get(i);
                
                // 각 템플릿별 응시자 수 계산
                int attendanceCount = lectureHistoryRepository.countTotalStudentsByTemplateId(template.getTemplateId());
                
                // 템플릿 상태 결정
                String status = template.getTemplateActive() == 0 ? "활성" : "비활성";
                
                classInfoList.add(LectureDetailDTO.ClassInfoDTO.builder()
                        .classId((long) (i + 1))
                        .className(template.getTemplateName())
                        .classDate(TimeZoneUtil.toKoreanTime(template.getCreatedAt()))
                        .status(status)
                        .attendanceCount(attendanceCount)
                        .notes("템플릿 정보")
                        .templateId(template.getTemplateId())
                        .templateName(template.getTemplateName())
                        .templateOpen(TimeZoneUtil.toKoreanTime(template.getTemplateOpen()))
                        .templateClosed(TimeZoneUtil.toKoreanTime(template.getTemplateClose()))
                        .build());
            }
            
            return classInfoList;
        } catch (Exception e) {
            log.warn("강의 일정 정보 목록 조회 실패 - courseId: {}, 빈 목록 반환", courseId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * ScoreDistributionDTO 변환
     */
    private LectureDetailDTO.ScoreDistributionDTO convertToDetailScoreDistribution(LectureHistoryDTO.ScoreDistributionDTO source) {
        return LectureDetailDTO.ScoreDistributionDTO.builder()
                .outstanding(source.getOutstanding())
                .excellent(source.getExcellent())
                .good(source.getGood())
                .average(source.getAverage())
                .needsImprovement(source.getNeedsImprovement())
                .build();
    }
    
    /**
     * CourseEntity를 ExamLectureDetailDTO로 변환
     */
    private ExamLectureDetailDTO convertToExamLectureDetailDTO(CourseEntity course) {
        // 기본 정보는 LectureHistoryDTO와 동일하게 계산
        LectureHistoryDTO basicInfo = convertToLectureHistoryDTO(course);
        
        // 기간 계산
        String period = "";
        if (course.getCourseStartDay() != null && course.getCourseEndDay() != null) {
            period = course.getCourseStartDay().format(DATE_FORMATTER) + " - " + course.getCourseEndDay().format(DATE_FORMATTER);
        } else if (course.getCourseStartDay() != null) {
            period = course.getCourseStartDay().format(DATE_FORMATTER) + " - 진행중";
        } else {
            period = "날짜 미정";
        }
        
        return ExamLectureDetailDTO.builder()
                .id(basicInfo.getId())
                .title(basicInfo.getTitle())
                .code(basicInfo.getCode())
                .period(period)
                .status(basicInfo.getStatus())
                .totalStudents(basicInfo.getTotalStudents())
                .completedStudents(basicInfo.getCompletedStudents())
                .dropoutStudents(basicInfo.getDropoutStudents())
                .averageScore(basicInfo.getAverageScore())
                .passRate(basicInfo.getPassRate())
                .satisfactionScore(0.0) // 실제 만족도 점수는 별도 테이블에서 조회 필요
                .totalClasses(basicInfo.getTotalClasses())
                .completedClasses(basicInfo.getCompletedClasses())
                .category("") // 실제 분야는 별도 테이블에서 조회 필요
                .level("") // 실제 수준은 별도 테이블에서 조회 필요
                .scoreDistribution(convertToExamDetailScoreDistribution(basicInfo.getScoreDistribution()))
                .examResults(getExamResultsList(course.getCourseId()))
                .students(getExamStudentInfoList(course.getCourseId()))
                .studentSubjects(getStudentSubjectsMap(course.getCourseId()))
                .build();
    }
    
    /**
     * 시험 강의용 학생 정보 목록 조회
     */
    private List<ExamLectureDetailDTO.StudentInfoDTO> getExamStudentInfoList(String courseId) {
        try {
            // CourseEntity 조회
            CourseEntity course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                log.warn("과정 정보를 찾을 수 없음 - courseId: {}", courseId);
                return new ArrayList<>();
            }
            
            // 실제 MemberEntity에서 과정의 학생 목록 조회
            List<MemberEntity> students = lectureHistoryRepository.findStudentsByCourseId(courseId);
            List<ExamLectureDetailDTO.StudentInfoDTO> studentInfoList = new ArrayList<>();
            
            for (MemberEntity student : students) {
                // 학생별 평균 점수 계산
                Double finalScore = lectureHistoryRepository.calculateAverageScoreByStudentAndCourse(student.getMemberId(), courseId);
                // 학생별 과제 점수 (실제 과제 데이터는 별도 테이블에서 조회 필요)
                Double assignmentScore = 0.0;
                // 학생별 과제 제출 수 (실제 과제 데이터는 별도 테이블에서 조회 필요)
                int submittedAssignments = 0;
                int totalAssignments = 0;
                int lateSubmissions = 0;
                // 학생별 참여도 점수 (실제 참여도 데이터는 별도 테이블에서 조회 필요)
                Double participationScore = 0.0;
                // 학생별 합격률 (실제 합격률 데이터는 별도 테이블에서 조회 필요)
                int passRate = 0;
                
                // 학생 상태 판단 로직
                String studentStatus = determineStudentStatus(student, course);
                
                studentInfoList.add(ExamLectureDetailDTO.StudentInfoDTO.builder()
                        .id(student.getMemberId())
                        .name(student.getMemberName())
                        .status(studentStatus)
                        .finalScore(finalScore) // null이면 null로 전송
                        .assignmentScore(assignmentScore != null ? assignmentScore : 0.0)
                        .grade(finalScore != null ? getGradeByScore(finalScore) : null)
                        .rank(0) // 석차는 별도 계산 필요
                        .submittedAssignments(submittedAssignments)
                        .totalAssignments(totalAssignments)
                        .lateSubmissions(lateSubmissions)
                        .participationScore(participationScore)
                        .passRate(passRate)
                        .build());
            }
            
            return studentInfoList;
        } catch (Exception e) {
            log.warn("시험 강의용 학생 정보 목록 조회 실패 - courseId: {}, 빈 목록 반환", courseId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 시험 결과 목록 조회
     */
    private List<ExamLectureDetailDTO.ExamResultDTO> getExamResultsList(String courseId) {
        try {
            // 실제 TemplateEntity에서 해당 과정의 시험 목록 조회
            List<TemplateEntity> templates = lectureHistoryRepository.findTemplatesByCourseId(courseId);
            List<ExamLectureDetailDTO.ExamResultDTO> examResults = new ArrayList<>();
            
            for (TemplateEntity template : templates) {
                // 각 시험별 평균 점수 계산
                Double averageScore = lectureHistoryRepository.calculateAverageScoreByTemplateId(template.getTemplateId());
                // 각 시험별 합격자 수 계산
                int passCount = lectureHistoryRepository.countPassedStudentsByTemplateId(template.getTemplateId());
                // 각 시험별 전체 응시자 수 계산
                int totalCount = lectureHistoryRepository.countTotalStudentsByTemplateId(template.getTemplateId());
                
                examResults.add(ExamLectureDetailDTO.ExamResultDTO.builder()
                        .examName(template.getTemplateName())
                        .subject(template.getTemplateName()) // templateName을 과목명으로 사용
                        .average(averageScore != null ? averageScore : 0.0)
                        .passCount(passCount)
                        .totalCount(totalCount)
                        .build());
            }
            
            return examResults;
        } catch (Exception e) {
            log.warn("시험 결과 목록 조회 실패 - courseId: {}, 빈 목록 반환", courseId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 학생별 과목 성적 맵 조회
     */
    private Map<String, List<ExamLectureDetailDTO.StudentSubjectDTO>> getStudentSubjectsMap(String courseId) {
        try {
            // 실제 ScoreStudentEntity에서 학생별 과목 성적 조회
            Map<String, List<ExamLectureDetailDTO.StudentSubjectDTO>> studentSubjects = new java.util.HashMap<>();
            
            // 과정의 모든 시험 조회
            List<TemplateEntity> templates = lectureHistoryRepository.findTemplatesByCourseId(courseId);
            
            for (TemplateEntity template : templates) {
                // 각 시험의 학생별 점수 조회
                List<ScoreStudentEntity> scoreStudents = lectureHistoryRepository.findScoreStudentsByTemplateId(template.getTemplateId());
                
                for (ScoreStudentEntity scoreStudent : scoreStudents) {
                    String studentId = scoreStudent.getMemberId();
                    
                    // 학생별 과목 성적 리스트 생성 또는 기존 리스트 가져오기
                    List<ExamLectureDetailDTO.StudentSubjectDTO> subjects = studentSubjects.getOrDefault(studentId, new ArrayList<>());
                    
                    subjects.add(ExamLectureDetailDTO.StudentSubjectDTO.builder()
                            .subject(template.getTemplateName()) // templateName을 과목명으로 사용
                            .score((double) scoreStudent.getScore())
                            .grade(getGradeByScore((double) scoreStudent.getScore()))
                            .build());
                    
                    studentSubjects.put(studentId, subjects);
                }
            }
            
            return studentSubjects;
        } catch (Exception e) {
            log.warn("학생별 과목 성적 맵 조회 실패 - courseId: {}, 빈 맵 반환", courseId, e);
            return new java.util.HashMap<>();
        }
    }
    
    /**
     * 학생 상태 판단
     */
    private String determineStudentStatus(MemberEntity student, CourseEntity course) {
        // memberExpired 필드를 기반으로 상태 판단
        if (student.getMemberExpired() == null) {
            // 현재 날짜 기준으로 수업 상태 판단
            LocalDate now = LocalDate.now();
            LocalDate startDate = course.getCourseStartDay();
            LocalDate endDate = course.getCourseEndDay();
            
            if (startDate != null && endDate != null) {
                if (now.isBefore(startDate)) {
                    return "수강예정";
                } else if (now.isAfter(endDate)) {
                    return "완료";
                } else {
                    return "수강중";
                }
            } else {
                // 날짜 정보가 없으면 기본값
                return "수강중";
            }
        } else {
            // 만료일이 있으면 중도탈락
            return "중도탈락";
        }
    }
    
    /**
     * 점수에 따른 등급 반환
     */
    private String getGradeByScore(Double score) {
        if (score == null) return null;
        if (score >= 90) return "탁월";
        else if (score >= 80) return "우수";
        else if (score >= 70) return "양호";
        else if (score >= 60) return "보통";
        else return "노력";
    }
    
    /**
     * ExamLectureDetailDTO용 ScoreDistributionDTO 변환
     */
    private ExamLectureDetailDTO.ScoreDistributionDTO convertToExamDetailScoreDistribution(LectureHistoryDTO.ScoreDistributionDTO source) {
        return ExamLectureDetailDTO.ScoreDistributionDTO.builder()
                .outstanding(source.getOutstanding())
                .excellent(source.getExcellent())
                .good(source.getGood())
                .average(source.getAverage())
                .needsImprovement(source.getNeedsImprovement())
                .build();
    }
} 