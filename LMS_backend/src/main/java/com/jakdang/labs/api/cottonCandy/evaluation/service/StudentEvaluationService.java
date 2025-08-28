package com.jakdang.labs.api.cottonCandy.evaluation.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.api.cottonCandy.course.repository.CourseRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseStudentCourseDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseStudentEvaluationDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.RequestEvaluationAnswerDTO;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.StudentEvaluationRepository;
import com.jakdang.labs.api.cottonCandy.evaluation.repository.TemplateGroupRepository;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.TemplateGroupEntity;
import com.jakdang.labs.entity.QuestionTemplateEntity;
import com.jakdang.labs.entity.EvaluationQuestionEntity;
import com.jakdang.labs.entity.EvaluationResponseEntity;
import com.jakdang.labs.entity.MemberEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jakdang.labs.api.cottonCandy.evaluation.dto.ResponseStudentEvaluationDetailDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentEvaluationService {
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final TemplateGroupRepository templateGroupRepository;


    
    // 학생의 해당 템플릿 평가 항목 조회
    public ResponseStudentEvaluationDTO findByTemplateGroupId(String templateGroupId) {
        // 1. templateGroupId로 템플릿 그룹과 질문들을 가져옴
        List<Object[]> questionData = templateGroupRepository.findByTemplateGroupIdWithQuestions(templateGroupId);
        
        if (questionData.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 템플릿 그룹입니다.");
        }
        
        // 2. 첫 번째 데이터에서 템플릿 그룹과 질문 템플릿 정보 추출
        Object[] firstData = questionData.get(0);
        TemplateGroupEntity templateGroup = (TemplateGroupEntity) firstData[0];
        QuestionTemplateEntity questionTemplate = (QuestionTemplateEntity) firstData[1];
        
        if (templateGroup == null || questionTemplate == null) {
            throw new IllegalArgumentException("템플릿 정보를 찾을 수 없습니다.");
        }
        
        // 3. ResponseStudentEvaluationDTO 생성
        ResponseStudentEvaluationDTO response = new ResponseStudentEvaluationDTO();
        response.setTemplateGroupId(templateGroup.getTemplateGroupId());
        response.setOpenDate(templateGroup.getOpenDate());
        response.setCloseDate(templateGroup.getCloseDate());
        response.setQuestionTemplateName(questionTemplate.getQuestionTemplateName());
        response.setQuestionTemplateNum(questionTemplate.getQuestionTemplateNum());
        response.setQuestionList(new ArrayList<>());
        
        // 4. 각 질문을 QuestionItem으로 변환
        for (Object[] data : questionData) {
            QuestionTemplateEntity qt = (QuestionTemplateEntity) data[1];
            EvaluationQuestionEntity eq = (EvaluationQuestionEntity) data[2];
            
            if (eq != null) {
                ResponseStudentEvaluationDTO.QuestionItem questionItem = ResponseStudentEvaluationDTO.QuestionItem.builder()
                    .evalQuestionId(eq.getEvalQuestionId())
                    .evalQuestionText(eq.getEvalQuestionText())
                    .evalQuestionType(eq.getEvalQuestionType())
                    .questionNum(qt.getQuestionNum())
                    .build();
                
                response.getQuestionList().add(questionItem);
            }
        }
        
        return response;
    }
    

    // 학생 평가 답변 등록
    @Transactional
    public ResponseStudentEvaluationDTO createEvaluationAnswer(RequestEvaluationAnswerDTO dto, String userId) {
        // 1. 필수 값 검증
        if (dto.getTemplateGroupId() == null || dto.getTemplateGroupId().trim().isEmpty()) {
            throw new IllegalArgumentException("템플릿 그룹 ID는 필수입니다.");
        }
        if (dto.getCourseId() == null || dto.getCourseId().trim().isEmpty()) {
            throw new IllegalArgumentException("과정 ID는 필수입니다.");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (dto.getAnswerList() == null || dto.getAnswerList().isEmpty()) {
            throw new IllegalArgumentException("답변 리스트는 필수입니다.");
        }
        
        // 2. userId와 courseId로 학생 memberId 조회 (ROLE_STUDENT인 경우만)
        String memberId = "";
        MemberEntity currentStudentMember = null;
        
        // DTO에서 courseId 사용
        String courseId = dto.getCourseId();
        
        try {
            // userId와 courseId로 해당 학생의 memberId 조회
            currentStudentMember = memberRepository.findByUserIdAndCourseIdAndStudentRole(userId, courseId)
                .orElse(null);
            
            if (currentStudentMember != null) {
                memberId = currentStudentMember.getMemberId();
            } else {
                throw new IllegalArgumentException("해당 강의에서 현재 학생의 정보를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("학생 정보 조회 중 오류가 발생했습니다.");
        }
        
        // 3. 템플릿 그룹 존재 여부 확인
        TemplateGroupEntity templateGroupEntity = templateGroupRepository.findById(dto.getTemplateGroupId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 템플릿 그룹입니다."));
        
        // 4. 과정 정보 조회 (educationId 필요)
        CourseEntity course = courseRepository.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과정입니다."));
        
        // 5. 각 답변에 대해 중복 체크 및 저장
        List<EvaluationResponseEntity> savedResponses = new ArrayList<>();
        
        for (RequestEvaluationAnswerDTO.AnswerItem answerItem : dto.getAnswerList()) {
            // 5-1. 답변 항목 검증
            if (answerItem.getEvalQuestionId() == null || answerItem.getEvalQuestionId().trim().isEmpty()) {
                throw new IllegalArgumentException("평가 항목 ID는 필수입니다.");
            }
            
            // 5-2. 중복 답변 체크 (프론트엔드에서 막아주지만 백엔드에서도 한번 더 체크)
            List<EvaluationResponseEntity> existingResponses = studentEvaluationRepository
                .findByTemplateGroupIdAndEvalQuestionIdAndMemberId(
                    dto.getTemplateGroupId(), 
                    answerItem.getEvalQuestionId(),
                    memberId
                );
            
            if (!existingResponses.isEmpty()) {
                throw new IllegalArgumentException("이미 답변이 등록되어 있습니다. 답변은 한 번만 등록 가능합니다.");
            }
            
            // 5-3. 새로운 답변 저장
            EvaluationResponseEntity response = EvaluationResponseEntity.builder()
                .templateGroupId(dto.getTemplateGroupId())
                .memberId(memberId)
                .evalQuestionId(answerItem.getEvalQuestionId())
                .score(answerItem.getScore())
                .answerText(answerItem.getAnswerText())
                .educationId(course.getEducationId())
                .build();
            
            EvaluationResponseEntity savedResponse = studentEvaluationRepository.save(response);
            savedResponses.add(savedResponse);
        }
        
        // 6. 응답 DTO 생성
        ResponseStudentEvaluationDTO responseDTO = ResponseStudentEvaluationDTO.builder()
            .templateGroupId(dto.getTemplateGroupId())
            .memberId(memberId)
            .memberName(currentStudentMember.getMemberName())  // 이미 찾은 currentStudentMember 사용
            .courseId(courseId)
            .courseName(course.getCourseName())
            .openDate(templateGroupEntity.getOpenDate())
            .closeDate(templateGroupEntity.getCloseDate())
            .questionTemplateNum(templateGroupEntity.getQuestionTemplateNum())
            .answerCount(savedResponses.size())
            .build();
        
        return responseDTO;
    }


    // 학생의 과정 목록 조회(강의평가)
    public List<ResponseStudentCourseDTO> getMyCourseListByCourseIds(List<String> courseIds, String educationId, String userId) {
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // courseId 목록으로 강의 정보 조회
        List<Object[]> courseList = courseRepository.findByCourseIdsAndEducationId(courseIds, educationId, userId);
        
        // courseId별로 그룹화하여 중복 제거
        Map<String, List<Object[]>> groupedByCourseId = courseList.stream()
            .collect(Collectors.groupingBy(course -> ((CourseEntity) course[0]).getCourseId()));
        
        // 현재 로그인한 학생의 모든 courseId에 대한 memberId를 미리 조회
        Map<String, String> courseIdToMemberIdMap = new HashMap<>();
        try {
            for (String courseId : groupedByCourseId.keySet()) {
                MemberEntity currentStudentMember = memberRepository.findByUserIdAndCourseIdAndStudentRole(userId, courseId)
                    .orElse(null);
                
                if (currentStudentMember != null) {
                    courseIdToMemberIdMap.put(courseId, currentStudentMember.getMemberId());
                }
            }
        } catch (Exception e) {
            // 예외 처리
        }
        
        List<ResponseStudentCourseDTO> responseList = new ArrayList<ResponseStudentCourseDTO>();
        
        for (Map.Entry<String, List<Object[]>> entry : groupedByCourseId.entrySet()) {
            String courseId = entry.getKey();
            List<Object[]> courseDataList = entry.getValue();
            
            // 첫 번째 데이터를 기준으로 ResponseStudentCourseDTO 생성
            Object[] firstCourseData = courseDataList.get(0);
            
            // CourseEntity에서 가져온 데이터
            CourseEntity courseEntity = (CourseEntity) firstCourseData[0];
            
            // 해당 강의의 모든 템플릿 그룹 조회
            List<TemplateGroupEntity> templateGroups = templateGroupRepository.findByCourseIdSimple(courseId);
            List<ResponseStudentCourseDTO.TemplateItem> templateList = new ArrayList<>();
            
            // 각 템플릿 그룹을 TemplateItem으로 변환
            String studentMemberId = courseIdToMemberIdMap.get(courseId);
            
            for (TemplateGroupEntity templateGroup : templateGroups) {
                // 템플릿 이름 조회
                String questionTemplateName = "";
                try {
                    // questionTemplateNum으로 템플릿 이름 조회
                    List<Object[]> questionTemplates = templateGroupRepository.findByCourseId(courseId);
                    for (Object[] templateData : questionTemplates) {
                        TemplateGroupEntity detailTemplateGroup = (TemplateGroupEntity) templateData[0];
                        QuestionTemplateEntity questionTemplate = (QuestionTemplateEntity) templateData[1];
                        
                        if (detailTemplateGroup.getQuestionTemplateNum() == templateGroup.getQuestionTemplateNum()) {
                            questionTemplateName = questionTemplate.getQuestionTemplateName();
                            break;
                        }
                    }
                } catch (Exception e) {
                    // 예외 처리
                }
                
                // 해당 템플릿 그룹에 대한 답변 여부 확인
                boolean templateResponse = false;
                if (studentMemberId != null && !studentMemberId.isEmpty()) {
                    List<EvaluationResponseEntity> responses = studentEvaluationRepository
                        .findByTemplateGroupIdAndMemberId(templateGroup.getTemplateGroupId(), studentMemberId);
                    templateResponse = !responses.isEmpty();
                }
                
                ResponseStudentCourseDTO.TemplateItem templateItem = ResponseStudentCourseDTO.TemplateItem.builder()
                    .templateGroupId(templateGroup.getTemplateGroupId())
                    .openDate(templateGroup.getOpenDate())
                    .closeDate(templateGroup.getCloseDate())
                    .questionTemplateNum(templateGroup.getQuestionTemplateNum())
                    .questionTemplateName(questionTemplateName)
                    .response(templateResponse) // 각 템플릿별 답변 여부
                    .build();
                templateList.add(templateItem);
            }
            
            // 강의 레벨 답변 여부 확인 (하나라도 답변이 있으면 true)
            boolean hasResponse = templateList.stream()
                .anyMatch(template -> template.isResponse());
            
            int studentCount = memberRepository.countByCourseIdAndMemberRole(courseEntity.getCourseId(), "ROLE_STUDENT");
            
            ResponseStudentCourseDTO response = ResponseStudentCourseDTO.builder()
                .courseId(courseEntity.getCourseId())
                .courseCode(courseEntity.getCourseCode())
                .courseName(courseEntity.getCourseName())
                .memberId(firstCourseData[7] != null ? (String) firstCourseData[7] : "")
                .memberName(firstCourseData[1] != null ? (String) firstCourseData[1] : "")
                .educationId(courseEntity.getEducationId())
                .maxCapacity(courseEntity.getMaxCapacity())
                .studentCount(studentCount)
                .courseStartDay(courseEntity.getCourseStartDay())
                .courseEndDay(courseEntity.getCourseEndDay())
                .courseDays(courseEntity.getCourseDays())
                .startTime(courseEntity.getStartTime())
                .endTime(courseEntity.getEndTime())
                .response(hasResponse)
                .templateList(templateList)
                .build();
            
            responseList.add(response);
        }
        return responseList;
    }
    

    

    
    // 학생의 특정 템플릿 평가 상세 조회 (ResponseStudentEvaluationDetailDTO 반환)
    public ResponseStudentEvaluationDetailDTO getStudentEvaluationDetailByTemplateGroupId(String templateGroupId, String userId) {
        // 1. templateGroupId로 템플릿 그룹과 질문들을 가져옴
        List<Object[]> questionData = templateGroupRepository.findByTemplateGroupIdWithQuestions(templateGroupId);
        
        if (questionData.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 템플릿 그룹입니다.");
        }
        
        // 2. 첫 번째 데이터에서 템플릿 그룹 정보 추출
        Object[] firstData = questionData.get(0);
        TemplateGroupEntity templateGroup = (TemplateGroupEntity) firstData[0];
        QuestionTemplateEntity questionTemplate = (QuestionTemplateEntity) firstData[1];
        String courseId = templateGroup.getCourseId();
        
        // 3. 현재 로그인한 학생의 memberId 찾기
        String studentMemberId = "";
        
        try {
            MemberEntity currentStudentMember = memberRepository.findByUserIdAndCourseIdAndStudentRole(userId, courseId)
                .orElse(null);
            
            if (currentStudentMember != null) {
                studentMemberId = currentStudentMember.getMemberId();
            } else {
                throw new IllegalArgumentException("해당 강의에서 현재 학생의 정보를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("학생 정보 조회 중 오류가 발생했습니다.");
        }        
        // 4. 해당 템플릿 그룹의 모든 질문에 대한 학생의 답변 조회
        List<ResponseStudentEvaluationDetailDTO.EvaluationResponseItem> responseList = new ArrayList<>();
        
        for (Object[] data : questionData) {
            QuestionTemplateEntity qt = (QuestionTemplateEntity) data[1];
            EvaluationQuestionEntity eq = (EvaluationQuestionEntity) data[2];
            
            if (eq == null) {
                continue;
            }
            
            // 해당 질문에 대한 학생의 답변 조회
            List<EvaluationResponseEntity> responses = studentEvaluationRepository
                .findByTemplateGroupIdAndEvalQuestionIdAndMemberId(
                    templateGroupId,
                    eq.getEvalQuestionId(),
                    studentMemberId
                );
            
            if (!responses.isEmpty()) {
                EvaluationResponseEntity response = responses.get(0);
                
                ResponseStudentEvaluationDetailDTO.EvaluationResponseItem item = ResponseStudentEvaluationDetailDTO.EvaluationResponseItem.builder()
                    .evalQuestionId(eq.getEvalQuestionId())
                    .score(response.getScore())
                    .answerText(response.getAnswerText())
                    .questionText(eq.getEvalQuestionText())
                    .questionType(eq.getEvalQuestionType())
                    .build();
                
                responseList.add(item);
            }
        }
        
        // 5. ResponseStudentEvaluationDetailDTO 생성
        ResponseStudentEvaluationDetailDTO result = ResponseStudentEvaluationDetailDTO.builder()
            .templateGroupId(templateGroupId)
            .questionTemplateName(questionTemplate != null ? questionTemplate.getQuestionTemplateName() : "")
            .questionTemplateNum(questionTemplate != null ? questionTemplate.getQuestionTemplateNum() : 0)
            .data(responseList)
            .build();
        
        return result;
    }
}