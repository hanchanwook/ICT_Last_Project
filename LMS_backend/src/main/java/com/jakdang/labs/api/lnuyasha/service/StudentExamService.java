package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.dto.QuestionDTO;
import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.lnuyasha.dto.ExamDTO;
import com.jakdang.labs.api.lnuyasha.repository.StudentExamRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyCourseRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.ScoreStudentRepository;
import com.jakdang.labs.api.lnuyasha.repository.SubGroupRepository;
import com.jakdang.labs.api.lnuyasha.repository.QuestionRepository;
import com.jakdang.labs.api.lnuyasha.repository.QuestionOptRepository;
import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.TemplateQuestionEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import com.jakdang.labs.entity.QuestionEntity;
import com.jakdang.labs.entity.QuestionOptEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.SubGroupEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.jakdang.labs.api.lnuyasha.util.TimeZoneUtil;
import java.util.HashSet;
import java.util.Set;
import com.jakdang.labs.api.lnuyasha.dto.QuestionOptionDTO;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentExamService {

    private final StudentExamRepository studentExamRepository;
    private final KyMemberRepository memberRepository;
    private final KyCourseRepository courseRepository;
    private final TemplateRepository templateRepository;
    private final ScoreStudentRepository scoreStudentRepository;
    private final SubGroupRepository subGroupRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptRepository questionOptRepository;

    /**
     * 학생 답안 조회 (통합된 AnswerDTO 사용)
     */
    public List<AnswerDTO> getStudentAnswers(String templateId, String studentId) {
        log.info("학생 답안 조회 - templateId: {}, studentId: {}", templateId, studentId);

        // 1. 템플릿 정보 조회
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId));
        
        // 2. 학생의 답안 조회
        List<AnswerEntity> answers = studentExamRepository.findStudentAnswersByTemplateId(templateId, studentId);
        log.info("답안 조회 결과 - templateId: {}, studentId: {}, answers.size: {}", templateId, studentId, answers.size());

        // 3. 답안을 AnswerDTO로 변환
        List<AnswerDTO> answerDTOs = new ArrayList<>();
        
        for (AnswerEntity answer : answers) {
            TemplateQuestionEntity templateQuestion = answer.getTemplateQuestion();
            QuestionEntity question = questionRepository.findById(templateQuestion.getQuestionId()).orElse(null);
            
            if (question != null) {
                // 정답 정보 조회 (객관식, 참거짓의 경우)
                String correctAnswer = null;
                Boolean isCorrect = null;
                
                if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                    List<QuestionOptEntity> correctOptions = questionOptRepository.findCorrectOptionsByQuestionId(question.getQuestionId());
                    if (!correctOptions.isEmpty()) {
                        correctAnswer = correctOptions.get(0).getOptText();
                        isCorrect = correctAnswer.equals(answer.getAnswerText());
                    }
                }
                
                AnswerDTO answerDTO = AnswerDTO.builder()
                        .answerId(answer.getAnswerId())
                        .questionId(question.getQuestionId())
                        .memberId(answer.getMemberId())
                        .answerContent(answer.getAnswerText())
                        .studentAnswer(answer.getAnswerText()) // 호환성
                        .answerResult(answer.getAnswerScore() > 0 ? "정답" : "오답")
                        .isCorrect(isCorrect)
                        .answerScore(answer.getAnswerScore())
                        .score(answer.getAnswerScore()) // 호환성
                        .comment(answer.getTeacherComment())
                        .questionText(question.getQuestionText())
                        .questionType(question.getQuestionType())
                        .questionScore(templateQuestion.getTemplateQuestionScore())
                        .correctAnswer(correctAnswer)
                        .answerDate(answer.getCreatedAt())
                        .createdAt(answer.getCreatedAt())
                        .updatedAt(answer.getCreatedAt())
                        .build();
                
                answerDTOs.add(answerDTO);
            }
        }
        
        return answerDTOs;
    }

    /**
     * 시험 문제 목록 조회 (통합된 QuestionDTO 사용)
     */
    public List<QuestionDTO> getExamQuestions(String templateId, String userId) {
        log.info("시험 문제 목록 조회 - templateId: {}, userId: {}", templateId, userId);
        
        // 1. 템플릿 문제 목록 조회
        List<TemplateQuestionEntity> templateQuestions = studentExamRepository.findTemplateQuestionsByTemplateId(templateId);
        
        // 2. 학생의 답안 조회
        List<AnswerEntity> studentAnswers = studentExamRepository.findStudentAnswersByTemplateId(templateId, userId);
        log.info("학생 답안 조회 결과 - templateId: {}, userId: {}, answers.size: {}", templateId, userId, studentAnswers.size());
        
        // 답안을 Map으로 변환하여 빠른 조회 가능하도록 함
        Map<String, AnswerEntity> answerMap = new HashMap<>();
        for (AnswerEntity answer : studentAnswers) {
            answerMap.put(answer.getTemplateQuestion().getTemplateQuestionId(), answer);
        }
        
        // 3. 문제를 QuestionDTO로 변환
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            QuestionEntity question = questionRepository.findById(templateQuestion.getQuestionId()).orElse(null);
            
            if (question != null) {
                // 객관식 선택지 조회
                List<QuestionOptionDTO> options = new ArrayList<>();
                if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                    List<QuestionOptEntity> questionOptions = questionOptRepository.findByQuestionId(question.getQuestionId());
                    options = questionOptions.stream()
                            .map(opt -> QuestionOptionDTO.builder()
                                    .optId(opt.getOptId())
                                    .optText(opt.getOptText())
                                    .optIsCorrect(opt.getOptIsCorrect())
                                    .build())
                            .collect(Collectors.toList());
                }
                
                // 학생의 답안 정보 조회
                AnswerEntity studentAnswer = answerMap.get(templateQuestion.getTemplateQuestionId());
                String studentAnswerText = null;
                Double studentScore = null;
                String correctAnswer = null;
                Boolean isCorrect = null;
                
                if (studentAnswer != null) {
                    studentAnswerText = studentAnswer.getAnswerText();
                    studentScore = (double) studentAnswer.getAnswerScore();
                    
                    // 정답 정보 조회 (객관식, 참거짓의 경우)
                    if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                        List<QuestionOptEntity> correctOptions = questionOptRepository.findCorrectOptionsByQuestionId(question.getQuestionId());
                        if (!correctOptions.isEmpty()) {
                            correctAnswer = correctOptions.get(0).getOptText();
                            isCorrect = correctAnswer.equals(studentAnswerText);
                        }
                    }
                }
                
                QuestionDTO questionDTO = QuestionDTO.builder()
                        .questionId(question.getQuestionId())
                        .questionText(question.getQuestionText())
                        .questionType(question.getQuestionType())
                        .questionAnswer(question.getQuestionAnswer())
                        .explanation(question.getExplanation())
                        .codeLanguage(question.getCodeLanguage())
                        .templateQuestionId(templateQuestion.getTemplateQuestionId())
                        .questionScore(templateQuestion.getTemplateQuestionScore()) // 원래 배점
                        .instructorId(question.getMemberId())
                        .subDetailId(question.getSubDetailId())
                        .educationId(question.getEducationId())
                        .questionActive(String.valueOf(question.getQuestionActive()))
                        .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                        .updatedAt(TimeZoneUtil.toKoreanTime(question.getUpdatedAt()))
                        .options(options) // 객관식 선택지
                        .studentAnswer(studentAnswerText) // 학생 답안
                        .studentScore(studentScore) // 학생이 받은 점수
                        .correctAnswer(correctAnswer) // 정답 (객관식, 참거짓의 경우)
                        .isCorrect(isCorrect) // 정답 여부 (객관식, 참거짓의 경우)
                        .build();
                
                questionDTOs.add(questionDTO);
            }
        }
        
        return questionDTOs;
    }

    /**
     * 이메일 기반 시험 문제 목록 조회
     */
    public List<QuestionDTO> getExamQuestionsByEmail(String templateId, String studentEmail) {
        log.info("이메일 기반 시험 문제 목록 조회 - templateId: {}, studentEmail: {}", templateId, studentEmail);
        
        // 1. 학생의 memberId들 조회
        List<MemberEntity> students = memberRepository.findByMemberEmail(studentEmail);
        if (students.isEmpty()) {
            throw new IllegalArgumentException("해당 이메일의 학생을 찾을 수 없습니다: " + studentEmail);
        }
        
        // 2. 템플릿 문제 목록 조회
        List<TemplateQuestionEntity> templateQuestions = studentExamRepository.findTemplateQuestionsByTemplateId(templateId);
        
        // 3. 모든 memberId에 대해 학생의 답안 조회
        List<AnswerEntity> allStudentAnswers = new ArrayList<>();
        for (MemberEntity student : students) {
            List<AnswerEntity> studentAnswers = studentExamRepository.findStudentAnswersByTemplateId(templateId, student.getMemberId());
            allStudentAnswers.addAll(studentAnswers);
        }
        log.info("학생 답안 조회 결과 - templateId: {}, studentEmail: {}, total answers.size: {}", templateId, studentEmail, allStudentAnswers.size());
        
        // 답안을 Map으로 변환하여 빠른 조회 가능하도록 함
        Map<String, AnswerEntity> answerMap = new HashMap<>();
        for (AnswerEntity answer : allStudentAnswers) {
            answerMap.put(answer.getTemplateQuestion().getTemplateQuestionId(), answer);
        }
        
        // 4. 학생의 성적 정보 조회 (isChecked 값 포함)
        Integer isChecked = null;
        for (MemberEntity student : students) {
            ScoreStudentEntity scoreStudent = scoreStudentRepository.findByTemplateIdAndMemberId(templateId, student.getMemberId());
            if (scoreStudent != null) {
                isChecked = scoreStudent.getIsChecked();
                log.info("학생 성적 정보 조회 - memberId: {}, isChecked: {}", student.getMemberId(), isChecked);
                break; // 첫 번째 학생의 정보만 사용
            }
        }
        
        // 5. 문제를 QuestionDTO로 변환
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            QuestionEntity question = questionRepository.findById(templateQuestion.getQuestionId()).orElse(null);
            
            if (question != null) {
                // 객관식 선택지 조회
                List<QuestionOptionDTO> options = new ArrayList<>();
                if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                    List<QuestionOptEntity> questionOptions = questionOptRepository.findByQuestionId(question.getQuestionId());
                    options = questionOptions.stream()
                            .map(opt -> QuestionOptionDTO.builder()
                                    .optId(opt.getOptId())
                                    .optText(opt.getOptText())
                                    .optIsCorrect(opt.getOptIsCorrect())
                                    .build())
                            .collect(Collectors.toList());
                }
                
                // 학생의 답안 정보 조회
                AnswerEntity studentAnswer = answerMap.get(templateQuestion.getTemplateQuestionId());
                String studentAnswerText = null;
                Double studentScore = null;
                String correctAnswer = null;
                Boolean isCorrect = null;
                
                if (studentAnswer != null) {
                    studentAnswerText = studentAnswer.getAnswerText();
                    studentScore = (double) studentAnswer.getAnswerScore();
                    
                    // 정답 정보 조회 (객관식, 참거짓의 경우)
                    if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                        List<QuestionOptEntity> correctOptions = questionOptRepository.findCorrectOptionsByQuestionId(question.getQuestionId());
                        if (!correctOptions.isEmpty()) {
                            correctAnswer = correctOptions.get(0).getOptText();
                            isCorrect = correctAnswer.equals(studentAnswerText);
                        }
                    }
                }
                
                QuestionDTO questionDTO = QuestionDTO.builder()
                        .questionId(question.getQuestionId())
                        .questionText(question.getQuestionText())
                        .questionType(question.getQuestionType())
                        .questionAnswer(question.getQuestionAnswer())
                        .explanation(question.getExplanation())
                        .codeLanguage(question.getCodeLanguage())
                        .templateQuestionId(templateQuestion.getTemplateQuestionId())
                        .questionScore(templateQuestion.getTemplateQuestionScore()) // 원래 배점
                        .instructorId(question.getMemberId())
                        .subDetailId(question.getSubDetailId())
                        .educationId(question.getEducationId())
                        .questionActive(String.valueOf(question.getQuestionActive()))
                        .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                        .updatedAt(TimeZoneUtil.toKoreanTime(question.getUpdatedAt()))
                        .options(options) // 객관식 선택지
                        .studentAnswer(studentAnswerText) // 학생 답안
                        .studentScore(studentScore) // 학생이 받은 점수
                        .correctAnswer(correctAnswer) // 정답 (객관식, 참거짓의 경우)
                        .isCorrect(isCorrect) // 정답 여부 (객관식, 참거짓의 경우)
                        .isChecked(isChecked) // 성적 확인 여부
                        .build();
                
                questionDTOs.add(questionDTO);
            }
        }
        
        return questionDTOs;
    }

    /**
     * 성적 확인 (이메일과 courseId 기반)
     */
    @Transactional
    public Map<String, Object> confirmGrade(AnswerDTO request, String studentEmail, String courseId) {
        log.info("성적 확인 - request: {}, studentEmail: {}, courseId: {}", request, studentEmail, courseId);
        
        // 1. 이메일과 courseId로 memberId 조회
        MemberEntity member = memberRepository.findByMemberEmailAndCourseId(studentEmail, courseId);
        if (member == null) {
            throw new IllegalArgumentException("해당 이메일과 과정에 해당하는 학생을 찾을 수 없습니다.");
        }
        
        String memberId = member.getMemberId();
        log.info("조회된 memberId: {}", memberId);
        
        // 2. 해당 memberId로 성적 조회
        ScoreStudentEntity scoreStudent = scoreStudentRepository.findByTemplateIdAndMemberId(
            request.getTemplateId(), memberId);
        
        if (scoreStudent == null) {
            throw new IllegalArgumentException("해당 시험의 성적을 찾을 수 없습니다.");
        }

        scoreStudent.setIsChecked(0);
        scoreStudentRepository.save(scoreStudent);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "성적 확인이 완료되었습니다.");
        
        return result;
    }

    /**
     * 성적 확인 (userId와 courseId 기반)
     */
    @Transactional
    public Map<String, Object> confirmGradeByUserIdAndCourseId(AnswerDTO request) {
        log.info("성적 확인 - request: {}, userId: {}, courseId: {}", request, request.getUserId(), request.getCourseId());
        
        if (request.getUserId() == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("userId와 courseId는 필수입니다.");
        }
        
        // 1. userId와 courseId로 memberId 조회
        String memberId = memberRepository.findMemberIdByUserIdAndCourseId(request.getUserId(), request.getCourseId());
        if (memberId == null) {
            throw new IllegalArgumentException("해당 사용자 ID와 과정에 해당하는 학생을 찾을 수 없습니다.");
        }
        
        log.info("조회된 memberId: {}", memberId);
        
        // 2. 해당 memberId로 성적 조회
        ScoreStudentEntity scoreStudent = scoreStudentRepository.findByTemplateIdAndMemberId(
            request.getTemplateId(), memberId);
        
        if (scoreStudent == null) {
            throw new IllegalArgumentException("해당 시험의 성적을 찾을 수 없습니다.");
        }

        scoreStudent.setIsChecked(0);
        scoreStudentRepository.save(scoreStudent);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "성적 확인이 완료되었습니다.");
        
        return result;
    }

    /**
     * 학생 과정 목록 조회
     */
    public List<CourseDTO> getStudentCourses(String studentId) {
        log.info("학생 과정 목록 조회 - studentId: {}", studentId);
        
        // 학생이 수강하는 모든 과정 조회
        List<MemberEntity> studentMembers = memberRepository.findByMemberId(studentId);
        if (studentMembers.isEmpty()) {
            log.warn("학생 ID {}에 해당하는 멤버 정보를 찾을 수 없습니다.", studentId);
            return new ArrayList<>();
        }
        
        List<CourseDTO> courseDTOs = new ArrayList<>();
        
        for (MemberEntity member : studentMembers) {
            String courseId = member.getCourseId();
            if (courseId != null && !courseId.trim().isEmpty()) {
                try {
                    // 과정 정보 조회
                    CourseEntity course = courseRepository.findById(courseId).orElse(null);
                    if (course != null) {
                        CourseDTO courseDTO = convertToCourseDTO(course, studentId);
                        courseDTOs.add(courseDTO);
                        log.info("과정 정보 조회 성공 - courseId: {}, courseName: {}", courseId, course.getCourseName());
                    } else {
                        log.warn("과정 ID {}에 해당하는 과정 정보를 찾을 수 없습니다.", courseId);
                    }
                } catch (Exception e) {
                    log.error("과정 ID {} 조회 중 오류 발생: {}", courseId, e.getMessage());
                }
            }
        }
        
        log.info("학생 {}의 과정 목록 조회 완료 - 총 {}개 과정", studentId, courseDTOs.size());
        return courseDTOs;
    }

    /**
     * 이메일로 학생 과정 목록 조회
     */
    public List<CourseDTO> getStudentCoursesByEmail(String studentEmail) {
        log.info("이메일로 학생 과정 목록 조회 - studentEmail: {}", studentEmail);
        
        List<MemberEntity> students = memberRepository.findByMemberEmail(studentEmail);
        if (students.isEmpty()) {
            throw new IllegalArgumentException("해당 이메일의 학생을 찾을 수 없습니다: " + studentEmail);
        }
        
        log.info("해당 이메일의 학생 멤버 레코드 수: {}", students.size());
        
        // 모든 멤버 레코드에서 courseId 수집 (중복 제거)
        Set<String> courseIds = new HashSet<>();
        String studentId = null;
        
        for (MemberEntity student : students) {
            if (studentId == null) {
                studentId = student.getMemberId();
            }
            String courseId = student.getCourseId();
            if (courseId != null && !courseId.trim().isEmpty()) {
                courseIds.add(courseId);
                log.info("수집된 courseId: {}", courseId);
            }
        }
        
        log.info("수집된 총 courseId 수: {}", courseIds.size());
        
        // 각 courseId에 대해 과정 정보 조회
        List<CourseDTO> courseDTOs = new ArrayList<>();
        for (String courseId : courseIds) {
            try {
                CourseEntity course = courseRepository.findById(courseId).orElse(null);
                if (course != null) {
                    CourseDTO courseDTO = convertToCourseDTO(course, studentId);
                    courseDTOs.add(courseDTO);
                    log.info("과정 정보 조회 성공 - courseId: {}, courseName: {}", courseId, course.getCourseName());
                } else {
                    log.warn("과정 ID {}에 해당하는 과정 정보를 찾을 수 없습니다.", courseId);
                }
            } catch (Exception e) {
                log.error("과정 ID {} 조회 중 오류 발생: {}", courseId, e.getMessage());
            }
        }
        
        log.info("학생 {}의 과정 목록 조회 완료 - 총 {}개 과정", studentEmail, courseDTOs.size());
        return courseDTOs;
    }

    /**
     * 학생 시험 목록 조회 (통합된 ExamDTO 사용)
     */
    public List<ExamDTO> getStudentExams(String studentId) {
        log.info("=== getStudentExams 메서드 시작 ===");
        log.info("학생 시험 목록 조회 - studentId: {}", studentId);
        
        // studentId가 UUID인지 이메일인지 확인
        List<MemberEntity> students;
        if (studentId.contains("@")) {
            // 이메일인 경우
            students = memberRepository.findByMemberEmail(studentId);
            log.info("=== findByMemberEmail 결과: {}개 레코드 ===", students.size());
        } else {
            // UUID인 경우 (id 컬럼)
            students = memberRepository.findByIdColumn(studentId);
            log.info("=== findByIdColumn 결과: {}개 레코드 ===", students.size());
        }
        
        if (students.isEmpty()) {
            throw new IllegalArgumentException("해당 studentId의 학생을 찾을 수 없습니다: " + studentId);
        }
        
        log.info("해당 studentId의 학생 멤버 레코드 수: {}", students.size());
        
        // 모든 멤버 레코드에서 courseId 수집 (중복 제거)
        Set<String> courseIds = new HashSet<>();
        
        for (MemberEntity student : students) {
            String courseId = student.getCourseId();
            if (courseId != null && !courseId.trim().isEmpty()) {
                courseIds.add(courseId);
                log.info("수집된 courseId: {}", courseId);
            }
        }
        
        log.info("수집된 총 courseId 수: {}", courseIds.size());
        
        // 각 courseId에 대해 시험 템플릿 조회
        List<ExamDTO> examDTOs = new ArrayList<>();
        for (String courseId : courseIds) {
            try {
                // 과정 정보 조회
                CourseEntity course = courseRepository.findById(courseId).orElse(null);
                if (course == null) {
                    log.warn("과정 ID {}에 해당하는 과정 정보를 찾을 수 없습니다.", courseId);
                    continue;
                }
                
                // 과정에 해당하는 서브그룹 조회
                List<SubGroupEntity> subGroups = subGroupRepository.findByCourseId(courseId);
                log.info("과정 {}의 서브그룹 수: {}", courseId, subGroups.size());
                
                for (SubGroupEntity subGroup : subGroups) {
                    // 서브그룹에 해당하는 시험 템플릿 조회 (활성화된 것만)
                    List<TemplateEntity> templates = templateRepository.findBySubGroupIdAndTemplateActive(subGroup.getSubGroupId(), 0);
                    log.info("서브그룹 {}의 활성화된 템플릿 수: {}", subGroup.getSubGroupId(), templates.size());
                    
                                         for (TemplateEntity template : templates) {
                         ExamDTO examDTO = convertToExamDTO(template, studentId);
                         examDTOs.add(examDTO);
                         log.info("시험 정보 조회 성공 - templateId: {}, templateName: {}, courseId: {}", 
                             template.getTemplateId(), template.getTemplateName(), courseId);
                     }
                }
                
            } catch (Exception e) {
                log.error("과정 ID {} 조회 중 오류 발생: {}", courseId, e.getMessage());
            }
        }
        
        log.info("학생 {}의 시험 목록 조회 완료 - 총 {}개 시험", studentId, examDTOs.size());
        return examDTOs;
    }

    /**
     * 시험 시작
     */
    @Transactional
    public Map<String, Object> startExam(String templateId, String studentEmail) {
        log.info("시험 시작 - templateId: {}, studentEmail: {}", templateId, studentEmail);
        
        List<MemberEntity> students = memberRepository.findByMemberEmail(studentEmail);
        if (students.isEmpty()) {
            throw new IllegalArgumentException("해당 이메일의 학생을 찾을 수 없습니다: " + studentEmail);
        }
        
        MemberEntity student = students.get(0);
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId));
        
        // 1. 템플릿 문제 목록 조회
        List<TemplateQuestionEntity> templateQuestions = studentExamRepository.findTemplateQuestionsByTemplateId(templateId);
        log.info("템플릿 문제 조회 결과 - templateId: {}, questions.size: {}", templateId, templateQuestions.size());
        
        // 2. 문제 정보를 QuestionDTO로 변환
        List<QuestionDTO> questions = new ArrayList<>();
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            QuestionEntity question = questionRepository.findById(templateQuestion.getQuestionId()).orElse(null);
            
            if (question != null) {
                // 객관식 선택지 조회
                List<QuestionOptionDTO> options = new ArrayList<>();
                if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                    List<QuestionOptEntity> questionOptions = questionOptRepository.findByQuestionId(question.getQuestionId());
                    options = questionOptions.stream()
                            .map(opt -> QuestionOptionDTO.builder()
                                    .optId(opt.getOptId())
                                    .optText(opt.getOptText())
                                    .optIsCorrect(opt.getOptIsCorrect())
                                    .build())
                            .collect(Collectors.toList());
                }
                
                QuestionDTO questionDTO = QuestionDTO.builder()
                        .questionId(question.getQuestionId())
                        .questionText(question.getQuestionText())
                        .questionType(question.getQuestionType())
                        .questionAnswer(question.getQuestionAnswer())
                        .explanation(question.getExplanation())
                        .codeLanguage(question.getCodeLanguage())
                        .templateQuestionId(templateQuestion.getTemplateQuestionId())
                        .questionScore(templateQuestion.getTemplateQuestionScore()) // 템플릿에서의 문제 배점
                        .instructorId(question.getMemberId())
                        .subDetailId(question.getSubDetailId())
                        .educationId(question.getEducationId())
                        .questionActive(String.valueOf(question.getQuestionActive()))
                        .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                        .updatedAt(TimeZoneUtil.toKoreanTime(question.getUpdatedAt()))
                        .options(options) // 객관식 선택지
                        .build();
                
                questions.add(questionDTO);
            }
        }
        
        // 3. 응답 데이터 구성
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "시험이 시작되었습니다.");
        result.put("templateId", templateId);
        result.put("templateName", template.getTemplateName());
        result.put("templateTime", template.getTemplateTime());
        result.put("templateOpen", template.getTemplateOpen());
        result.put("templateClose", template.getTemplateClose());
        result.put("questions", questions);
        result.put("totalQuestions", questions.size());
        
        return result;
    }

    /**
     * 시험 제출
     */
    @Transactional
    public Map<String, Object> submitExam(String templateId, String courseId, Map<String, String> answers, String studentEmail) {
        return submitExamByEmail(templateId, courseId, answers, studentEmail);
    }

    @Transactional
    public Map<String, Object> submitExamByEmail(String templateId, String courseId, Map<String, String> answers, String studentEmail) {
        log.info("시험 제출 - templateId: {}, courseId: {}, studentEmail: {}", templateId, courseId, studentEmail);
        
        // 1. 학생 정보 조회
        List<MemberEntity> students = memberRepository.findByMemberEmail(studentEmail);
        if (students.isEmpty()) {
            throw new IllegalArgumentException("해당 이메일의 학생을 찾을 수 없습니다: " + studentEmail);
        }
        
        MemberEntity student = students.get(0);
        String studentId = student.getMemberId();
        
        // 2. 시험 템플릿 존재 확인
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("시험 템플릿을 찾을 수 없습니다: " + templateId));
        
        // 3. 시험 문제 목록 조회
        List<TemplateQuestionEntity> templateQuestions = studentExamRepository.findTemplateQuestionsByTemplateId(templateId);
        if (templateQuestions.isEmpty()) {
            throw new IllegalArgumentException("시험 문제를 찾을 수 없습니다: " + templateId);
        }
        
        // 4. 기존 답안이 있는지 확인 (중복 제출 방지)
        List<AnswerEntity> existingAnswers = new ArrayList<>();
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            AnswerEntity existingAnswer = studentExamRepository.findStudentAnswerByTemplateQuestionAndMember(templateQuestion.getTemplateQuestionId(), studentId);
            if (existingAnswer != null) {
                existingAnswers.add(existingAnswer);
            }
        }
        
        if (!existingAnswers.isEmpty()) {
            log.warn("이미 답안을 제출한 시험입니다. templateId: {}, studentId: {}", templateId, studentId);
            throw new IllegalArgumentException("이미 답안을 제출한 시험입니다. 재제출은 불가능합니다.");
        }
        
        // 5. 답안 저장 및 자동 채점
        List<AnswerEntity> savedAnswers = new ArrayList<>();
        int totalScore = 0;
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            String questionId = templateQuestion.getQuestionId();
            String studentAnswer = answers.getOrDefault(questionId, "");
            
            // 문제 정보 조회
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + questionId));
            
            // 자동 채점 (객관식, 참거짓 문제만)
            int score = 0;
            String teacherComment = null;
            
            if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                // 정답 선택지 조회
                List<QuestionOptEntity> correctOptions = questionOptRepository.findCorrectOptionsByQuestionId(questionId);
                if (!correctOptions.isEmpty()) {
                    String correctAnswer = correctOptions.get(0).getOptText();
                    
                    // 정답 여부 확인
                    if (correctAnswer.equals(studentAnswer)) {
                        score = templateQuestion.getTemplateQuestionScore();
                        teacherComment = "자동 채점: 정답";
                    } else {
                        score = 0;
                        teacherComment = "자동 채점: 오답 (정답: " + correctAnswer + ")";
                    }
                }
            } else {
                // 주관식, 서술형 등은 수동 채점 필요
                score = 0;
                teacherComment = "수동 채점 필요";
            }
            
            totalScore += score;
            
            // AnswerEntity 생성 및 저장
            AnswerEntity answer = AnswerEntity.builder()
                    .templateQuestion(templateQuestion)
                    .memberId(studentId)
                    .answerText(studentAnswer)
                    .answerScore(score)
                    .teacherComment(teacherComment)
                    .answerActive(1)
                    .build();
            
            AnswerEntity savedAnswer = studentExamRepository.save(answer);
            
            // 자동 채점된 경우 answerGradedAt 설정
            if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                savedAnswer.setAnswerGradedAt(savedAnswer.getCreatedAt());
                savedAnswer.setAnswerGradeUpdatedAt(savedAnswer.getCreatedAt());
                studentExamRepository.save(savedAnswer);
            }
            
            savedAnswers.add(savedAnswer);
            
            log.info("답안 저장 완료 - questionId: {}, answer: {}, score: {}", questionId, studentAnswer, score);
        }
        
        // 6. 결과 반환
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "시험이 성공적으로 제출되었습니다.");
        result.put("templateId", templateId);
        result.put("studentId", studentId);
        result.put("totalScore", totalScore);
        result.put("submittedAnswers", savedAnswers.size());
        result.put("submittedAt", LocalDateTime.now());
        
        log.info("시험 제출 완료 - studentId: {}, totalScore: {}, submittedAnswers: {}", 
                studentId, totalScore, savedAnswers.size());
        
        return result;
    }

    /**
     * CourseDTO로 변환
     */
    private CourseDTO convertToCourseDTO(CourseEntity course, String studentId) {
        String status = calculateCourseStatus(course);
        
        return CourseDTO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .status(status)
                .build();
    }
    
    /**
     * ExamDTO로 변환
     */
    private ExamDTO convertToExamDTO(TemplateEntity template, String studentId) {
        // 과정 정보 조회
        String courseName = "과정명";
        String courseCode = "";
        String courseId = "";
        
        if (template.getSubGroupId() != null) {
            SubGroupEntity subGroup = subGroupRepository.findById(template.getSubGroupId()).orElse(null);
            if (subGroup != null && subGroup.getCourseId() != null) {
                CourseEntity course = courseRepository.findById(subGroup.getCourseId()).orElse(null);
                if (course != null) {
                    courseName = course.getCourseName();
                    courseCode = course.getCourseCode();
                    courseId = course.getCourseId();
                }
            }
        }
        
        // 강사 이름 조회
        String memberName = null;
        if (template.getMemberId() != null) {
            MemberEntity instructor = memberRepository.findById(template.getMemberId()).orElse(null);
            if (instructor != null) {
                memberName = instructor.getMemberName();
            }
        }
        
        // 학생의 답안 제출 여부 확인 (courseId와 studentId를 고려하여 확인)
        boolean hasSubmitted = false;
        List<AnswerEntity> studentAnswers = new ArrayList<>();
        
        // 해당 courseId와 studentId를 가진 memberId 찾기
        List<MemberEntity> studentMembers;
        if (studentId.contains("@")) {
            // 이메일인 경우
            studentMembers = memberRepository.findByMemberEmail(studentId);
        } else {
            // UUID인 경우 (id 컬럼)
            studentMembers = memberRepository.findByIdColumn(studentId);
        }
        
        String targetMemberId = null;
        
        for (MemberEntity member : studentMembers) {
            if (courseId.equals(member.getCourseId())) {
                targetMemberId = member.getMemberId();
                break;
            }
        }
        
        // 해당 courseId에 맞는 memberId가 있으면 답안 확인
        if (targetMemberId != null) {
            // 1. templateId로 templateQuestionId들 조회
            List<TemplateQuestionEntity> templateQuestions = studentExamRepository.findTemplateQuestionsByTemplateId(template.getTemplateId());
            
            // 2. 각 templateQuestionId로 답안 조회
            for (TemplateQuestionEntity templateQuestion : templateQuestions) {
                AnswerEntity answer = studentExamRepository.findStudentAnswerByTemplateQuestionAndMember(templateQuestion.getTemplateQuestionId(), targetMemberId);
                if (answer != null) {
                    studentAnswers.add(answer);
                }
            }
        }
        hasSubmitted = !studentAnswers.isEmpty();
        
        // 학생의 시험 성적 조회 (courseId와 email을 고려하여 확인)
        ScoreStudentEntity score = null;
        if (targetMemberId != null) {
            score = scoreStudentRepository.findByTemplateIdAndMemberId(template.getTemplateId(), targetMemberId);
        }
        boolean isGraded = score != null;
        
        // 시험 상태 계산 (submitted 상태도 함께 고려)
        String status = calculateExamStatus(template, hasSubmitted, isGraded);
        
        // submitted가 true인데 status가 available인 경우를 방지
        if (hasSubmitted && "available".equals(status)) {
            if (isGraded) {
                status = "completed";
            } else {
                status = "submitted";
            }
        }
        
        return ExamDTO.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .subGroupId(template.getSubGroupId())
                .memberId(template.getMemberId())
                .memberName(memberName) // 강사 이름 추가
                .courseId(courseId)
                .courseName(courseName)
                .courseCode(courseCode)
                .templateOpen(template.getTemplateOpen())
                .templateClose(template.getTemplateClose())
                .templateTime(template.getTemplateTime())
                .templateActive(template.getTemplateActive())
                .status(status)
                .graded(isGraded) // 채점 완료 여부
                .myScore(score != null ? score.getScore() : null)
                .grade(score != null ? calculateGrade(score.getScore()) : null)
                .attempts(hasSubmitted ? 1 : 0) // 답안 제출 여부에 따라 설정
                .maxAttempts(1) // 기본값으로 1 설정
                .submitted(hasSubmitted) // 답안 제출 여부
                .submittedAt(hasSubmitted && !studentAnswers.isEmpty() ? TimeZoneUtil.toKoreanTime(studentAnswers.get(0).getCreatedAt()) : null)
                .isChecked(score != null ? score.getIsChecked() : 0) // 채점 확인 여부
                .build();
    }
    
    /**
     * 과정 상태 계산
     */
    private String calculateCourseStatus(CourseEntity course) {
        // 기존 로직 유지
        return "진행중";
    }
    
    /**
     * 시험 상태 계산
     */
    private String calculateExamStatus(TemplateEntity template, boolean hasSubmitted, boolean isGraded) {
        LocalDateTime now = LocalDateTime.now();
        
        // 답안을 제출한 경우의 상태 우선 처리
        if (hasSubmitted) {
            if (isGraded) {
                return "completed"; // 답안 제출 완료 + 채점 완료
            } else {
                return "submitted"; // 답안 제출 완료 + 채점 대기
            }
        }
        
        // 시험 기간 확인
        if (template.getTemplateOpen() != null && now.isBefore(template.getTemplateOpen())) {
            return "waiting"; // 시험 시작 전
        }
        
        if (template.getTemplateClose() != null && now.isAfter(template.getTemplateClose())) {
            return "expired"; // 시험 종료 + 답안 미제출
        }
        
        // 시험 진행 중
        if (template.getTemplateActive() == 0) {
            // 시험이 활성화된 경우
            if (template.getTemplateOpen() != null && now.isBefore(template.getTemplateOpen())) {
                return "waiting"; // 시험 시작 전
            } else {
                return "ongoing"; // 시험 진행 중 + 답안 미제출
            }
        } else {
            // 시험이 비활성화된 경우
            return "available"; // 시험 비활성화 + 답안 미제출
        }
    }
    
    /**
     * 등급 계산
     */
    private String calculateGrade(double score) {
        if (score >= 90) return "탁월";
        else if (score >= 80) return "우수";
        else if (score >= 70) return "양호";
        else if (score >= 60) return "보통";
        else return "노력";
    }

    @Transactional
    public Map<String, Object> submitExamByUserIdAndCourseId(String templateId, String courseId, Map<String, String> answers, String userId) {
        log.info("시험 제출 (userId/courseId) - templateId: {}, courseId: {}, userId: {}", templateId, courseId, userId);
        
        // 1. userId와 courseId로 memberId 조회
        String memberId = memberRepository.findMemberIdByUserIdAndCourseId(userId, courseId);
        if (memberId == null) {
            throw new IllegalArgumentException("해당 userId와 courseId의 학생을 찾을 수 없습니다: userId=" + userId + ", courseId=" + courseId);
        }
        
        // 2. 시험 템플릿 존재 확인
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("시험 템플릿을 찾을 수 없습니다: " + templateId));
        
        // 3. 시험 문제 목록 조회
        List<TemplateQuestionEntity> templateQuestions = studentExamRepository.findTemplateQuestionsByTemplateId(templateId);
        if (templateQuestions.isEmpty()) {
            throw new IllegalArgumentException("시험 문제를 찾을 수 없습니다: " + templateId);
        }
        
        // 4. 기존 답안이 있는지 확인 (중복 제출 방지)
        List<AnswerEntity> existingAnswers = new ArrayList<>();
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            AnswerEntity existingAnswer = studentExamRepository.findStudentAnswerByTemplateQuestionAndMember(templateQuestion.getTemplateQuestionId(), memberId);
            if (existingAnswer != null) {
                existingAnswers.add(existingAnswer);
            }
        }
        
        if (!existingAnswers.isEmpty()) {
            log.warn("이미 답안을 제출한 시험입니다. templateId: {}, memberId: {}", templateId, memberId);
            throw new IllegalArgumentException("이미 답안을 제출한 시험입니다. 재제출은 불가능합니다.");
        }
        
        // 5. 답안 저장 및 자동 채점
        List<AnswerEntity> savedAnswers = new ArrayList<>();
        int totalScore = 0;
        
        for (TemplateQuestionEntity templateQuestion : templateQuestions) {
            String questionId = templateQuestion.getQuestionId();
            String studentAnswer = answers.getOrDefault(questionId, "");
            
            // 문제 정보 조회
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + questionId));
            
            // 자동 채점 (객관식, 참거짓 문제만)
            int score = 0;
            String teacherComment = null;
            
            if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                // 정답 선택지 조회
                List<QuestionOptEntity> correctOptions = questionOptRepository.findCorrectOptionsByQuestionId(questionId);
                if (!correctOptions.isEmpty()) {
                    String correctAnswer = correctOptions.get(0).getOptText();
                    
                    // 정답 여부 확인
                    if (correctAnswer.equals(studentAnswer)) {
                        score = templateQuestion.getTemplateQuestionScore();
                        teacherComment = "자동 채점: 정답";
                    } else {
                        score = 0;
                        teacherComment = "자동 채점: 오답 (정답: " + correctAnswer + ")";
                    }
                }
            } else {
                // 주관식, 서술형 등은 수동 채점 필요
                score = 0;
                teacherComment = "수동 채점 필요";
            }
            
            totalScore += score;
            
            // AnswerEntity 생성 및 저장
            AnswerEntity answer = AnswerEntity.builder()
                    .templateQuestion(templateQuestion)
                    .memberId(memberId)
                    .answerText(studentAnswer)
                    .answerScore(score)
                    .teacherComment(teacherComment)
                    .answerActive(1)
                    .build();
            
            AnswerEntity savedAnswer = studentExamRepository.save(answer);
            
            // 자동 채점된 경우 answerGradedAt 설정
            if ("객관식".equals(question.getQuestionType()) || "참거짓".equals(question.getQuestionType())) {
                savedAnswer.setAnswerGradedAt(savedAnswer.getCreatedAt());
                savedAnswer.setAnswerGradeUpdatedAt(savedAnswer.getCreatedAt());
                studentExamRepository.save(savedAnswer);
            }
            
            savedAnswers.add(savedAnswer);
            
            log.info("답안 저장 완료 - questionId: {}, answer: {}, score: {}", questionId, studentAnswer, score);
        }
        
        // 6. 결과 반환
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "시험이 성공적으로 제출되었습니다.");
        result.put("templateId", templateId);
        result.put("studentId", memberId);
        result.put("totalScore", totalScore);
        result.put("submittedAnswers", savedAnswers.size());
        result.put("submittedAt", LocalDateTime.now());
        
        log.info("시험 제출 완료 - memberId: {}, totalScore: {}, submittedAnswers: {}", 
                memberId, totalScore, savedAnswers.size());
        
        return result;
    }
} 