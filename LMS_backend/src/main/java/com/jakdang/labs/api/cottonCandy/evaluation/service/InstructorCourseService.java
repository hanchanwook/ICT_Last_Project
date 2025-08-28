package com.jakdang.labs.api.cottonCandy.evaluation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jakdang.labs.api.cottonCandy.course.repository.CourseRepository;
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
public class InstructorCourseService {

    private final CourseRepository courseRepository;
    private final TemplateGroupRepository templateGroupRepository;
    private final MemberRepository memberRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final QuestiontemplateRepository questiontemplateRepository;

        // 선생님이 자신의 모든 강의 조회
    public List<ResponseTemplateGroupDTO> getInstructorCourses(String userId) {
        try {
            // 1. userId로 memberId 조회
            Optional<MemberEntity> memberOpt = memberRepository.findByUserId(userId);
            if (memberOpt.isEmpty()) {
                throw new RuntimeException("사용자를 찾을 수 없습니다.");
            }

            String memberId = memberOpt.get().getMemberId();

            // 2. educationId로 해당 교육기관의 모든 강의 조회 (TemplateGroupService와 동일한 방식)
            String educationId = memberOpt.get().getEducationId();
            List<Object[]> allCourses = courseRepository.findByEducationIdAndCourseActiveWithTemplateGroup(educationId, 0);
            
            // 해당 강사의 강의만 필터링하고 중복 제거
            Map<String, Object[]> uniqueInstructorCourses = new HashMap<>();
            for (Object[] courseData : allCourses) {
                CourseEntity course = (CourseEntity) courseData[0];
                if (course.getMemberId().equals(memberId)) {
                    uniqueInstructorCourses.put(course.getCourseId(), courseData);
                }
            }
            
            List<Object[]> instructorCourses = new ArrayList<>(uniqueInstructorCourses.values());
            
            if (instructorCourses.isEmpty()) {
                return new ArrayList<>();
            }

            List<ResponseTemplateGroupDTO> result = new ArrayList<>();

            for (Object[] courseData : instructorCourses) {
                try {
                    CourseEntity course = (CourseEntity) courseData[0];
                String memberName = (String) courseData[1]; // 강사 이름
                LocalDate openDate = (LocalDate) courseData[2];
                LocalDate closeDate = (LocalDate) courseData[3];
                    
                    // 3. 해당 강의를 듣는 학생 수 조회
                    int studentCount = memberRepository.countByCourseIdAndMemberRole(course.getCourseId(), "ROLE_STUDENT");
                    
                                         // 4. 해당 과정에 템플릿 그룹이 설정되어 있는지 확인 (TemplateGroupService와 동일한 방식)
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
                        // 해당 템플릿 그룹의 상세 정보 조회
                        List<Object[]> templateDetails = templateGroupRepository.findByCourseId(course.getCourseId());
                            
                            List<ResponseTemplateGroupDTO.QuestionItem> questionList = new ArrayList<>();
                            
                            // 해당 템플릿 그룹의 질문들만 필터링
                            for (Object[] templateData : templateDetails) {
                                TemplateGroupEntity detailTemplateGroup = (TemplateGroupEntity) templateData[0];
                                QuestionTemplateEntity questionTemplate = (QuestionTemplateEntity) templateData[1];
                                EvaluationQuestionEntity evaluationQuestion = (EvaluationQuestionEntity) templateData[2];
                                
                                // 현재 처리 중인 템플릿 그룹의 질문들만 추가
                                if (detailTemplateGroup.getQuestionTemplateNum() == templateGroup.getQuestionTemplateNum()) {
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
                            }
                            
                                                    // 해당 템플릿 그룹의 답변 정보 조회 (질문 번호 포함)
                        List<EvaluationResponseEntity> answers = studentEvaluationRepository.findByTemplateGroupId(templateGroup.getTemplateGroupId());
                            
                            List<ResponseTemplateGroupDTO.AnswerItem> answerList = new ArrayList<>();
                            for (EvaluationResponseEntity answer : answers) {
                                // 학생 이름 조회 (MemberRepository에서 findByMemberId 메서드 사용)
                                String studentName = "알 수 없음";
                                try {
                                    // MemberRepository에서 memberId로 조회
                                    Optional<MemberEntity> memberOpt2 = memberRepository.findById(answer.getMemberId());
                                    if (memberOpt2.isPresent()) {
                                        studentName = memberOpt2.get().getMemberName();
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
                    
                } catch (Exception e) {
                    // 예외 처리
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("선생님 강의 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 