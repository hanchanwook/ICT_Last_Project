package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.*;
import com.jakdang.labs.api.lnuyasha.repository.TemplateQuestionRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.KySubjectDetailRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyCourseRepository;
import com.jakdang.labs.api.lnuyasha.repository.SubGroupRepository;
import com.jakdang.labs.api.lnuyasha.repository.AnswerRepository;
import com.jakdang.labs.api.lnuyasha.repository.QuestionRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.ScoreStudentRepository;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.TemplateQuestionEntity;
import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.QuestionEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Set;
import com.jakdang.labs.api.lnuyasha.util.TimeZoneUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    
    private final TemplateRepository templateRepository;
    private final TemplateQuestionRepository templateQuestionRepository;
    private final QuestionBankService questionBankService;
    private final KySubjectDetailRepository subjectDetailRepository;
    @Qualifier("lnuyashaSubGroupRepository")
    private final SubGroupRepository subGroupRepository;
    private final KyCourseRepository courseRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final KyMemberRepository memberRepository;
    private final ScoreStudentRepository scoreStudentRepository;
    // private final MemberService memberService;
    
    /**
     * 시험 템플릿 생성
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateResponseDTO createTemplate(TemplateCreateRequestDTO request) {
        
        // 필수 필드 검증
        if (request.getTemplateName() == null || request.getTemplateName().trim().isEmpty()) {
            throw new IllegalArgumentException("시험명은 필수입니다.");
        }
        if (request.getMemberId() == null || request.getMemberId().trim().isEmpty()) {
            throw new IllegalArgumentException("강사 ID는 필수입니다.");
        }
        if (request.getSubGroupId() == null || request.getSubGroupId().trim().isEmpty()) {
            throw new IllegalArgumentException("subGroupId는 필수입니다.");
        }
        if (request.getEducationId() == null || request.getEducationId().trim().isEmpty()) {
            throw new IllegalArgumentException("교육기관 ID는 필수입니다.");
        }
        
        // 템플릿 엔티티 생성
        TemplateEntity template = TemplateEntity.builder()
                .templateName(request.getTemplateName()) // String 타입으로 직접 저장
                .templateTime(request.getTemplateTime() != null ? request.getTemplateTime() : 60) // 기본값 60분
                .templateOpen(request.getTemplateOpen())
                .templateClose(request.getTemplateClose())
                .templateNumber(request.getTemplateNumber() != null ? request.getTemplateNumber() : 0) // 기본값 0
                .templateActive(request.getTemplateActive() != null ? request.getTemplateActive() : 0) // 기본값 0
                .memberId(request.getMemberId())
                .subGroupId(request.getSubGroupId())
                .educationId(request.getEducationId())
                .build();
        
        TemplateEntity savedTemplate = templateRepository.save(template);
        String templateId = savedTemplate.getTemplateId();
        
        return convertToTemplateResponseDTO(savedTemplate, new ArrayList<>());
    }
    
    
    /**
     * 시험-문제 연결 생성
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateResponseDTO createTemplateQuestions(TemplateQuestionCreateRequestDTO request) {
        return createTemplateQuestionsWithRetry(request, 3); // 최대 3번 재시도
    }
    
    private TemplateResponseDTO createTemplateQuestionsWithRetry(TemplateQuestionCreateRequestDTO request, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                
                // 1. 템플릿 존재 확인
                TemplateEntity template = templateRepository.findById(request.getTemplateId())
                        .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + request.getTemplateId()));
                
                // 2. 기존 문제 연결 삭제
                List<TemplateQuestionEntity> existingQuestions = templateQuestionRepository.findByTemplateId(request.getTemplateId());
                templateQuestionRepository.deleteAll(existingQuestions);
                
                // 3. 새 문제 생성 및 연결
                List<TemplateQuestionEntity> savedQuestions = new ArrayList<>();
                
                if (request.getNewQuestions() != null && !request.getNewQuestions().isEmpty()) {
                    for (var newQuestion : request.getNewQuestions()) {
                        // 새 문제 생성 (QuestionMappingDTO는 단순 매핑 정보이므로 실제 문제 생성 로직은 별도로 처리)
                        // 여기서는 기존 문제 ID를 사용한다고 가정
                        
                        // 템플릿-문제 연결 생성
                        TemplateQuestionEntity templateQuestion = TemplateQuestionEntity.builder()
                                .templateId(request.getTemplateId())
                                .questionId(newQuestion.getQuestionId())
                                .templateQuestionScore(newQuestion.getScore() != null ? newQuestion.getScore() : 0)
                                .build();
                        
                        TemplateQuestionEntity savedQuestion = templateQuestionRepository.save(templateQuestion);
                        savedQuestions.add(savedQuestion);
                    }
                }
                
                // 4. 기존 문제 연결
                if (request.getBankQuestions() != null && !request.getBankQuestions().isEmpty()) {
                    for (var bankQuestion : request.getBankQuestions()) {
                        TemplateQuestionEntity templateQuestion = TemplateQuestionEntity.builder()
                                .templateId(request.getTemplateId())
                                .questionId(bankQuestion.getQuestionId())
                                .templateQuestionScore(bankQuestion.getScore() != null ? bankQuestion.getScore() : 0)
                                .build();
                        
                        TemplateQuestionEntity savedQuestion = templateQuestionRepository.save(templateQuestion);
                        savedQuestions.add(savedQuestion);
                    }
                }
                
                // templateNumber를 실제 연결된 문제 수로 업데이트
                if (template.getTemplateNumber() != savedQuestions.size()) {
                    template.setTemplateNumber(savedQuestions.size());
                    templateRepository.save(template);
                }
                
                return convertToTemplateResponseDTO(template, savedQuestions);
                
            } catch (Exception e) {
                log.error("시험-문제 연결 생성 시도 {} 실패: {}", attempt, e.getMessage(), e);
                if (attempt == maxRetries) {
                    throw e;
                }
                // 잠시 대기 후 재시도
                try {
                    Thread.sleep(1000 * attempt); // 1초, 2초, 3초 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
        
        throw new RuntimeException("최대 재시도 횟수를 초과했습니다.");
    }
    
    /**
     * 시험 템플릿 상세 조회
     */
    @Transactional(readOnly = true)
    public TemplateResponseDTO getTemplateDetail(String templateId) {
        log.info("=== 시험 템플릿 상세 조회 시작 ===");
        log.info("templateId: {}", templateId);
        
        // 템플릿 조회
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + templateId));
        
        // 템플릿 문제 목록 조회
        List<TemplateQuestionEntity> questions = templateQuestionRepository.findByTemplateId(templateId);
        
        // DTO로 변환
        TemplateResponseDTO result = convertToTemplateResponseDTO(template, questions);
        
        return result;
    }
    
    /**
     * 내 시험 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TemplateResponseDTO> getMyTemplates(String memberId, String educationId) {
        // 1. 모든 시험 조회 (디버깅용)
        List<TemplateEntity> allTemplates = templateRepository.findAll();
        
        // 2. memberId만으로 조회
        List<TemplateEntity> memberTemplates = templateRepository.findByMemberIdAndActive(memberId);
        
        // 3. educationId만으로 조회
        List<TemplateEntity> educationTemplates = templateRepository.findByEducationIdAndActive(educationId);
        
        // 4. memberId와 educationId 모두로 조회
        List<TemplateEntity> templates = templateRepository.findByMemberIdAndEducationIdAndActive(memberId, educationId);
        
        // 5. 디버깅을 위한 추가 조회
        // memberId만으로 조회 (templateActive 조건 제외)
        List<TemplateEntity> allMemberTemplates = templateRepository.findAll().stream()
                .filter(t -> t.getMemberId().equals(memberId))
                .collect(Collectors.toList());
        
        // educationId만으로 조회 (templateActive 조건 제외)
        List<TemplateEntity> allEducationTemplates = templateRepository.findAll().stream()
                .filter(t -> t.getEducationId().equals(educationId))
                .collect(Collectors.toList());
        
        List<TemplateResponseDTO> result = new ArrayList<>();
        
        for (TemplateEntity template : templates) {
            List<TemplateQuestionEntity> questions = templateQuestionRepository.findByTemplateId(template.getTemplateId());
            result.add(convertToTemplateResponseDTO(template, questions));
        }
        
        return result;
    }
    
    /**
     * 시험 상세 조회
     */
    
    
    /**
     * TemplateEntity를 TemplateResponseDTO로 변환
     */
    private TemplateResponseDTO convertToTemplateResponseDTO(TemplateEntity template, List<TemplateQuestionEntity> questions) {
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        
        for (TemplateQuestionEntity question : questions) {
            // 문제 상세 정보 조회
            QuestionDTO questionDTO = null;
            
            try {
                var questionEntity = questionBankService.getQuestionDetail(question.getQuestionId());
                if (questionEntity != null) {
                    questionDTO = QuestionDTO.builder()
                            .templateQuestionId(question.getTemplateQuestionId())
                            .questionId(questionEntity.getQuestionId())
                            .questionText(questionEntity.getQuestionText())
                            .questionType(questionEntity.getQuestionType())
                            .questionAnswer(questionEntity.getQuestionAnswer())
                            .explanation(questionEntity.getExplanation())
                            .questionScore(null) // QuestionEntity에는 원래 배점이 없음
                            .questionActive(questionEntity.getQuestionActive())
                            .options(questionEntity.getOptions())
                            .subDetailId(questionEntity.getSubDetailId())
                            .subDetailName(questionEntity.getSubDetailName())
                            .subjectId(questionEntity.getSubjectId())
                            .subjectName(questionEntity.getSubjectName())
                            .instructorId(questionEntity.getInstructorId())
                            .instructorName(questionEntity.getInstructorName())
                            .educationId(questionEntity.getEducationId())
                            .codeLanguage(questionEntity.getCodeLanguage())
                            .templateId(questionEntity.getTemplateId())
                            .templateQuestionScore(question.getTemplateQuestionScore())
                            .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                            .updatedAt(TimeZoneUtil.toKoreanTime(question.getUpdatedAt()))
                            .questionNumber(questionEntity.getQuestionNumber())
                            .correctCount(questionEntity.getCorrectCount())
                            .incorrectCount(questionEntity.getIncorrectCount())
                            .noAnswerCount(questionEntity.getNoAnswerCount())
                            .correctRate(questionEntity.getCorrectRate())
                            .examId(questionEntity.getExamId())
                            .examName(questionEntity.getExamName())
                            .questionStats(questionEntity.getQuestionStats())
                            .totalQuestions(questionEntity.getTotalQuestions())
                            .avgCorrectRate(questionEntity.getAvgCorrectRate())
                            .build();
                } else {
                    // 문제 상세 정보가 없는 경우 기본 정보만 설정
                    questionDTO = QuestionDTO.builder()
                            .templateQuestionId(question.getTemplateQuestionId())
                            .questionId(question.getQuestionId())
                            .templateQuestionScore(question.getTemplateQuestionScore())
                            .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                            .build();
                }
            } catch (Exception e) {
                log.warn("문제 상세 정보 조회 실패: questionId={}, error={}", question.getQuestionId(), e.getMessage());
                // 오류 발생 시 기본 정보만 설정
                questionDTO = QuestionDTO.builder()
                        .templateQuestionId(question.getTemplateQuestionId())
                        .questionId(question.getQuestionId())
                        .templateQuestionScore(question.getTemplateQuestionScore())
                        .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                        .build();
            }
            
            questionDTOs.add(questionDTO);
        }
        
        // 과정 정보 조회 (더 안전한 방식으로 수정)
        String courseId = null;
        String courseName = null;
        String courseCode = null;
        
        if (template.getSubGroupId() != null && !template.getSubGroupId().trim().isEmpty()) {
            try {
                var subGroup = subGroupRepository.findBySubGroupId(template.getSubGroupId());
                if (subGroup != null) {
                    courseId = subGroup.getCourseId();
                    
                    // courseId로 과정 정보 조회
                    if (courseId != null && !courseId.trim().isEmpty()) {
                        var course = courseRepository.findById(courseId).orElse(null);
                        if (course != null) {
                            courseName = course.getCourseName();
                            courseCode = course.getCourseCode();
                        } else {
                            log.warn("courseId에 해당하는 과정을 찾을 수 없음: courseId={}", courseId);
                        }
                    } else {
                        log.warn("subGroup에서 courseId가 null 또는 빈 문자열: subGroupId={}", template.getSubGroupId());
                    }
                } else {
                    log.warn("subGroupId에 해당하는 subGroup을 찾을 수 없음: subGroupId={}", template.getSubGroupId());
                }
            } catch (Exception e) {
                log.error("과정 정보 조회 중 오류 발생: subGroupId={}, error={}", template.getSubGroupId(), e.getMessage(), e);
            }
        }
        
        // 학생 응시 현황 계산
        Map<String, Object> examStats = calculateExamStatistics(template.getTemplateId(), courseId);
        
        // 해당 과정에 등록된 학생 수 계산
        int studentsCount = 0;
        if (courseId != null && !courseId.trim().isEmpty()) {
            try {
                List<MemberEntity> courseStudents = memberRepository.findActiveStudentsByCourseId(courseId);
                studentsCount = courseStudents.size();
                log.info("과정 학생 수 조회 완료: courseId={}, studentsCount={}", courseId, studentsCount);
            } catch (Exception e) {
                log.error("과정 학생 수 조회 중 오류 발생: courseId={}, error={}", courseId, e.getMessage(), e);
            }
        }
        
        return TemplateResponseDTO.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName()) // String 타입으로 직접 사용
                .templateTime(template.getTemplateTime())
                .templateOpen(template.getTemplateOpen())
                .templateClose(template.getTemplateClose())
                .templateNumber(template.getTemplateNumber())
                .templateActive(template.getTemplateActive())
                .memberId(template.getMemberId())
                .subGroupId(template.getSubGroupId())
                .educationId(template.getEducationId())
                .courseId(courseId)
                .courseName(courseName)
                .courseCode(courseCode)
                .createdAt(TimeZoneUtil.toKoreanTime(template.getCreatedAt()))
                .updatedAt(TimeZoneUtil.toKoreanTime(template.getUpdatedAt()))
                .questions(questionDTOs)
                .totalQuestions(questionDTOs.size())  // 총 문제 수 설정
                .participants((Integer) examStats.get("participants"))
                .submittedCount((Integer) examStats.get("submittedCount"))
                .totalStudents((Integer) examStats.get("totalStudents"))
                .submissionRate((String) examStats.get("submissionRate"))
                .gradedCount((Integer) examStats.get("gradedCount"))
                .gradingRate((String) examStats.get("gradingRate"))
                .averageScore((Double) examStats.get("averageScore"))
                .unsubmittedCount((Integer) examStats.get("unsubmittedCount"))
                .students(studentsCount)
                .participantCount((Integer) examStats.get("totalStudents"))
                .waitingForGradingCount((Integer) examStats.get("waitingForGradingCount"))
                .build();
    }
    
    /**
     * 시험 템플릿 수정
     * @param templateId 템플릿 ID
     * @param request 수정 요청 데이터
     * @param memberId 강사 ID (권한 확인용)
     * @return 수정된 템플릿 정보
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateResponseDTO updateTemplate(String templateId, TemplateCreateRequestDTO request, String memberId) {
        
        try {
            // 1. 시험 템플릿 조회
            TemplateEntity template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("시험 템플릿을 찾을 수 없습니다: " + templateId));
            
            // 2. 권한 확인 (시험 소유자만 수정 가능)
            if (!template.getMemberId().equals(memberId)) {
                throw new RuntimeException("시험 템플릿 수정 권한이 없습니다. 시험 소유자만 수정할 수 있습니다.");
            }
            
            // 3. 시험 상태 확인 (진행 중인 시험은 수정 불가)
            LocalDateTime now = LocalDateTime.now();
            if (template.getTemplateOpen() != null && template.getTemplateClose() != null) {
                if (now.isAfter(template.getTemplateOpen()) && now.isBefore(template.getTemplateClose())) {
                    throw new RuntimeException("진행 중인 시험은 수정할 수 없습니다.");
                }
            }
            
            // 4. 템플릿 정보 업데이트
            if (request.getTemplateName() != null) {
                template.setTemplateName(request.getTemplateName());
            }
            if (request.getTemplateTime() != null) {
                template.setTemplateTime(request.getTemplateTime());
            }
            if (request.getTemplateOpen() != null) {
                template.setTemplateOpen(request.getTemplateOpen());
            }
            if (request.getTemplateClose() != null) {
                template.setTemplateClose(request.getTemplateClose());
            }
            if (request.getTemplateNumber() != null) {
                template.setTemplateNumber(request.getTemplateNumber());
            }
            if (request.getSubGroupId() != null) {
                template.setSubGroupId(request.getSubGroupId());
            }
            
            TemplateEntity savedTemplate = templateRepository.save(template);
            
            return convertToTemplateResponseDTO(savedTemplate, new ArrayList<>());
            
        } catch (Exception e) {
            log.error("시험 템플릿 수정 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 템플릿 수정 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 시험 열기/닫기 업데이트
     * @param templateId 템플릿 ID
     * @param request 업데이트 요청 데이터
     * @param memberId 강사 ID (권한 확인용)
     * @return 업데이트된 템플릿 정보
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateResponseDTO updateTemplateOpenClose(String templateId, TemplateUpdateRequestDTO request, String memberId) {
        
        try {
            // 1. 시험 템플릿 조회
            TemplateEntity template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("시험 템플릿을 찾을 수 없습니다: " + templateId));
            
            // 2. 권한 확인 (시험 소유자만 수정 가능)
            if (!template.getMemberId().equals(memberId)) {
                throw new RuntimeException("시험 템플릿 수정 권한이 없습니다. 시험 소유자만 수정할 수 있습니다.");
            }
            
            // 3. 시험 상태 확인 (진행 중인 시험은 수정 불가)
            LocalDateTime now = LocalDateTime.now();
            if (template.getTemplateOpen() != null && template.getTemplateClose() != null) {
                if (now.isAfter(template.getTemplateOpen()) && now.isBefore(template.getTemplateClose())) {
                    throw new RuntimeException("진행 중인 시험은 수정할 수 없습니다.");
                }
            }
            
            // 4. 열기/닫기 시간 업데이트
                        if (request.getTemplateOpen() != null) {
                template.setTemplateOpen(request.getTemplateOpen());
            }
            
            if (request.getTemplateClose() != null) {
                template.setTemplateClose(request.getTemplateClose());
            }
            
            TemplateEntity savedTemplate = templateRepository.save(template);
            
            return convertToTemplateResponseDTO(savedTemplate, new ArrayList<>());
            
        } catch (Exception e) {
            log.error("시험 열기/닫기 업데이트 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 열기/닫기 업데이트 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 시험-문제 연결 수정 (주석 처리됨 - 관련 DTO 삭제로 인해)
     * @param templateId 템플릿 ID
     * @param request 수정 요청 데이터
     * @param memberId 강사 ID (권한 확인용)
     * @return 수정 결과
     */
    /*
    @Transactional(rollbackFor = Exception.class)
    public TemplateQuestionUpdateResponseDTO updateTemplateQuestions(String templateId, TemplateQuestionUpdateRequestDTO request, String memberId) {
        // 주석 처리됨 - TemplateQuestionUpdateResponseDTO와 TemplateQuestionUpdateRequestDTO가 삭제됨
    }
    */
    
    /**
     * 시험 템플릿 비활성화
     * @param templateId 템플릿 ID
     * @param memberId 강사 ID (권한 확인용)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deactivateTemplate(String templateId, String memberId) {
        
        try {
            // 1. 시험 템플릿 조회
            TemplateEntity template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("시험 템플릿을 찾을 수 없습니다: " + templateId));
            
            // 2. 권한 확인 (시험 소유자만 비활성화 가능)
            if (!template.getMemberId().equals(memberId)) {
                throw new RuntimeException("시험 템플릿 비활성화 권한이 없습니다. 시험 소유자만 비활성화할 수 있습니다.");
            }
            
            // 3. 시험 상태 확인 (진행 중인 시험은 비활성화 불가)
            LocalDateTime now = LocalDateTime.now();
            if (template.getTemplateOpen() != null && template.getTemplateClose() != null) {
                if (now.isAfter(template.getTemplateOpen()) && now.isBefore(template.getTemplateClose())) {
                    throw new RuntimeException("진행 중인 시험은 비활성화할 수 없습니다.");
                }
            }
            
            // 4. 시험 비활성화 (templateActive를 1로 설정)
            template.setTemplateActive(1);
            templateRepository.save(template);
            
        } catch (Exception e) {
            log.error("시험 템플릿 비활성화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 템플릿 비활성화 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 시험 템플릿 응시 학생 목록 조회
     * @param templateId 템플릿 ID
     * @return 학생 목록 (현재는 빈 리스트 반환)
     */
    @Transactional(readOnly = true)
    public List<Object> getTemplateStudents(String templateId) {
        // TODO: 실제 학생 목록 조회 로직 구현 필요
        // 현재는 빈 리스트 반환
        return new ArrayList<>();
    }
    
    /**
     * 통합 시험 생성 (새 문제 생성 + 시험 템플릿 생성 + 시험-문제 연결)
     * @param request 통합 시험 생성 요청
     * @return 생성된 시험 정보
     */
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ExamDTO createCompleteExam(ExamCreateCompleteRequestDTO request) {
        return createCompleteExamWithRetry(request, 3); // 최대 3번 재시도
    }
    
    /**
     * 재시도 로직이 포함된 통합 시험 생성
     */
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ExamDTO createCompleteExamWithRetry(ExamCreateCompleteRequestDTO request, int maxRetries) {
        // examData null 체크
        if (request.getExamData() == null) {
            log.error("examData가 null입니다.");
            throw new IllegalArgumentException("시험 데이터(examData)가 누락되었습니다.");
        }
        
        try {
            // 1. 새 문제들 생성 (같은 트랜잭션 내에서 처리)
            List<String> newQuestionIds = new ArrayList<>();
            if (request.getNewQuestions() != null && !request.getNewQuestions().isEmpty()) {
                log.info("새 문제 생성 시작: {}개", request.getNewQuestions().size());
                
                for (int i = 0; i < request.getNewQuestions().size(); i++) {
                    var newQuestion = request.getNewQuestions().get(i);
                    log.info("새 문제 {} 생성: {}", i + 1, newQuestion.getQuestionText());
                    
                    // educationId 검증 및 설정
                    if (newQuestion.getEducationId() == null || newQuestion.getEducationId().trim().isEmpty()) {
                        newQuestion.setEducationId(request.getExamData().getEducationId());
                        log.info("새 문제 {}에 educationId 설정: {}", i + 1, request.getExamData().getEducationId());
                    }
                    
                    // subDetailId로 subDetailName 조회
                    String subDetailName = null;
                    if (newQuestion.getSubDetailId() != null && !newQuestion.getSubDetailId().trim().isEmpty()) {
                        try {
                            var subDetail = subjectDetailRepository.findById(newQuestion.getSubDetailId());
                            if (subDetail.isPresent()) {
                                subDetailName = subDetail.get().getSubDetailName();
                                log.info("subDetailId {}에 해당하는 subDetailName 조회: {}", newQuestion.getSubDetailId(), subDetailName);
                            } else {
                                log.warn("subDetailId {}에 해당하는 세부과목을 찾을 수 없습니다.", newQuestion.getSubDetailId());
                            }
                        } catch (Exception e) {
                            log.error("subDetailName 조회 중 오류: {}", e.getMessage(), e);
                        }
                    }
                    
                    // QuestionCreateRequestDTO로 변환
                    QuestionCreateRequestDTO questionRequest = QuestionCreateRequestDTO.builder()
                            .questionText(newQuestion.getQuestionText())
                            .questionType(newQuestion.getQuestionType())
                            .questionAnswer(newQuestion.getQuestionAnswer())
                            .explanation(newQuestion.getExplanation())
                            .subDetailId(newQuestion.getSubDetailId())
                            .subDetailName(subDetailName)
                            .codeLanguage(newQuestion.getCodeLanguage())
                            .questionActive(newQuestion.getQuestionActive())
                            .instructorId(newQuestion.getMemberId() != null ? newQuestion.getMemberId() : request.getExamData().getMemberId())
                            .educationId(newQuestion.getEducationId() != null ? newQuestion.getEducationId() : request.getExamData().getEducationId())
                            .options(convertToQuestionOptions(newQuestion.getOptions()))
                            .build();
                    
                    // 같은 트랜잭션 내에서 문제 생성
                    // 문제 생성에는 사용자 로그인 ID를 사용 (QuestionValidator에서 id 컬럼으로 조회하기 때문)
                    String instructorId = request.getNewQuestions().get(i).getMemberId();
                    QuestionDTO createdQuestion = createQuestionInSameTransaction(
                            instructorId, questionRequest);
                    String questionId = createdQuestion.getQuestionId();
                    newQuestionIds.add(questionId);
                    log.info("=== 새 문제 {} 생성 완료 ===", i + 1);
                    log.info("questionId (UUID): {}", questionId);
                    log.info("questionText: {}", createdQuestion.getQuestionText());
                    log.info("questionType: {}", createdQuestion.getQuestionType());
                }
            }
            
            // 2. 시험 템플릿 생성
            log.info("시험 템플릿 생성 시작");
            
            String templateName = request.getExamData().getTemplateName();
            if (templateName == null || templateName.trim().isEmpty()) {
                templateName = "새로운 시험";
                log.info("시험명이 없어서 기본값으로 설정: {}", templateName);
            }
            
            // 필수 필드 검증 및 로깅
            log.info("=== 템플릿 생성 데이터 검증 ===");
            log.info("templateName: {}", templateName);
            log.info("templateTime: {}", request.getExamData().getTemplateTime());
            log.info("templateOpen: {}", request.getExamData().getTemplateOpen());
            log.info("templateClose: {}", request.getExamData().getTemplateClose());
            log.info("subGroupId: {}", request.getExamData().getSubGroupId());
            log.info("memberId: {}", request.getExamData().getMemberId());
            log.info("educationId: {}", request.getExamData().getEducationId());
            
            // subGroupId 검증 및 설정
            String subGroupId = request.getExamData().getSubGroupId();
            String courseId = request.getExamData().getCourseId();
            String subjectId = request.getExamData().getSubjectId();
            
            // subGroupId가 없고 courseId와 subjectId가 있으면 해당하는 subGroupId 찾기
            if ((subGroupId == null || subGroupId.trim().isEmpty()) && 
                courseId != null && !courseId.trim().isEmpty() && 
                subjectId != null && !subjectId.trim().isEmpty()) {
                
                try {
                    var subGroup = subGroupRepository.findByCourseIdAndSubjectId(courseId, subjectId);
                    if (subGroup != null) {
                        subGroupId = subGroup.getSubGroupId();
                        log.info("courseId와 subjectId로 subGroupId 찾음: courseId={}, subjectId={}, subGroupId={}", 
                                courseId, subjectId, subGroupId);
                    } else {
                        log.warn("courseId와 subjectId에 해당하는 subGroup을 찾을 수 없음: courseId={}, subjectId={}", 
                                courseId, subjectId);
                    }
                } catch (Exception e) {
                    log.error("courseId와 subjectId로 subGroupId 조회 중 오류: courseId={}, subjectId={}, error={}", 
                            courseId, subjectId, e.getMessage(), e);
                }
            } else if (subGroupId != null && !subGroupId.trim().isEmpty()) {
                log.info("요청에서 받은 subGroupId 사용: {}", subGroupId);
            } else {
                log.info("subGroupId가 null로 설정됨");
            }
            
            // educationId 검증
            String educationId = request.getExamData().getEducationId();
            if (educationId == null || educationId.trim().isEmpty()) {
                educationId = "1";
                log.info("educationId가 없어서 기본값으로 설정: {}", educationId);
                request.getExamData().setEducationId(educationId);
            } else {
                log.info("요청에서 받은 educationId 사용: {}", educationId);
            }
            
            // templateOpen, templateClose - null 허용
            LocalDateTime templateOpen = request.getExamData().getTemplateOpen();
            LocalDateTime templateClose = request.getExamData().getTemplateClose();
            
            if (templateOpen == null) {
                log.info("templateOpen이 null로 설정됨");
            }
            
            if (templateClose == null) {
                log.info("templateClose가 null로 설정됨");
            }
            
            // 실제 문항 수 계산
            int newQuestionsCount = request.getNewQuestions() != null ? request.getNewQuestions().size() : 0;
            int bankQuestionsCount = request.getBankQuestions() != null ? request.getBankQuestions().size() : 0;
            int totalQuestionsCount = newQuestionsCount + bankQuestionsCount;
            
            log.info("=== 문항 수 계산 ===");
            log.info("새 문제 수: {}", newQuestionsCount);
            log.info("기존 문제 수: {}", bankQuestionsCount);
            log.info("총 문항 수: {}", totalQuestionsCount);
            
            TemplateCreateRequestDTO templateRequest = TemplateCreateRequestDTO.builder()
                    .templateName(templateName)
                    .templateTime(request.getExamData().getTemplateTime())
                    .templateOpen(templateOpen)
                    .templateClose(templateClose)
                    .templateNumber(totalQuestionsCount) // 실제 문항 수로 설정
                    .templateActive(request.getExamData().getTemplateActive() != null ? request.getExamData().getTemplateActive() : 0)
                    .memberId(request.getExamData().getMemberId())
                    .subGroupId(subGroupId)
                    .educationId(educationId)
                    .build();
            
            TemplateResponseDTO createdTemplate = createTemplate(templateRequest);
            String templateId = createdTemplate.getTemplateId();
            log.info("=== 시험 템플릿 생성 완료 ===");
            log.info("templateId (UUID): {}", templateId);
            log.info("templateName: {}", createdTemplate.getTemplateName());
            log.info("templateTime: {}분", createdTemplate.getTemplateTime());
            log.info("templateOpen: {}", createdTemplate.getTemplateOpen());
            log.info("templateClose: {}", createdTemplate.getTemplateClose());
            log.info("저장된 memberId (UUID): {}", createdTemplate.getMemberId());
            log.info("subGroupId: {}", createdTemplate.getSubGroupId());
            log.info("educationId: {}", createdTemplate.getEducationId());
            
            // 3. 시험-문제 연결 생성
            log.info("시험-문제 연결 생성 시작");
            
            // 새로 생성된 문제들 연결
            List<QuestionMappingDTO> newQuestionMappings = new ArrayList<>();
            log.info("=== 새로 생성된 문제들 연결 시작 ===");
            for (int i = 0; i < newQuestionIds.size(); i++) {
                String questionId = newQuestionIds.get(i);
                Integer score = request.getNewQuestions().get(i).getTemplateQuestionScore();
                newQuestionMappings.add(QuestionMappingDTO.builder()
                        .questionId(questionId)
                        .score(score)
                        .build());
                log.info("새 문제 {} 연결: questionId = {}, score = {}", i + 1, questionId, score);
            }
            log.info("새로 생성된 문제 연결 완료: {}개", newQuestionMappings.size());
            
            // 기존 문제은행 문제들 연결
            List<QuestionMappingDTO> bankQuestionMappings = new ArrayList<>();
            log.info("=== 기존 문제은행 문제들 연결 시작 ===");
            if (request.getBankQuestions() != null) {
                for (int i = 0; i < request.getBankQuestions().size(); i++) {
                    var bankQuestion = request.getBankQuestions().get(i);
                    bankQuestionMappings.add(QuestionMappingDTO.builder()
                            .questionId(bankQuestion.getQuestionId())
                            .score(bankQuestion.getTemplateQuestionScore())
                            .build());
                    log.info("기존 문제 {} 연결: questionId = {}, score = {}", i + 1, bankQuestion.getQuestionId(), bankQuestion.getTemplateQuestionScore());
                }
            }
            log.info("기존 문제은행 문제 연결 완료: {}개", bankQuestionMappings.size());
            
            TemplateQuestionCreateRequestDTO templateQuestionRequest = TemplateQuestionCreateRequestDTO.builder()
                    .templateId(templateId)
                    .newQuestions(newQuestionMappings)
                    .bankQuestions(bankQuestionMappings)
                    .build();
            
            log.info("=== 시험-문제-배점 연결 생성 시작 ===");
            log.info("templateId: {}", templateId);
            log.info("새 문제 개수: {}", newQuestionMappings.size());
            log.info("기존 문제 개수: {}", bankQuestionMappings.size());
            
            createTemplateQuestions(templateQuestionRequest);
            log.info("=== 시험-문제-배점 연결 생성 완료 ===");
            log.info("새 문제 {}개, 기존 문제 {}개 연결됨", newQuestionMappings.size(), bankQuestionMappings.size());
            
            // 4. 응답 생성
            int totalQuestions = newQuestionMappings.size() + bankQuestionMappings.size();
            ExamDTO response = ExamDTO.builder()
                    .templateId(templateId)
                    .newQuestions(newQuestionMappings.size())
                    .bankQuestions(bankQuestionMappings.size())
                    .totalQuestions(totalQuestions)
                    .success(true)
                    .message("시험 생성 완료")
                    .build();
            
            log.info("=== 통합 시험 생성 완료 ===");
            log.info("최종 templateId (UUID): {}", response.getTemplateId());
            log.info("생성된 새 문제: {}개", response.getNewQuestions());
            log.info("연결된 기존 문제: {}개", response.getBankQuestions());
            log.info("총 문제 수: {}개", response.getTotalQuestions());
            log.info("=== 모든 UUID 연결 완료 ===");
            
            return response;
            
        } catch (org.springframework.transaction.TransactionSystemException e) {
            log.error("트랜잭션 시스템 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 생성 실패: 트랜잭션 오류가 발생했습니다", e);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.error("낙관적 락 충돌 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 생성 실패: 동시성 충돌이 발생했습니다. 다시 시도해주세요.", e);
        } catch (Exception e) {
            log.error("통합 시험 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 같은 트랜잭션 내에서 문제 생성
     */
    public QuestionDTO createQuestionInSameTransaction(String instructorId, QuestionCreateRequestDTO request) {
        return questionBankService.createQuestion(instructorId, request);
    }
    
    /**
     * ExamCreateCompleteRequestDTO의 options를 QuestionOptionDTO로 변환
     */
    private List<QuestionOptionDTO> convertToQuestionOptions(List<ExamCreateCompleteRequestDTO.QuestionOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }
        
        return options.stream()
                .map(option -> QuestionOptionDTO.builder()
                        .optText(option.getOptText())
                        .optIsCorrect(option.getOptIsCorrect())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 답안 채점 (강사용)
     */
    @Transactional
    public void gradeAnswer(String answerId, Integer score, String teacherComment, String memberId) {
        log.info("답안 채점 시작: answerId={}, score={}, teacherComment={}, memberId={}", 
                answerId, score, teacherComment, memberId);
        
        try {
            // 답안 조회
            AnswerEntity answer = answerRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException("답안을 찾을 수 없습니다: " + answerId));
            
            // 문제 정보 조회
            QuestionEntity question = questionRepository.findByQuestionId(answer.getTemplateQuestion().getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + answer.getTemplateQuestion().getQuestionId()));
            
            // 서술형/코드형 문제만 수동 채점 가능
            if ("객관식".equals(question.getQuestionType())) {
                throw new IllegalArgumentException("객관식 문제는 자동 채점되므로 수동 채점할 수 없습니다.");
            }
            
            // 점수 업데이트
            answer.setAnswerScore(score);
            answer.setTeacherComment(teacherComment);
            answer.setAnswerGradedAt(LocalDateTime.now());  // 채점 완료 시간 설정
            answer.setAnswerGradeUpdatedAt(LocalDateTime.now());  // 채점 수정 시간 설정
            
            answerRepository.save(answer);
            
            // 해당 학생의 ScoreStudentEntity의 graded 값을 0으로 업데이트 (채점 완료)
            String templateId = answer.getTemplateQuestion().getTemplateQuestionId().split("_")[0]; // templateQuestionId에서 templateId 추출
            ScoreStudentEntity scoreStudent = scoreStudentRepository.findByTemplateIdAndMemberId(templateId, answer.getMemberId());
            if (scoreStudent != null) {
                scoreStudent.setGraded(0); // 채점 완료 (0: 채점 완료, 1: 미채점)
                scoreStudentRepository.save(scoreStudent);
                log.info("ScoreStudentEntity graded 값 업데이트: scoreStudentId={}, graded={}", 
                        scoreStudent.getScoreStudentId(), scoreStudent.getGraded());
            }
            
            log.info("답안 채점 완료: answerId={}, score={}", answerId, score);
            
        } catch (Exception e) {
            log.error("답안 채점 중 오류 발생: answerId={}, error={}", answerId, e.getMessage(), e);
            throw new RuntimeException("답안 채점 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 시험 통계 정보 계산 (실제 수강생 기준)
     */
    private Map<String, Object> calculateExamStatistics(String templateId, String courseId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 1. 해당 과정의 실제 수강생 수 조회
            int totalStudents = 0;
            if (courseId != null && !courseId.trim().isEmpty()) {
                List<MemberEntity> courseStudents = memberRepository.findActiveStudentsByCourseId(courseId);
                totalStudents = courseStudents.size();
                log.info("과정 수강생 수 조회: courseId={}, totalStudents={}", courseId, totalStudents);
            }
            
            // 2. 해당 시험의 모든 답안 조회
            List<AnswerEntity> allAnswers = answerRepository.findByTemplateId(templateId);
            
            // 3. 응시한 학생 목록 (중복 제거)
            List<String> participantMemberIds = allAnswers.stream()
                    .map(AnswerEntity::getMemberId)
                    .distinct()
                    .collect(Collectors.toList());
            
            int participants = participantMemberIds.size();
            int submittedCount = participants; // 응시한 학생 = 제출한 학생
            
            // 4. 각 학생별로 모든 답안이 채점 완료되었는지 확인
            int fullyGradedStudents = 0;
            int waitingForGradingStudents = 0; // 채점 대기 학생 수
            double totalStudentScore = 0.0;
            
            for (String memberId : participantMemberIds) {
                // 해당 학생의 모든 답안 조회
                List<AnswerEntity> studentAnswers = allAnswers.stream()
                        .filter(answer -> memberId.equals(answer.getMemberId()))
                        .collect(Collectors.toList());
                
                // 해당 학생의 모든 답안이 채점 완료되었는지 확인
                boolean allGraded = studentAnswers.stream()
                        .allMatch(answer -> answer.getAnswerGradedAt() != null);
                
                if (allGraded) {
                    fullyGradedStudents++;
                    // 학생의 총점 계산
                    int studentTotalScore = studentAnswers.stream()
                            .mapToInt(AnswerEntity::getAnswerScore)
                            .sum();
                    totalStudentScore += studentTotalScore;
                } else {
                    // 답안은 제출했지만 아직 채점이 완료되지 않은 학생
                    waitingForGradingStudents++;
                }
            }
            
            // 5. 평균 점수 계산 (채점 완료된 학생들만)
            double averageScore = fullyGradedStudents > 0 ? 
                    Math.round((totalStudentScore / fullyGradedStudents) * 10.0) / 10.0 : 0.0;
            
            // 6. 제출률: 제출한 학생 수 / 전체 수강생 수
            String submissionRate = totalStudents > 0 ?
                    String.format("%.0f%%", (submittedCount * 100.0 / totalStudents)) : "0%";
            
            // 7. 채점률: 모든 답안이 채점 완료된 학생 수 / 제출한 학생 수
            String gradingRate = submittedCount > 0 ?
                    String.format("%.0f%%", (fullyGradedStudents * 100.0 / submittedCount)) : "0%";
            
            // 8. 미제출 학생 수
            int unsubmittedCount = totalStudents - submittedCount;
            
            stats.put("participants", participants);
            stats.put("submittedCount", submittedCount);
            stats.put("totalStudents", totalStudents);
            stats.put("submissionRate", submissionRate);
            stats.put("gradedCount", fullyGradedStudents);
            stats.put("gradingRate", gradingRate);
            stats.put("averageScore", averageScore);
            stats.put("unsubmittedCount", unsubmittedCount);
            stats.put("waitingForGradingCount", waitingForGradingStudents); // 채점 대기 학생 수
            
            log.info("시험 통계 계산 완료: templateId={}, courseId={}, totalStudents={}, participants={}, submittedCount={}, fullyGradedStudents={}, waitingForGradingStudents={}, submissionRate={}, gradingRate={}", 
                    templateId, courseId, totalStudents, participants, submittedCount, fullyGradedStudents, waitingForGradingStudents, submissionRate, gradingRate);
            
        } catch (Exception e) {
            log.error("시험 통계 계산 중 오류 발생: templateId={}, courseId={}, error={}", templateId, courseId, e.getMessage(), e);
            // 기본값 설정
            stats.put("participants", 0);
            stats.put("submittedCount", 0);
            stats.put("totalStudents", 0);
            stats.put("submissionRate", "0%");
            stats.put("gradedCount", 0);
            stats.put("gradingRate", "0%");
            stats.put("averageScore", 0.0);
            stats.put("unsubmittedCount", 0);
            stats.put("waitingForGradingCount", 0);
        }
        
        return stats;
    }

    /**
     * 시험 종료 처리 - 미제출 학생 자동 0점 처리
     * @param templateId 시험 템플릿 ID
     * @param memberId 강사 ID (권한 확인용)
     * @return 시험 종료 처리 결과
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamDTO endExam(String templateId, String memberId) {
        log.info("=== 시험 종료 처리 요청 ===");
        log.info("templateId: {}, memberId: {}", templateId, memberId);
        
        try {
            // 1. 시험 템플릿 조회
            TemplateEntity template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("시험 템플릿을 찾을 수 없습니다: " + templateId));
            
            // 2. 권한 확인 (시험 소유자만 종료 가능)
            if (!template.getMemberId().equals(memberId)) {
                throw new RuntimeException("시험 종료 권한이 없습니다. 시험 소유자만 종료할 수 있습니다.");
            }
            
            // 3. 시험 종료 시간 확인
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime templateClose = template.getTemplateClose();
            
            if (templateClose == null) {
                throw new RuntimeException("시험 종료 시간이 설정되지 않았습니다.");
            }
            
            if (now.isBefore(templateClose)) {
                throw new RuntimeException("아직 시험 종료 시간이 되지 않았습니다. 종료 시간: " + templateClose);
            }
            
            // 4. 과정 정보 조회 (courseId 추출)
            String courseId = null;
            if (template.getSubGroupId() != null && !template.getSubGroupId().trim().isEmpty()) {
                try {
                    var subGroup = subGroupRepository.findBySubGroupId(template.getSubGroupId());
                    if (subGroup != null) {
                        courseId = subGroup.getCourseId();
                        log.info("과정 ID 조회: {}", courseId);
                    }
                } catch (Exception e) {
                    log.warn("과정 정보 조회 실패: {}", e.getMessage());
                }
            }
            
            // 5. 과정에 등록된 모든 학생 조회
            List<MemberEntity> allStudents = new ArrayList<>();
            if (courseId != null && !courseId.trim().isEmpty()) {
                try {
                    allStudents = memberRepository.findActiveStudentsByCourseId(courseId);
                    log.info("과정 학생 수: {}", allStudents.size());
                } catch (Exception e) {
                    log.error("과정 학생 조회 실패: {}", e.getMessage(), e);
                }
            }
            
            // 6. 이미 제출한 학생 목록 조회
            List<AnswerEntity> submittedAnswers = answerRepository.findByTemplateId(templateId);
            Set<String> submittedMemberIds = submittedAnswers.stream()
                    .map(AnswerEntity::getMemberId)
                    .collect(Collectors.toSet());
            
            log.info("제출한 학생 수: {}", submittedMemberIds.size());
            
            // 7. 미제출 학생 목록 생성
            List<MemberEntity> unsubmittedStudents = allStudents.stream()
                    .filter(student -> !submittedMemberIds.contains(student.getMemberId()))
                    .collect(Collectors.toList());
            
            log.info("미제출 학생 수: {}", unsubmittedStudents.size());
            
            // 8. 미제출 학생들을 자동으로 0점 처리
            int autoSubmittedCount = 0;
            for (MemberEntity student : unsubmittedStudents) {
                try {
                    // 이미 scoreStudent 레코드가 있는지 확인
                    ScoreStudentEntity existingScore = scoreStudentRepository.findByTemplateIdAndMemberId(templateId, student.getMemberId());
                    
                    if (existingScore == null) {
                        // 새로운 scoreStudent 레코드 생성
                        ScoreStudentEntity scoreStudent = ScoreStudentEntity.builder()
                                .templateId(templateId)
                                .memberId(student.getMemberId())
                                .score(0) // 0점
                                .isChecked(1) // 학생 미확인
                                .totalComment("시험 종료로 인한 자동 0점 처리")
                                .graded(1) // 채점완료
                                .build();
                        
                        scoreStudentRepository.save(scoreStudent);
                        autoSubmittedCount++;
                        
                        log.info("미제출 학생 자동 0점 처리: memberId={}", student.getMemberId());
                    } else {
                        log.info("이미 점수 기록이 있는 학생: memberId={}", student.getMemberId());
                    }
                } catch (Exception e) {
                    log.error("미제출 학생 자동 0점 처리 실패: memberId={}, error={}", student.getMemberId(), e.getMessage(), e);
                }
            }
            
            log.info("=== 시험 종료 처리 완료 ===");
            log.info("자동 처리된 미제출 학생 수: {}", autoSubmittedCount);
            log.info("전체 학생 수: {}", allStudents.size());
            
            return ExamDTO.builder()
                    .templateId(templateId)
                    .templateClose(templateClose)
                    .autoSubmittedCount(autoSubmittedCount)
                    .totalStudents(allStudents.size())
                    .success(true)
                    .message("시험 종료 처리 완료")
                    .build();
            
        } catch (Exception e) {
            log.error("시험 종료 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("시험 종료 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 시험 종료 및 미제출 학생 자동 처리
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamDTO closeWithAutoSubmission(String templateId, String templateClose, List<String> studentMemberIds, String memberId) {
        log.info("=== Exam close with auto submission request ===");
        log.info("templateId: {}, templateClose: {}, studentMemberIds: {}, memberId: {}", 
                templateId, templateClose, studentMemberIds, memberId);
        
        try {
            // 1. 시험 템플릿 조회
            TemplateEntity template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("시험 템플릿을 찾을 수 없습니다: " + templateId));
            log.info("Template found successfully: templateName={}", template.getTemplateName());
            
            // 2. 권한 확인 (시험 소유자만 종료 가능)
            if (!template.getMemberId().equals(memberId)) {
                throw new RuntimeException("시험 종료 권한이 없습니다. 시험 소유자만 종료할 수 있습니다.");
            }
            log.info("Permission check successful: requester={}, template owner={}", memberId, template.getMemberId());
            
            // 3. templateClose 시간을 LocalDateTime으로 변환
            LocalDateTime closeTime;
            try {
                if (templateClose != null && !templateClose.trim().isEmpty()) {
                    // ISO 8601 형식의 문자열을 LocalDateTime으로 변환
                    closeTime = LocalDateTime.parse(templateClose.replace("Z", ""));
                    log.info("templateClose parsing successful: {}", closeTime);
                } else {
                    closeTime = LocalDateTime.now();
                    log.info("templateClose is empty, using current time: {}", closeTime);
                }
            } catch (Exception e) {
                log.warn("templateClose parsing failed, using current time: {}", e.getMessage());
                closeTime = LocalDateTime.now();
            }
            
            // 4. 시험 템플릿 상태 업데이트
            template.setTemplateClose(closeTime);
            template.setTemplateActive(0); // 시험 비활성화 (종료)
            templateRepository.save(template);
            log.info("Template close time and status updated successfully: {}", closeTime);
            
            // 5. templateQuestionId 목록 조회 (PK 값들)
            List<TemplateQuestionEntity> templateQuestions = templateQuestionRepository.findByTemplateId(templateId);
            if (templateQuestions.isEmpty()) {
                log.warn("No questions found for template: templateId={}", templateId);
                return ExamDTO.builder()
                        .templateId(templateId)
                        .templateClose(closeTime)
                        .autoSubmittedCount(0)
                        .totalStudents(studentMemberIds.size())
                        .studentMemberIds(studentMemberIds)
                        .success(true)
                        .message("시험 종료 처리 완료 (문제 없음)")
                        .build();
            }
            
            log.info("Template questions found successfully: question count={}", templateQuestions.size());
            for (TemplateQuestionEntity tq : templateQuestions) {
                log.info("Question info: templateQuestionId={}, questionId={}, score={}", 
                        tq.getTemplateQuestionId(), tq.getQuestionId(), tq.getTemplateQuestionScore());
            }
            
            // 6. 미제출 학생 식별 및 처리
            int autoSubmittedCount = 0;
            log.info("Starting unsubmitted student identification: student count={}", studentMemberIds.size());
            
            for (String studentMemberId : studentMemberIds) {
                log.info("Processing student: memberId={}", studentMemberId);
                try {
                    // 해당 학생의 미제출 문제 식별
                    List<String> unsubmittedQuestionIds = new ArrayList<>();
                    
                    for (TemplateQuestionEntity templateQuestion : templateQuestions) {
                        // answer 테이블에서 제출 기록 확인
                        AnswerEntity existingAnswer = answerRepository.findByTemplateQuestionIdAndMemberId(
                                templateQuestion.getTemplateQuestionId(), studentMemberId);
                        
                        if (existingAnswer == null) {
                            // 미제출 문제 발견
                            unsubmittedQuestionIds.add(templateQuestion.getTemplateQuestionId());
                            log.info("Unsubmitted question found: templateQuestionId={}, memberId={}", 
                                    templateQuestion.getTemplateQuestionId(), studentMemberId);
                        } else {
                            log.info("Submitted question found: answerId={}, templateQuestionId={}, memberId={}", 
                                    existingAnswer.getAnswerId(), templateQuestion.getTemplateQuestionId(), studentMemberId);
                        }
                    }
                    
                    // 미제출 문제가 있는 경우에만 처리
                    if (!unsubmittedQuestionIds.isEmpty()) {
                        log.info("Student has unsubmitted questions: memberId={}, unsubmitted count={}", 
                                studentMemberId, unsubmittedQuestionIds.size());
                        
                        // 6-1. answer 테이블에 미제출 기록 생성
                        for (String templateQuestionId : unsubmittedQuestionIds) {
                            // templateQuestionId로 TemplateQuestionEntity 찾기
                            TemplateQuestionEntity templateQuestion = templateQuestions.stream()
                                    .filter(tq -> tq.getTemplateQuestionId().equals(templateQuestionId))
                                    .findFirst()
                                    .orElse(null);
                            
                            if (templateQuestion != null) {
                                AnswerEntity answer = AnswerEntity.builder()
                                        .templateQuestion(templateQuestion)
                                        .memberId(studentMemberId)
                                        .answerText("") // 빈 답안
                                        .answerScore(0) // 0점
                                        .teacherComment("시험 종료로 인한 자동 0점 처리")
                                        .answerGradedAt(closeTime) // 시험 종료 시간
                                        .createdAt(closeTime) // 생성 시간 (submittedAt 역할)
                                        .build();
                                
                                AnswerEntity savedAnswer = answerRepository.save(answer);
                                log.info("Unsubmitted answer created: answerId={}, templateQuestionId={}, memberId={}", 
                                        savedAnswer.getAnswerId(), templateQuestionId, studentMemberId);
                            }
                        }
                        
                        // 6-2. scorestudent 테이블에 0점 기록 생성
                        ScoreStudentEntity scoreStudent = ScoreStudentEntity.builder()
                                .templateId(templateId)
                                .memberId(studentMemberId)
                                .score(0) // 0점
                                .isChecked(1) // 채점 완료, 학생 확인 가능
                                .graded(0) // 채점 완료 (0: 채점 완료, 1: 미채점)
                                .totalComment("시험 종료로 인한 자동 0점 처리")
                                .build();
                        
                        ScoreStudentEntity savedScore = scoreStudentRepository.save(scoreStudent);
                        log.info("Score record created: scoreStudentId={}, templateId={}, memberId={}, score={}", 
                                savedScore.getScoreStudentId(), templateId, studentMemberId, savedScore.getScore());
                        
                        autoSubmittedCount++;
                    } else {
                        log.info("Student has no unsubmitted questions: memberId={}", studentMemberId);
                    }
                    
                } catch (Exception e) {
                    log.error("Student auto-submission processing failed: memberId={}, error={}", studentMemberId, e.getMessage(), e);
                }
            }
            
            log.info("=== Exam close with auto submission completed ===");
            log.info("Auto-submitted student count: {}", autoSubmittedCount);
            log.info("Total student count: {}", studentMemberIds.size());
            
            return ExamDTO.builder()
                    .templateId(templateId)
                    .templateClose(closeTime)
                    .autoSubmittedCount(autoSubmittedCount)
                    .totalStudents(studentMemberIds.size())
                    .studentMemberIds(studentMemberIds)
                    .success(true)
                    .message("시험 종료 및 미제출 학생 자동 처리 완료")
                    .build();
            
        } catch (Exception e) {
            log.error("Exam close with auto submission failed: {}", e.getMessage(), e);
            throw new RuntimeException("시험 종료 및 미제출 학생 자동 처리 실패: " + e.getMessage(), e);
        }
    }

}
