package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.ExamDTO;
import com.jakdang.labs.api.lnuyasha.repository.LectureHistoryRepository;
import com.jakdang.labs.entity.TemplateEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseExamService {

    private final LectureHistoryRepository lectureHistoryRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 과정별 시험 목록 조회
     */
    public List<ExamDTO> getCourseExams(String courseId) {
        // 과정의 시험 목록 조회
        List<TemplateEntity> templates = lectureHistoryRepository.findTemplatesByCourseId(courseId);

        return templates.stream()
                .map(this::convertToExamDTO)
                .collect(Collectors.toList());
    }

    /**
     * TemplateEntity를 ExamDTO로 변환
     */
    private ExamDTO convertToExamDTO(TemplateEntity template) {
        // 시험별 통계 데이터 조회
        Double avgScore = lectureHistoryRepository.calculateAverageScoreByTemplateId(template.getTemplateId());
        Double passRate = lectureHistoryRepository.calculatePassRateByTemplateId(template.getTemplateId());
        Integer participantCount = lectureHistoryRepository.countParticipantsByTemplateId(template.getTemplateId());

        // 시험일 설정 (templateOpen을 사용하거나 현재 날짜로 설정)
        String examDate = template.getTemplateOpen() != null 
                ? template.getTemplateOpen().toLocalDate().format(DATE_FORMATTER)
                : LocalDate.now().format(DATE_FORMATTER);

        return ExamDTO.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .avgScore(avgScore != null ? avgScore : 0.0)
                .passRate(passRate != null ? passRate : 0.0)
                .examDate(examDate)
                .participantCount(participantCount != null ? participantCount : 0)
                .build();
    }
} 