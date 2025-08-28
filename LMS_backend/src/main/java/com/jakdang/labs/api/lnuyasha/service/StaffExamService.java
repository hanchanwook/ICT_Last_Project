package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.lnuyasha.dto.StaffCourseGradeDTO;
import com.jakdang.labs.api.lnuyasha.dto.StaffExamDetailDTO;
import com.jakdang.labs.api.lnuyasha.dto.StaffExamListDTO;
import com.jakdang.labs.api.lnuyasha.dto.QuestionDTO;
import com.jakdang.labs.api.lnuyasha.repository.KyCourseRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.ScoreStudentRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.TemplateQuestionEntity;
import com.jakdang.labs.entity.QuestionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 강사용 시험 시스템 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffExamService {
    
    private final KyCourseRepository courseRepository;
    private final KyMemberRepository memberRepository;
    private final TemplateRepository templateRepository;
    private final com.jakdang.labs.api.lnuyasha.repository.AnswerRepository answerRepository;
    private final ScoreStudentRepository scoreStudentRepository;
    private final com.jakdang.labs.api.lnuyasha.repository.TemplateQuestionRepository templateQuestionRepository;
    private final com.jakdang.labs.api.lnuyasha.repository.QuestionRepository questionRepository;
    
    /**
     * 강사가 담당하는 과정 목록 조회 (시험 관리용)
     * @param email 강사 이메일
     * @return 해당 학원의 모든 과정 목록
     */
    public List<CourseDTO> getStaffCourses(String email) {
        log.info("강사 과정 정보 조회: email = {}", email);
        
        try {
            // 1. 이메일로 강사 정보 조회하여 학원(educationId) 확인
            List<MemberEntity> members = memberRepository.findByMemberEmail(email);
            if (members.isEmpty()) {
                log.warn("이메일 {}에 해당하는 강사 정보가 없습니다.", email);
                return new ArrayList<>();
            }
            
            // 강사 정보에서 educationId 추출
            MemberEntity instructor = members.get(0);
            String educationId = instructor.getEducationId();
            
            log.info("강사 정보: email = {}, educationId = {}, memberName = {}", 
                    email, educationId, instructor.getMemberName());
            
            // 2. 해당 학원의 모든 과정 목록 조회
            List<CourseEntity> courses = courseRepository.findByEducationId(educationId);
            log.info("학원 ID {}로 조회된 과정 수: {}", educationId, courses.size());
            
            if (courses.isEmpty()) {
                log.warn("학원 ID {}에 해당하는 과정이 없습니다.", educationId);
                return new ArrayList<>();
            }
            
            // 3. 각 과정별 상세 정보 조회 및 DTO 변환
            List<CourseDTO> courseDTOs = courses.stream()
                    .map(course -> {
                        // 과정의 소그룹 ID 목록 조회
                        List<String> subGroupIds = courseRepository.findSubGroupIdsByCourseIds(List.of(course.getCourseId()));
                        
                        // 각 소그룹별 시험 개수 계산
                        int examCount = 0;
                        for (String subGroupId : subGroupIds) {
                            List<TemplateEntity> templates = 
                                templateRepository.findBySubGroupIdAndTemplateActive(subGroupId, 0);
                            examCount += templates.size();
                        }
                        
                        // 강의실 번호 조회
                        String classNumber = getClassNumber(course.getClassId());
                        
                        // 과정 담당 강사 정보 조회
                        String courseInstructorName = getMemberName(course.getMemberId());
                        
                        // 수강생 수 조회
                        Integer studentCount = getStudentCount(course.getCourseId());
                        
                        // 시험 평균 점수 계산
                        Double averageScore = calculateAverageScore(course.getCourseId());
                        
                        return CourseDTO.builder()
                                .courseId(course.getCourseId())
                                .courseName(course.getCourseName())
                                .courseCode(course.getCourseCode())
                                .memberId(course.getMemberId())
                                .memberName(courseInstructorName)
                                .educationId(course.getEducationId())
                                .courseStartDay(course.getCourseStartDay())
                                .courseEndDay(course.getCourseEndDay())
                                .startTime(LocalTime.parse(course.getStartTime()))
                                .endTime(LocalTime.parse(course.getEndTime()))
                                .courseDays(course.getCourseDays())
                                .maxCapacity(course.getMaxCapacity())
                                .minCapacity(course.getMinCapacity())
                                .classId(course.getClassId())
                                .classNumber(classNumber)
                                .courseActive(course.getCourseActive())
                                .studentCount(studentCount)
                                .examCount(examCount)
                                .averageScore(averageScore)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            log.info("강사 과정 정보 조회 완료: {}개 과정", courseDTOs.size());
            return courseDTOs;
            
        } catch (Exception e) {
            log.error("강사 과정 정보 조회 중 오류 발생: email = {}, error = {}", email, e.getMessage(), e);
            throw new RuntimeException("강사 과정 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 특정 과정의 시험 목록 조회
     * @param courseId 과정 ID
     * @return 시험 목록
     */
    public List<StaffExamListDTO> getCourseExams(String courseId) {
        log.info("과정 시험 목록 조회: courseId = {}", courseId);
        
        try {
            // 1. 과정 정보 조회
            CourseEntity course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과정입니다: " + courseId));
            
            // 2. 과정의 소그룹 ID 목록 조회
            List<String> subGroupIds = courseRepository.findSubGroupIdsByCourseIds(List.of(courseId));
            
            // 3. 각 소그룹의 시험 템플릿 목록 조회
            List<TemplateEntity> allTemplates = new ArrayList<>();
            for (String subGroupId : subGroupIds) {
                List<TemplateEntity> templates = templateRepository.findBySubGroupIdAndTemplateActive(subGroupId, 0);
                allTemplates.addAll(templates);
            }
            
            // 4. 각 시험별 상세 정보 조회
            List<StaffExamListDTO> examList = allTemplates.stream()
                    .map(template -> {
                        // 시험 문제 수 조회
                        List<TemplateQuestionEntity> questions = templateQuestionRepository.findByTemplateId(template.getTemplateId());
                        Integer totalQuestions = questions.size();
                        
                        // 총점 계산
                        Integer totalScore = questions.stream()
                                .mapToInt(TemplateQuestionEntity::getTemplateQuestionScore)
                                .sum();
                        
                        // 응시자 수 조회
                        List<ScoreStudentEntity> scores = scoreStudentRepository.findByTemplateId(template.getTemplateId());
                        Integer participantCount = scores.size();
                        
                        // 평균 점수 계산
                        Double avgScore = null;
                        if (!scores.isEmpty()) {
                            double totalScoreSum = scores.stream()
                                    .mapToDouble(ScoreStudentEntity::getScore)
                                    .sum();
                            avgScore = Math.round((totalScoreSum / scores.size()) * 100.0) / 100.0;
                        }
                        
                        // 합격률 계산 (60점 이상을 합격으로 가정)
                        Double passRate = null;
                        if (!scores.isEmpty()) {
                            long passCount = scores.stream()
                                    .filter(score -> score.getScore() >= 60.0)
                                    .count();
                            passRate = Math.round(((double) passCount / scores.size()) * 1000.0) / 10.0;
                        }
                        
                        return StaffExamListDTO.builder()
                                .examId(template.getTemplateId())
                                .examName(template.getTemplateName())
                                .examDescription(null) // TemplateEntity에 description 필드가 없으므로 null로 설정
                                .examTime(template.getTemplateTime())
                                .totalQuestions(totalQuestions)
                                .totalScore(totalScore)
                                .participantCount(participantCount)
                                .avgScore(avgScore)
                                .passRate(passRate)
                                .createdAt(template.getCreatedAt()) // Instant 타입 그대로 사용
                                .examActive(template.getTemplateActive())
                                .build();
                    })
                    .collect(Collectors.toList());
            
            log.info("과정 시험 목록 조회 완료: {}개 시험", examList.size());
            return examList;
            
        } catch (Exception e) {
            log.error("과정 시험 목록 조회 중 오류 발생: courseId = {}, error = {}", courseId, e.getMessage(), e);
            throw new RuntimeException("과정 시험 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 특정 과정의 성적 통계 조회
     * @param courseId 과정 ID
     * @return 과정 성적 통계 정보
     */
    public StaffCourseGradeDTO getCourseGrades(String courseId) {
        log.info("과정 성적 통계 조회: courseId = {}", courseId);
        
        try {
            // 1. 과정 정보 조회
            CourseEntity course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과정입니다: " + courseId));
            
            // 2. 과정의 소그룹 ID 목록 조회
            List<String> subGroupIds = courseRepository.findSubGroupIdsByCourseIds(List.of(courseId));
            
            // 3. 각 소그룹의 시험 템플릿 목록 조회
            List<TemplateEntity> allTemplates = new ArrayList<>();
            for (String subGroupId : subGroupIds) {
                List<TemplateEntity> templates = templateRepository.findBySubGroupIdAndTemplateActive(subGroupId, 0);
                allTemplates.addAll(templates);
            }
            
            // 4. 수강생 수 조회
            Integer studentCount = getStudentCount(courseId);
            
            // 5. 수강생 목록 조회
            List<MemberEntity> courseStudents = memberRepository.findByCourseIdAndMemberRole(courseId, "ROLE_STUDENT");
            
            // 6. 전체 시험 점수 데이터 조회 (종료된 시험의 미응시자는 0점으로 처리)
            List<ScoreStudentEntity> allScores = new ArrayList<>();
            for (TemplateEntity template : allTemplates) {
                List<ScoreStudentEntity> scores = scoreStudentRepository.findByTemplateId(template.getTemplateId());
                
                // 시험 상태 확인
                String examStatus = getExamStatus(template);
                
                // 응시한 학생들의 memberId 수집
                Set<String> participatedMemberIds = scores.stream()
                        .map(ScoreStudentEntity::getMemberId)
                        .filter(memberId -> memberId != null && !memberId.trim().isEmpty())
                        .collect(Collectors.toSet());
                
                // 시험이 종료된 경우에만 미응시 학생들에게 0점 부여
                if ("ENDED".equals(examStatus)) {
                    Set<String> nonParticipatedMemberIds = new HashSet<>();
                    for (MemberEntity student : courseStudents) {
                        if (!participatedMemberIds.contains(student.getMemberId())) {
                            nonParticipatedMemberIds.add(student.getMemberId());
                        }
                    }
                    
                    for (String memberId : nonParticipatedMemberIds) {
                        ScoreStudentEntity zeroScore = new ScoreStudentEntity();
                        zeroScore.setTemplateId(template.getTemplateId());
                        zeroScore.setMemberId(memberId);
                        zeroScore.setScore(0);
                        allScores.add(zeroScore);
                    }
                }
                
                allScores.addAll(scores);
            }
            
            // 6. 전체 평균 점수 계산
            Double avgScore = calculateOverallAverageScore(allScores);
            
            // 7. 합격률 계산 (60점 이상을 합격으로 가정)
            Double passRate = calculatePassRate(allScores, 60.0);
            
            // 8. 등급별 분포 계산 (전체 과정 통합 - 제거)
            // Map<String, Integer> gradeDistribution = calculateGradeDistribution(allScores);
            
            // 9. 시험별 상세 정보 계산
            List<StaffExamDetailDTO> examDetails = calculateExamDetails(allTemplates);
            
            // 10. DTO 구성
            StaffCourseGradeDTO result = StaffCourseGradeDTO.builder()
                    .avgScore(avgScore)
                    .passRate(passRate)
                    .examCount(allTemplates.size())
                    .studentCount(studentCount)
                    .gradeDistribution(null) // 시험별로 분리하여 표시하므로 null로 설정
                    .examDetails(examDetails)
                    .build();
            
            log.info("과정 성적 통계 조회 완료: 평균점수={}, 합격률={}%, 시험수={}, 수강생수={}", 
                    avgScore, passRate, allTemplates.size(), studentCount);
            
            return result;
            
        } catch (Exception e) {
            log.error("과정 성적 통계 조회 중 오류 발생: courseId = {}, error = {}", courseId, e.getMessage(), e);
            throw new RuntimeException("과정 성적 통계 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 전체 평균 점수 계산
     */
    private Double calculateOverallAverageScore(List<ScoreStudentEntity> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        
        double totalScore = scores.stream()
                .mapToDouble(ScoreStudentEntity::getScore)
                .sum();
        double average = totalScore / scores.size();
        
        return Math.round(average * 100.0) / 100.0;
    }
    
    /**
     * 합격률 계산
     */
    private Double calculatePassRate(List<ScoreStudentEntity> scores, double passThreshold) {
        if (scores.isEmpty()) {
            return null;
        }
        
        long passCount = scores.stream()
                .filter(score -> score.getScore() >= passThreshold)
                .count();
        
        double passRate = (double) passCount / scores.size() * 100.0;
        return Math.round(passRate * 10.0) / 10.0;
    }
    
    /**
     * 등급별 분포 계산
     */
    private Map<String, Integer> calculateGradeDistribution(List<ScoreStudentEntity> scores) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("A", 0); // 90점 이상
        distribution.put("B", 0); // 80-89점
        distribution.put("C", 0); // 70-79점
        distribution.put("D", 0); // 60-69점
        distribution.put("F", 0); // 60점 미만
        
        for (ScoreStudentEntity score : scores) {
            int scoreValue = score.getScore();
            if (scoreValue >= 90) {
                distribution.put("A", distribution.get("A") + 1);
            } else if (scoreValue >= 80) {
                distribution.put("B", distribution.get("B") + 1);
            } else if (scoreValue >= 70) {
                distribution.put("C", distribution.get("C") + 1);
            } else if (scoreValue >= 60) {
                distribution.put("D", distribution.get("D") + 1);
            } else {
                distribution.put("F", distribution.get("F") + 1);
            }
        }
        
        return distribution;
    }
    
    /**
     * 시험별 상세 정보 계산
     */
    private List<StaffExamDetailDTO> calculateExamDetails(List<TemplateEntity> templates) {
        return templates.stream()
                .map(template -> {
                    List<ScoreStudentEntity> scores = scoreStudentRepository.findByTemplateId(template.getTemplateId());
                    
                    Double avgScore = null;
                    Double passRate = null;
                    Integer participantCount = scores.size();
                    Map<String, Integer> gradeDistribution = calculateGradeDistribution(scores);
                    
                    // 시험 상태 계산
                    String examStatus = getExamStatus(template);
                    String examStatusDescription = getExamStatusDescription(examStatus);
                    
                    // 문제 개수 조회
                    List<TemplateQuestionEntity> questions = templateQuestionRepository.findByTemplateId(template.getTemplateId());
                    Integer questionCount = questions.size();
                    
                    if (!scores.isEmpty()) {
                        // 평균 점수 계산
                        double totalScore = scores.stream()
                                .mapToDouble(ScoreStudentEntity::getScore)
                                .sum();
                        avgScore = Math.round((totalScore / scores.size()) * 100.0) / 100.0;
                        
                        // 합격률 계산
                        long passCount = scores.stream()
                                .filter(score -> score.getScore() >= 60.0)
                                .count();
                        passRate = Math.round(((double) passCount / scores.size()) * 1000.0) / 10.0;
                    }
                    
                    return StaffExamDetailDTO.builder()
                            .examId(template.getTemplateId())
                            .examName(template.getTemplateName())
                            .avgScore(avgScore)
                            .passRate(passRate)
                            .participantCount(participantCount)
                            .gradeDistribution(gradeDistribution)
                            .examStatus(examStatus)
                            .examStatusDescription(examStatusDescription)
                            .questionCount(questionCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 강의실 번호 조회
     * @param classId 강의실 ID
     * @return 강의실 번호
     */
    private String getClassNumber(String classId) {
        try {
            if (classId == null || classId.trim().isEmpty()) {
                return null;
            }
            
            // 강의실 정보 조회 (ClassroomEntity가 있다면 사용)
            // 현재는 기본값 반환
            return classId;
            
        } catch (Exception e) {
            log.error("강의실 번호 조회 중 오류 발생: classId = {}, error = {}", classId, e.getMessage(), e);
            return classId; // 기본값으로 classId 반환
        }
    }
    
    /**
     * memberId로 강사 이름 조회
     * @param memberId 강사 ID
     * @return 강사 이름
     */
    private String getMemberName(String memberId) {
        try {
            if (memberId == null || memberId.trim().isEmpty()) {
                return null;
            }
            
            // memberId로 Member 정보 조회
            List<MemberEntity> members = memberRepository.findByMemberId(memberId);
            if (!members.isEmpty()) {
                return members.get(0).getMemberName();
            }
            
            return null;
        } catch (Exception e) {
            log.error("강사 이름 조회 중 오류 발생: memberId={}, error={}", memberId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 과정별 수강생 수 조회 (이메일 기준 중복 제거)
     * @param courseId 과정 ID
     * @return 수강생 수
     */
    private Integer getStudentCount(String courseId) {
        try {
            if (courseId == null || courseId.trim().isEmpty()) {
                return 0;
            }
            
            // courseId로 수강생 조회 (ROLE_STUDENT인 멤버)
            List<MemberEntity> students = memberRepository.findByCourseIdAndMemberRole(courseId, "ROLE_STUDENT");
            
            // 이메일 기준으로 중복 제거
            long uniqueStudentCount = students.stream()
                .map(MemberEntity::getMemberEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .distinct()
                .count();
            
            log.debug("과정 {}의 수강생 수 (이메일 기준 중복 제거): {}", courseId, uniqueStudentCount);
            return (int) uniqueStudentCount;
            
        } catch (Exception e) {
            log.error("수강생 수 조회 중 오류 발생: courseId={}, error={}", courseId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 과정별 시험 평균 점수 계산
     * @param courseId 과정 ID
     * @return 평균 점수 (소수점 2자리)
     */
    private Double calculateAverageScore(String courseId) {
        try {
            if (courseId == null || courseId.trim().isEmpty()) {
                return null;
            }
            
            // 1. 과정의 소그룹 ID 목록 조회
            List<String> subGroupIds = courseRepository.findSubGroupIdsByCourseIds(List.of(courseId));
            if (subGroupIds.isEmpty()) {
                log.debug("과정 {}에 해당하는 소그룹이 없습니다.", courseId);
                return null;
            }
            
            // 2. 각 소그룹의 시험 템플릿 ID 목록 조회
            List<String> templateIds = new ArrayList<>();
            for (String subGroupId : subGroupIds) {
                List<TemplateEntity> templates = 
                    templateRepository.findBySubGroupIdAndTemplateActive(subGroupId, 0);
                templateIds.addAll(templates.stream()
                    .map(TemplateEntity::getTemplateId)
                    .collect(Collectors.toList()));
            }
            
            if (templateIds.isEmpty()) {
                log.debug("과정 {}에 해당하는 시험이 없습니다.", courseId);
                return null;
            }
            
            // 3. ScoreStudentEntity에서 평균 점수 계산
            List<ScoreStudentEntity> scoreStudents = scoreStudentRepository.findAll().stream()
                .filter(score -> templateIds.contains(score.getTemplateId()))
                .collect(Collectors.toList());
            
            if (scoreStudents.isEmpty()) {
                log.debug("과정 {}의 시험 점수 데이터가 없습니다.", courseId);
                return null;
            }
            
            // 4. 평균 점수 계산
            double totalScore = scoreStudents.stream()
                .mapToDouble(ScoreStudentEntity::getScore)
                .sum();
            double averageScore = totalScore / scoreStudents.size();
            
            // 소수점 2자리로 반올림
            averageScore = Math.round(averageScore * 100.0) / 100.0;
            
            log.debug("과정 {}의 평균 점수: {} (총 {}명)", courseId, averageScore, scoreStudents.size());
            return averageScore;
            
        } catch (Exception e) {
            log.error("평균 점수 계산 중 오류 발생: courseId={}, error={}", courseId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 시험 상태 판단
     * @param template 시험 템플릿
     * @return 시험 상태 (BEFORE_START, IN_PROGRESS, ENDED, NO_SCHEDULE)
     */
    private String getExamStatus(TemplateEntity template) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = template.getTemplateOpen();
        LocalDateTime endTime = template.getTemplateClose();
        
        // 시작/종료 시간이 설정되지 않은 경우
        if (startTime == null || endTime == null) {
            return "NO_SCHEDULE";
        }
        
        // 시험 시작 전
        if (now.isBefore(startTime)) {
            return "BEFORE_START";
        }
        
        // 시험 진행 중
        if (now.isAfter(startTime) && now.isBefore(endTime)) {
            return "IN_PROGRESS";
        }
        
        // 시험 종료
        if (now.isAfter(endTime)) {
            return "ENDED";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 시험 상태에 따른 설명 반환
     * @param status 시험 상태
     * @return 상태 설명
     */
    private String getExamStatusDescription(String status) {
        switch (status) {
            case "BEFORE_START":
                return "시험 시작 전";
            case "IN_PROGRESS":
                return "시험 진행 중";
            case "ENDED":
                return "시험 종료";
            case "NO_SCHEDULE":
                return "일정 미설정";
            default:
                return "알 수 없음";
        }
    }

    /**
     * 시험별 문제 통계 조회
     * @param templateId 시험 템플릿 ID
     * @return 시험별 문제 통계 정보
     */
    public QuestionDTO getExamQuestionStats(String templateId) {
        log.info("시험별 문제 통계 조회: templateId = {}", templateId);
        
        try {
            // 1. 시험 템플릿 정보 조회
            TemplateEntity template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시험입니다: " + templateId));
            
            // 2. 시험 문제 목록 조회
            List<TemplateQuestionEntity> questions = templateQuestionRepository.findByTemplateId(templateId);
            
            // 3. 시험 응시자 목록 조회
            List<ScoreStudentEntity> participants = scoreStudentRepository.findByTemplateId(templateId);
            
            // 4. 문제별 통계 계산
            List<QuestionDTO> questionStats = calculateQuestionStats(questions, participants);
            
            // 5. 평균 정답률 계산
            Double avgCorrectRate = calculateAverageCorrectRate(questionStats);
            
            // 6. DTO 구성
            QuestionDTO result = QuestionDTO.builder()
                    .examId(template.getTemplateId())
                    .examName(template.getTemplateName())
                    .questionStats(questionStats)
                    .totalQuestions(questions.size())
                    .avgCorrectRate(avgCorrectRate)
                    .build();
            
            log.info("시험별 문제 통계 조회 완료: 시험명={}, 문제수={}, 평균정답률={}%", 
                    template.getTemplateName(), questions.size(), avgCorrectRate);
            
            return result;
            
        } catch (Exception e) {
            log.error("시험별 문제 통계 조회 중 오류 발생: templateId = {}, error = {}", templateId, e.getMessage(), e);
            throw new RuntimeException("시험별 문제 통계 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 문제별 통계 계산
     */
    private List<QuestionDTO> calculateQuestionStats(List<TemplateQuestionEntity> questions, List<ScoreStudentEntity> participants) {
        return questions.stream()
                .map(question -> {
                    // 해당 문제의 모든 답안 조회
                    List<AnswerEntity> answers = answerRepository.findByTemplateQuestionId(question.getTemplateQuestionId());
                    
                    // 문제 유형 조회
                    String questionType = "알 수 없음";
                    try {
                        QuestionEntity questionEntity = questionRepository.findById(question.getQuestionId()).orElse(null);
                        if (questionEntity != null) {
                            questionType = questionEntity.getQuestionType();
                        }
                    } catch (Exception e) {
                        log.warn("문제 유형 조회 실패: questionId={}, error={}", question.getQuestionId(), e.getMessage());
                    }
                    
                    // 정답자 수 계산
                    int correctCount;
                    if ("서술형".equals(questionType)) {
                        // 서술형 문제: 배점의 80% 이상이면 정답으로 인정
                        double passThreshold = question.getTemplateQuestionScore() * 0.8;
                        log.info("서술형 문제 {} - 배점: {}, 80% 기준: {}, 답안 점수들: {}", 
                                questions.indexOf(question) + 1, 
                                question.getTemplateQuestionScore(), 
                                passThreshold,
                                answers.stream().map(AnswerEntity::getAnswerScore).collect(Collectors.toList()));
                        
                        correctCount = (int) answers.stream()
                                .filter(answer -> answer.getAnswerScore() >= passThreshold)
                                .count();
                    } else {
                        // 객관식/코드형 문제: 배점과 정확히 일치해야 정답
                        correctCount = (int) answers.stream()
                                .filter(answer -> answer.getAnswerScore() == question.getTemplateQuestionScore())
                                .count();
                    }
                    
                    // 오답자 수 계산
                    int incorrectCount;
                    if ("서술형".equals(questionType)) {
                        // 서술형 문제: 배점의 80% 미만이지만 0보다 큰 경우
                        double passThreshold = question.getTemplateQuestionScore() * 0.8;
                        incorrectCount = (int) answers.stream()
                                .filter(answer -> answer.getAnswerScore() > 0 && 
                                        answer.getAnswerScore() < passThreshold)
                                .count();
                    } else {
                        // 객관식/코드형 문제: 기존 로직 유지
                        incorrectCount = (int) answers.stream()
                                .filter(answer -> answer.getAnswerScore() > 0 && 
                                        answer.getAnswerScore() < question.getTemplateQuestionScore())
                                .count();
                    }
                    
                    // 미응답자 수 계산 (답안이 없거나 점수가 0인 경우)
                    int noAnswerCount = participants.size() - correctCount - incorrectCount;
                    
                    // 정답률 계산
                    double correctRate = participants.isEmpty() ? 0.0 : 
                            (double) correctCount / participants.size() * 100.0;
                    correctRate = Math.round(correctRate * 10.0) / 10.0; // 소수점 첫째 자리까지
                    
                    return QuestionDTO.builder()
                            .questionNumber(questions.indexOf(question) + 1) // 문제 번호는 순서대로 1부터 시작
                            .questionType(questionType)
                            .correctCount(correctCount)
                            .incorrectCount(incorrectCount)
                            .noAnswerCount(noAnswerCount)
                            .correctRate(correctRate)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 평균 정답률 계산
     */
    private Double calculateAverageCorrectRate(List<QuestionDTO> questionStats) {
        if (questionStats.isEmpty()) {
            return 0.0;
        }
        
        double totalCorrectRate = questionStats.stream()
                .mapToDouble(QuestionDTO::getCorrectRate)
                .sum();
        
        double avgCorrectRate = totalCorrectRate / questionStats.size();
        return Math.round(avgCorrectRate * 10.0) / 10.0; // 소수점 첫째 자리까지
    }
} 