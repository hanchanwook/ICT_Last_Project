package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.StudentGradeDTO;
import com.jakdang.labs.api.lnuyasha.repository.StudentGradeRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentGradeService {
    
    private final StudentGradeRepository studentGradeRepository;
    private final MemberRepository memberRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 학생 성적 조회
     */
    public List<StudentGradeDTO> getStudentGrades(String userId) {
        log.info("학생 성적 조회 - userId: {}", userId);
        
        // userId로 학생이 수강 중인 모든 과정 정보 조회 (memberId, courseId)
        List<MemberEntity> studentCourses = studentGradeRepository.findStudentCoursesByUserId(userId);
        
        if (studentCourses.isEmpty()) {
            log.info("학생이 수강 중인 과정이 없습니다 - userId: {}", userId);
            return List.of();
        }
        
        return studentCourses.stream()
                .map(this::convertToStudentGradeDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * MemberEntity를 StudentGradeDTO로 변환
     */
    private StudentGradeDTO convertToStudentGradeDTO(MemberEntity studentCourse) {
        String courseId = studentCourse.getCourseId();
        String memberId = studentCourse.getMemberId();
        
        log.info("=== 과정 정보 변환 시작 ===");
        log.info("courseId: {}, memberId: {}", courseId, memberId);
        
        // 과정 정보 조회
        CourseEntity course = studentGradeRepository.findCourseByCourseId(courseId);
        if (course == null) {
            log.warn("과정 정보를 찾을 수 없음 - courseId: {}", courseId);
            return null;
        }
        
        // 과정의 시험 목록 조회
        List<TemplateEntity> templates = studentGradeRepository.findTemplatesByCourseId(courseId);
        log.info("과정 '{}'의 시험 개수: {}", course.getCourseName(), templates.size());
        
        if (templates.isEmpty()) {
            log.warn("과정 '{}'에 시험이 없습니다", course.getCourseName());
        } else {
            templates.forEach(template -> {
                log.info("시험 정보 - ID: {}, 이름: {}", template.getTemplateId(), template.getTemplateName());
            });
        }
        
        // 학생의 시험 성적 목록 조회
        List<StudentGradeDTO.ExamGradeDTO> examGrades = templates.stream()
                .map(template -> convertToExamGradeDTO(template, memberId))
                .collect(Collectors.toList()); // 필터링 제거 - 모든 시험 표시
        
        log.info("학생의 성적이 있는 시험 개수: {}", examGrades.size());
        
        return StudentGradeDTO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .instructor(getInstructorName(course.getMemberId()))
                .exams(examGrades)
                .build();
    }
    
    /**
     * TemplateEntity를 ExamGradeDTO로 변환
     */
    private StudentGradeDTO.ExamGradeDTO convertToExamGradeDTO(TemplateEntity template, String memberId) {
        log.info("=== 시험 성적 변환 시작 ===");
        log.info("templateId: {}, memberId: {}", template.getTemplateId(), memberId);
        
        // 학생의 해당 시험 성적 조회
        ScoreStudentEntity scoreStudent = studentGradeRepository.findScoreByTemplateAndMember(template.getTemplateId(), memberId);
        
        if (scoreStudent == null) {
            log.warn("학생의 시험 성적이 없음 - templateId: {}, memberId: {}", template.getTemplateId(), memberId);
            // 성적이 없어도 시험 정보는 반환 (점수는 0으로 설정)
            return StudentGradeDTO.ExamGradeDTO.builder()
                    .examId(template.getTemplateId())
                    .examName(template.getTemplateName())
                    .examDate(template.getCreatedAt() != null ? 
                        template.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER) : null)
                    .totalScore(0.0)
                    .maxScore(calculateMaxScore(template.getTemplateId()))
                    .grade("미응시")
                    .questionTypes(StudentGradeDTO.QuestionTypesDTO.builder()
                            .objective(StudentGradeDTO.ScoreDTO.builder().score(0.0).maxScore(0.0).build())
                            .subjective(StudentGradeDTO.ScoreDTO.builder().score(0.0).maxScore(0.0).build())
                            .build())
                    .build();
        }
        
        log.info("시험 성적 조회 성공 - 점수: {}", scoreStudent.getScore());
        
        // 문제 유형별 점수 계산
        StudentGradeDTO.QuestionTypesDTO questionTypes = calculateQuestionTypes(template.getTemplateId(), memberId);
        
        return StudentGradeDTO.ExamGradeDTO.builder()
                .examId(template.getTemplateId())
                .examName(template.getTemplateName())
                .examDate(template.getCreatedAt() != null ? 
                    template.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER) : null)
                .totalScore((double) scoreStudent.getScore())
                .maxScore(calculateMaxScore(template.getTemplateId()))
                .grade(getGradeByScore((double) scoreStudent.getScore()))
                .questionTypes(questionTypes)
                .build();
    }
    
    /**
     * 문제 유형별 점수 계산
     */
    private StudentGradeDTO.QuestionTypesDTO calculateQuestionTypes(String templateId, String memberId) {
        // 객관식 점수
        Double objectiveScore = studentGradeRepository.calculateObjectiveScore(templateId, memberId);
        Double objectiveMaxScore = studentGradeRepository.calculateObjectiveMaxScore(templateId);
        
        // 서술형 점수
        Double subjectiveScore = studentGradeRepository.calculateSubjectiveScore(templateId, memberId);
        Double subjectiveMaxScore = studentGradeRepository.calculateSubjectiveMaxScore(templateId);
        
        return StudentGradeDTO.QuestionTypesDTO.builder()
                .objective(StudentGradeDTO.ScoreDTO.builder()
                        .score(objectiveScore != null ? objectiveScore : 0.0)
                        .maxScore(objectiveMaxScore != null ? objectiveMaxScore : 0.0)
                        .build())
                .subjective(StudentGradeDTO.ScoreDTO.builder()
                        .score(subjectiveScore != null ? subjectiveScore : 0.0)
                        .maxScore(subjectiveMaxScore != null ? subjectiveMaxScore : 0.0)
                        .build())
                .build();
    }
    
    /**
     * 시험 만점 계산
     */
    private Double calculateMaxScore(String templateId) {
        try {
            return studentGradeRepository.calculateMaxScoreByTemplateId(templateId);
        } catch (Exception e) {
            log.warn("시험 만점 계산 실패 - templateId: {}, 기본값 사용", templateId, e);
            return 100.0; // 기본 만점
        }
    }
    
    /**
     * 강사명 조회
     */
    private String getInstructorName(String instructorMemberId) {
        try {
            MemberEntity instructor = memberRepository.findById(instructorMemberId).orElse(null);
            return instructor != null ? instructor.getMemberName() : "알 수 없음";
        } catch (Exception e) {
            log.warn("강사명 조회 실패 - instructorMemberId: {}", instructorMemberId, e);
            return "알 수 없음";
        }
    }
    
    /**
     * 점수에 따른 등급 계산
     */
    private String getGradeByScore(Double score) {
        if (score == null) return "노력";
        
        if (score >= 90) return "탁월";
        else if (score >= 80) return "우수";
        else if (score >= 70) return "양호";
        else if (score >= 60) return "보통";
        else return "노력";
    }
} 