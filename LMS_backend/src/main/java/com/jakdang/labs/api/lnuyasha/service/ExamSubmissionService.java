package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;
import com.jakdang.labs.api.lnuyasha.repository.AnswerRepository;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.repository.TemplateRepository;
import com.jakdang.labs.api.lnuyasha.repository.ScoreStudentRepository;
import com.jakdang.labs.entity.AnswerEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.TemplateEntity;
import com.jakdang.labs.entity.ScoreStudentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.jakdang.labs.api.lnuyasha.util.TimeZoneUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamSubmissionService {
    
    private final AnswerRepository answerRepository;
    private final KyMemberRepository memberRepository;
    private final TemplateRepository templateRepository;
    private final ScoreStudentRepository scoreStudentRepository;
    private final MemberService memberService;
    
    /**
     * 시험 제출 현황 조회
     * @param templateId 시험 템플릿 ID
     * @param userId 사용자 ID (선택사항)
     * @return 제출 현황 목록
     */
    public List<AnswerDTO> getExamSubmissions(String templateId, String userId) {
        
        try {
            // 1. 시험 템플릿 존재 확인
            TemplateEntity template = templateRepository.findByTemplateId(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다: " + templateId));
            
            // 2. userId가 있으면 해당 사용자의 권한 확인
            if (userId != null && !userId.trim().isEmpty()) {
                MemberInfoDTO memberInfo = memberService.getMemberInfo(userId);
                if (memberInfo == null) {
                    throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다: " + userId);
                }

            }
            
            // 3. 해당 시험의 모든 답안 조회
            List<AnswerEntity> answers = answerRepository.findByTemplateId(templateId);
            
            // 4. 과정에 등록된 모든 학생 조회 (memberExpired가 null인 학생만)
            // TODO: template.getSubGroupId() 대신 실제 courseId를 사용해야 함
            // 현재는 임시로 빈 리스트 사용
            List<MemberEntity> courseStudents = new ArrayList<>();
            

            
            // 5. 학생별 제출 현황 매핑 및 DTO 변환
            Map<String, List<AnswerEntity>> answersByMemberId = answers.stream()
                    .collect(Collectors.groupingBy(AnswerEntity::getMemberId));
            
            // 6. DTO 변환 (실제 학생 정보 조회)
            List<AnswerDTO> submissions = new ArrayList<>();
            
            for (Map.Entry<String, List<AnswerEntity>> entry : answersByMemberId.entrySet()) {
                String memberId = entry.getKey();
                List<AnswerEntity> studentAnswers = entry.getValue();
                
                // 학생 정보 조회
                List<MemberEntity> students = memberRepository.findByMemberId(memberId);
                String studentName = "알 수 없음";
                String memberEmail = "";
                
                if (!students.isEmpty()) {
                    MemberEntity student = students.get(0);
                    studentName = student.getMemberName() != null ? student.getMemberName() : "이름 없음";
                    memberEmail = student.getMemberEmail() != null ? student.getMemberEmail() : "";
                }
                
                // 학생의 총점 계산
                int totalScore = studentAnswers.stream()
                        .mapToInt(AnswerEntity::getAnswerScore)
                        .sum();
                
                // 첫 번째 답안의 시간 정보 사용
                AnswerEntity firstAnswer = studentAnswers.get(0);
                
                // ScoreStudentEntity에서 isChecked, graded 정보 조회
                ScoreStudentEntity scoreStudent = scoreStudentRepository.findByTemplateIdAndMemberId(templateId, memberId);
                Integer isChecked = null;
                Integer graded = null;
                String totalComment = null;
                
                if (scoreStudent != null) {
                    isChecked = scoreStudent.getIsChecked();
                    graded = scoreStudent.getGraded();
                    totalComment = scoreStudent.getTotalComment();
                }
                
                // status 결정 로직: scorestudent에 값이 있으면 "채점완료", 없으면 "채점대기"
                String status;
                if (scoreStudent != null) {
                    status = "채점완료";
                } else {
                    status = "채점대기";
                }
                
                AnswerDTO submission = AnswerDTO.builder()
                        .answerId(firstAnswer.getAnswerId())
                        .memberId(memberId)
                        .studentName(studentName)
                        .memberEmail(memberEmail)
                        .answerScore(totalScore)
                        .grade(calculateGrade(totalScore, getTotalScore(templateId)))
                        .submittedAt(TimeZoneUtil.toKoreanTime(firstAnswer.getCreatedAt()))
                        .gradedAt(firstAnswer.getAnswerGradedAt())
                        .status(status)
                        .isChecked(isChecked)
                        .graded(graded)
                        .totalComment(totalComment)
                        .build();
                
                submissions.add(submission);
            }
            
            return submissions;
            
        } catch (Exception e) {
            log.error("시험 제출 현황 조회 중 오류 발생: templateId = {}, error = {}", templateId, e.getMessage(), e);
            throw new RuntimeException("시험 제출 현황 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 등급 계산
     */
    private String calculateGrade(Integer score, Integer totalScore) {
        if (score == null || totalScore == null || totalScore == 0) {
            return null;
        }
        
        double percentage = (double) score / totalScore * 100;
        
        if (percentage >= 90) return "탁월";
        else if (percentage >= 80) return "우수";
        else if (percentage >= 70) return "양호";
        else if (percentage >= 60) return "보통";
        else return "노력";
    }
    
    /**
     * 시험 제출 시 학생 총점 계산 및 저장
     * @param templateId 시험 템플릿 ID
     * @param memberId 학생 ID
     */
    @Transactional
    public void calculateAndSaveStudentScore(String templateId, String memberId) {
        
        try {
            // 1. 해당 학생의 모든 답안 조회
            List<AnswerEntity> studentAnswers = answerRepository.findByTemplateIdAndMemberId(templateId, memberId);
            
            if (studentAnswers.isEmpty()) {
                log.warn("학생 답안이 없습니다: templateId={}, memberId={}", templateId, memberId);
                return;
            }
            
            // 2. 학생의 총점 계산 (답안 점수 합계)
            int totalScore = studentAnswers.stream()
                    .mapToInt(AnswerEntity::getAnswerScore)
                    .sum();
            

            
            // 3. 기존 scorestudent 레코드 확인
            ScoreStudentEntity existingScore = scoreStudentRepository.findByTemplateIdAndMemberId(templateId, memberId);
            
            if (existingScore != null) {
                // 기존 레코드 업데이트
                existingScore.setScore(totalScore);
                existingScore.setIsChecked(1); // 채점 완료, 학생 확인 가능
                existingScore.setGraded(0); // 채점 완료 (0: 채점 완료, 1: 미채점)
                scoreStudentRepository.save(existingScore);
            } else {
                // 새 레코드 생성
                ScoreStudentEntity newScore = ScoreStudentEntity.builder()
                        .score(totalScore)
                        .isChecked(1) // 채점 완료, 학생 확인 가능
                        .memberId(memberId)
                        .templateId(templateId)
                        .graded(0) // 채점 완료 (0: 채점 완료, 1: 미채점)
                        .build();
                
                ScoreStudentEntity savedScore = scoreStudentRepository.save(newScore);
            }
            
        } catch (Exception e) {
            log.error("학생 총점 계산 및 저장 중 오류 발생: templateId={}, memberId={}, error={}", 
                    templateId, memberId, e.getMessage(), e);
            throw new RuntimeException("학생 총점 계산 및 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 시험 점수 조회 (강사용)
     * @param templateId 시험 템플릿 ID
     * @return 학생별 점수 목록
     */
    public List<AnswerDTO> getExamScores(String templateId) {
        
        try {
            // 1. 시험 템플릿 존재 확인
            TemplateEntity template = templateRepository.findByTemplateId(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다: " + templateId));
            
            // 2. 해당 시험의 모든 scorestudent 레코드 조회
            List<ScoreStudentEntity> scoreStudents = scoreStudentRepository.findByTemplateId(templateId);
            
            // 3. DTO 변환
            return scoreStudents.stream()
                    .map(scoreStudent -> AnswerDTO.builder()
                            .memberId(scoreStudent.getMemberId())
                            .score(scoreStudent.getScore())
                            .isChecked(scoreStudent.getIsChecked())
                            .totalComment(scoreStudent.getTotalComment())
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("시험 점수 조회 중 오류 발생: templateId = {}, error = {}", templateId, e.getMessage(), e);
            throw new RuntimeException("시험 점수 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 시험 총점 계산 (실제 시험 문제들의 총점)
     */
    private int getTotalScore(String templateId) {
        // TODO: 실제 시험 총점 계산 로직 구현
        // TemplateQuestionEntity에서 각 문제의 점수를 합산하여 총점 계산
        // 현재는 임시로 100점으로 설정
        return 100;
    }
} 