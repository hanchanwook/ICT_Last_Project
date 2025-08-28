package com.jakdang.labs.api.cottonCandy.evaluation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jakdang.labs.api.cottonCandy.course.repository.CourseRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestTemplateGroupDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseTemplateGroupDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.TemplateGroupRepository;

import com.jakdang.labs.api.cottonCandy.evaluation.repository.StudentEvaluationRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.EvaluationQuestionRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.QuestiontemplateRepository;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.TemplateGroupEntity;
import com.jakdang.labs.entity.EvaluationQuestionEntity;
import com.jakdang.labs.entity.QuestionTemplateEntity;
import com.jakdang.labs.entity.EvaluationResponseEntity;
import com.jakdang.labs.entity.MemberEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateGroupService {

    private final CourseRepository courseRepository;
    private final TemplateGroupRepository templateGroupRepository;
    private final MemberRepository memberRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final QuestiontemplateRepository questiontemplateRepository;



    // 과정 평가 강의 리스트 조회 = 
    public List<ResponseTemplateGroupDTO> getCourseEvaluationList(String educationId) {
        try {
            // 1. activeId=0인 과정(course) 목록 조회 (TemplateGroup 정보 포함)
            List<Object[]> courses = courseRepository.findByEducationIdAndCourseActiveWithTemplateGroup(educationId, 0);
            List<ResponseTemplateGroupDTO> result = new ArrayList<>();

            for (Object[] courseData : courses) {
                CourseEntity course = (CourseEntity) courseData[0];
                String memberName = (String) courseData[1]; // 강사 이름
                LocalDate openDate = (LocalDate) courseData[2];
                LocalDate closeDate = (LocalDate) courseData[3];
                
                // openDate, closeDate가 null인 경우 기본값 설정
                if (openDate == null) {
                    openDate = course.getCourseStartDay(); // 과정 시작일을 기본값으로 사용
                }
                if (closeDate == null) {
                    closeDate = course.getCourseEndDay(); // 과정 종료일을 기본값으로 사용
                }
                
                // 2. 해당 강의를 듣는 학생 수 조회
                int studentCount = memberRepository.countByCourseIdAndMemberRole(course.getCourseId(), "ROLE_STUDENT");
                
                // 3. 해당 과정에 템플릿 그룹이 설정되어 있는지 확인
                List<TemplateGroupEntity> templateGroups = templateGroupRepository.findByCourseIdSimple(course.getCourseId());
                
                // 과정별로 하나의 DTO 생성 (여러 템플릿 그룹 포함)
                ResponseTemplateGroupDTO courseItem = ResponseTemplateGroupDTO.builder()
                    .templateGroupId("") // 과정 레벨이므로 빈 값
                    .courseId(course.getCourseId())
                    .courseCode(course.getCourseCode())
                    .courseName(course.getCourseName())
                    .maxCapacity(course.getMaxCapacity())
                    .courseStartDay(course.getCourseStartDay())
                    .courseEndDay(course.getCourseEndDay())
                    .memberId(course.getMemberId())
                    .memberName(memberName)
                    .educationId(course.getEducationId())
                    .studentCount(studentCount)
                    .evaluationTemplates(new ArrayList<>()) // 평가 템플릿 리스트 초기화
                    .build();
                
                if (templateGroups.isEmpty()) {
                    // 템플릿 그룹이 없는 경우: 빈 evaluationTemplates 리스트로 반환
                } else {
                    // 템플릿 그룹이 있는 경우: 각 템플릿 그룹을 evaluationTemplates에 추가
                    for (TemplateGroupEntity templateGroup : templateGroups) {
                        // 해당 템플릿 그룹의 상세 정보 조회 (특정 템플릿 그룹 ID로 필터링)
                        List<Object[]> templateDetails = templateGroupRepository.findByTemplateGroupIdWithQuestions(templateGroup.getTemplateGroupId());
                        
                        List<ResponseTemplateGroupDTO.QuestionItem> questionList = new ArrayList<>();
                        
                        // 해당 템플릿 그룹의 질문들만 필터링
                        for (Object[] templateData : templateDetails) {
                            TemplateGroupEntity detailTemplateGroup = (TemplateGroupEntity) templateData[0];
                            QuestionTemplateEntity questionTemplate = (QuestionTemplateEntity) templateData[1];
                            EvaluationQuestionEntity evaluationQuestion = (EvaluationQuestionEntity) templateData[2];
                            
                            // findByTemplateGroupIdWithQuestions는 이미 특정 템플릿 그룹으로 필터링된 결과이므로
                            // 모든 질문을 추가
                            questionList.add(ResponseTemplateGroupDTO.QuestionItem.builder()
                                .questionNum(questionTemplate.getQuestionNum())
                                .evalQuestionId(evaluationQuestion.getEvalQuestionId())
                                .evalQuestionText(evaluationQuestion.getEvalQuestionText())
                                .evalQuestionType(evaluationQuestion.getEvalQuestionType())
                                .templateGroupId(templateGroup.getTemplateGroupId())
                                .questionTemplateName(questionTemplate.getQuestionTemplateName())
                                .questionTemplateNum(questionTemplate.getQuestionTemplateNum())
                                .openDate(templateGroup.getOpenDate())
                                .closeDate(templateGroup.getCloseDate())
                                .createdAt(evaluationQuestion.getCreatedAt() != null ? evaluationQuestion.getCreatedAt().toString() : null)
                                .build());
                        }
                        
                        // 해당 템플릿 그룹의 답변 정보 조회 (질문 번호 포함)
                        List<EvaluationResponseEntity> answers = studentEvaluationRepository.findByTemplateGroupId(templateGroup.getTemplateGroupId());
                        
                        List<ResponseTemplateGroupDTO.AnswerItem> answerList = new ArrayList<>();
                        for (EvaluationResponseEntity answer : answers) {
                            // 학생 이름 조회 (MemberRepository에서 findByMemberId 메서드 사용)
                            String studentName = "알 수 없음";
                            try {
                                // MemberRepository에서 memberId로 조회
                                Optional<MemberEntity> memberOpt = memberRepository.findById(answer.getMemberId());
                                if (memberOpt.isPresent()) {
                                    studentName = memberOpt.get().getMemberName();
                                }
                            } catch (Exception e) {
                                // 예외 처리
                            }
                            
                            // 질문 정보 조회 (QuestionTemplate과 EvaluationQuestion에서 가져오기)
                            int questionNum = 0;
                            String evalQuestionText = "";
                            int evalQuestionType = 0;
                            
                            try {
                                // evalQuestionId로 QuestionTemplate 조회
                                List<QuestionTemplateEntity> questionTemplates = questiontemplateRepository.findByEvalQuestionId(answer.getEvalQuestionId());
                                if (!questionTemplates.isEmpty()) {
                                    // 첫 번째 템플릿의 questionNum 사용
                                    questionNum = questionTemplates.get(0).getQuestionNum();
                                }
                                
                                // evalQuestionId로 EvaluationQuestion 조회하여 텍스트와 타입 가져오기
                                Optional<EvaluationQuestionEntity> questionOpt = evaluationQuestionRepository.findById(answer.getEvalQuestionId());
                                if (questionOpt.isPresent()) {
                                    EvaluationQuestionEntity question = questionOpt.get();
                                    evalQuestionText = question.getEvalQuestionText();
                                    evalQuestionType = question.getEvalQuestionType();
                                }
                            } catch (Exception e) {
                                // 예외 처리
                            }
                            
                            answerList.add(ResponseTemplateGroupDTO.AnswerItem.builder()
                                .responseId(answer.getEvalResultId()) // evalResultId가 실제 ID
                                .memberId(answer.getMemberId())
                                .memberName(studentName)
                                .evalQuestionId(answer.getEvalQuestionId())
                                .questionNum(questionNum) // 질문 번호 추가
                                .evalQuestionText(evalQuestionText) // 평가항목 내용 추가
                                .evalQuestionType(evalQuestionType) // 평가항목 타입 추가
                                .answerText(answer.getAnswerText())
                                .score(answer.getScore()) // score는 int 타입이므로 null 체크 불필요
                                .createdAt(answer.getCreatedAt() != null ? answer.getCreatedAt().toString() : null)
                                .templateGroupId(answer.getTemplateGroupId())
                                .build());
                        }
                        
                        // 템플릿 이름 조회
                        String questionTemplateName = "";
                        try {
                            // questionTemplateNum으로 템플릿 이름 조회
                            List<QuestionTemplateEntity> questionTemplates = questiontemplateRepository.findByQuestionTemplateNum(templateGroup.getQuestionTemplateNum());
                            if (!questionTemplates.isEmpty()) {
                                questionTemplateName = questionTemplates.get(0).getQuestionTemplateName();
                            }
                        } catch (Exception e) {
                            // 예외 처리
                        }
                        
                                                 // EvaluationTemplate 생성하여 과정의 evaluationTemplates에 추가
                         ResponseTemplateGroupDTO.EvaluationTemplate evaluationTemplate = ResponseTemplateGroupDTO.EvaluationTemplate.builder()
                             .templateGroupId(templateGroup.getTemplateGroupId())
                             .openDate(templateGroup.getOpenDate())
                             .closeDate(templateGroup.getCloseDate())
                             .questionTemplateNum(templateGroup.getQuestionTemplateNum())
                             .questionTemplateName(questionTemplateName)
                             .questionList(questionList)
                             .answerList(answerList)
                             .build();
                         
                        courseItem.getEvaluationTemplates().add(evaluationTemplate);
                    }
                }
                
                // 과정을 결과 리스트에 추가
                result.add(courseItem);
            }

        return result;
        } catch (Exception e) {
            throw new RuntimeException("과정 평가 강의 리스트 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 과정에 템플릿 설정(평가 일정 설정)
    public ResponseTemplateGroupDTO createTemplateGroup(RequestTemplateGroupDTO dto) {
        try {
            // 필수 필드 검증
            if (dto.getCourseId() == null || dto.getCourseId().isEmpty()) {
                throw new IllegalArgumentException("courseId는 필수입니다.");
            }
            
            if (dto.getOpenDate() == null) {
                throw new IllegalArgumentException("openDate는 필수입니다.");
            }

            if (dto.getCloseDate() == null) {
                throw new IllegalArgumentException("closeDate는 필수입니다.");
            }
            
            if (dto.getQuestionTemplateNum() <= 0) {
                throw new IllegalArgumentException("questionTemplateNum은 0보다 커야 합니다.");
            }
            
            // TemplateGroup 생성
            TemplateGroupEntity templateGroup = TemplateGroupEntity.builder()
                .courseId(dto.getCourseId())
                .openDate(dto.getOpenDate())
                .closeDate(dto.getCloseDate())
                .questionTemplateNum(dto.getQuestionTemplateNum())
                .build();
                
                         TemplateGroupEntity savedTemplateGroup = templateGroupRepository.save(templateGroup);
             List<ResponseTemplateGroupDTO.EvaluationTemplate> evaluationTemplates = new ArrayList<>();
             evaluationTemplates.add(ResponseTemplateGroupDTO.EvaluationTemplate.builder()
                 .templateGroupId(savedTemplateGroup.getTemplateGroupId())
                 .openDate(savedTemplateGroup.getOpenDate())
                 .closeDate(savedTemplateGroup.getCloseDate())
                 .questionTemplateNum(savedTemplateGroup.getQuestionTemplateNum())
                 .build());
            
            return ResponseTemplateGroupDTO.builder()
                .templateGroupId(savedTemplateGroup.getTemplateGroupId())
                .courseId(savedTemplateGroup.getCourseId())
                .evaluationTemplates(evaluationTemplates)
                .build();
        } catch (Exception e) {
            throw e;
        }
    }
    

    // 과정에 설정된 템플릿 그룹 수정(평가 일정 수정)
    public ResponseTemplateGroupDTO updateTemplateGroup(RequestTemplateGroupDTO dto) {
        // 필수 필드 검증
        if (dto.getTemplateGroupId() == null || dto.getTemplateGroupId().isEmpty()) {
            throw new IllegalArgumentException("templateGroupId는 필수입니다.");
        }
        if (dto.getCourseId() == null || dto.getCourseId().isEmpty()) {
            throw new IllegalArgumentException("courseId는 필수입니다.");
        }
        
        TemplateGroupEntity templateGroup = templateGroupRepository.findByTemplateGroupId(dto.getTemplateGroupId());
        if (templateGroup == null) {
            throw new IllegalArgumentException("템플릿 그룹을 찾을 수 없습니다. templateGroupId: " + dto.getTemplateGroupId());
        }
        
        // 기존 ID를 유지하면서 수정
        String originalTemplateGroupId = templateGroup.getTemplateGroupId();
        
        templateGroup.setCourseId(dto.getCourseId());
        templateGroup.setOpenDate(dto.getOpenDate());
        templateGroup.setCloseDate(dto.getCloseDate());
        templateGroup.setQuestionTemplateNum(dto.getQuestionTemplateNum());
        
        // ID가 변경되지 않도록 명시적으로 설정
        templateGroup.setTemplateGroupId(originalTemplateGroupId);
        
        TemplateGroupEntity updatedTemplateGroup = templateGroupRepository.save(templateGroup);
        
        return ResponseTemplateGroupDTO.builder()
            .templateGroupId(updatedTemplateGroup.getTemplateGroupId())
            .build();
    }

    // 과정에 설정된 템플릿 그룹 삭제(일정 삭제)
    public void deleteTemplateGroup(String templateGroupId) {
        try {
            // 1. 먼저 해당 템플릿 그룹이 존재하는지 확인
            TemplateGroupEntity templateGroup = templateGroupRepository.findByTemplateGroupId(templateGroupId);
            if (templateGroup == null) {
                throw new IllegalArgumentException("템플릿 그룹을 찾을 수 없습니다. templateGroupId: " + templateGroupId);
            }
            
            // 2. 해당 템플릿 그룹과 관련된 평가 응답들 삭제
            List<EvaluationResponseEntity> responses = studentEvaluationRepository.findByTemplateGroupId(templateGroupId);
            if (!responses.isEmpty()) {
                studentEvaluationRepository.deleteAll(responses);
            }
            
            // 3. 템플릿 그룹 삭제
            templateGroupRepository.deleteById(templateGroupId);
        } catch (Exception e) {
            throw new RuntimeException("템플릿 그룹 삭제 중 에러가 발생했습니다.", e);
        }
    }
    
}