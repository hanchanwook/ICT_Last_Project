package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.lnuyasha.repository.LectureHistoryRepository;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
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
public class InstructorAllCourseService {

    private final LectureHistoryRepository lectureHistoryRepository;
    private final MemberRepository memberRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<CourseDTO> getAllCoursesByInstructorId(String userId) {
        log.info("강사 과정 목록 조회 - userId: {}", userId);

        // userId로 memberId 조회
        String memberId = getMemberIdByUserId(userId);
        if (memberId == null) {
            log.warn("강사 정보를 찾을 수 없음 - userId: {}", userId);
            return List.of();
        }

        // 강사의 모든 과정 조회
        List<CourseEntity> courses = lectureHistoryRepository.findByInstructorId(memberId);
        log.info("강사의 과정 개수: {}", courses.size());

        return courses.stream()
                .map(this::convertToCourseDTO)
                .collect(Collectors.toList());
    }

    private String getMemberIdByUserId(String userId) {
        try {
            MemberEntity member = memberRepository.findById(userId).orElse(null);
            return member != null ? member.getMemberId() : null;
        } catch (Exception e) {
            log.error("userId로 memberId 조회 실패 - userId: {}", userId, e);
            return null;
        }
    }

    private CourseDTO convertToCourseDTO(CourseEntity course) {
        // 과정별 통계 데이터 조회
        Integer totalStudents = lectureHistoryRepository.countStudentsByCourseId(course.getCourseId());
        Integer completedStudents = lectureHistoryRepository.countCompletedStudentsByCourseId(course.getCourseId());
        Integer dropoutStudents = lectureHistoryRepository.countDropoutStudentsByCourseId(course.getCourseId());
        Double averageScore = lectureHistoryRepository.calculateAverageScoreByCourseId(course.getCourseId());
        Integer passRate = lectureHistoryRepository.calculatePassRateByCourseId(course.getCourseId());

        return CourseDTO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .courseStartDay(course.getCourseStartDay())
                .courseEndDay(course.getCourseEndDay())
                .courseStartDayStr(course.getCourseStartDay() != null ? course.getCourseStartDay().format(DATE_FORMATTER) : null)
                .courseEndDayStr(course.getCourseEndDay() != null ? course.getCourseEndDay().format(DATE_FORMATTER) : null)
                .status(determineLectureStatus(course))
                .totalStudents(totalStudents != null ? totalStudents : 0)
                .completedStudents(completedStudents != null ? completedStudents : 0)
                .dropoutStudents(dropoutStudents != null ? dropoutStudents : 0)
                .averageScore(averageScore != null ? averageScore : 0.0)
                .passRate(passRate != null ? passRate : 0)
                .build();
    }

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
            return "예정";
        }
    }
} 