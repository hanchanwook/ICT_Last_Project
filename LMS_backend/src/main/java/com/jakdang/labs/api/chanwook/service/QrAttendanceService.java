package com.jakdang.labs.api.chanwook.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.jakdang.labs.api.chanwook.DTO.AttendanceDTO;
import com.jakdang.labs.entity.AttendanceEntity;
import com.jakdang.labs.api.chanwook.repository.AttendanceRepository;
import com.jakdang.labs.api.chanwook.repository.QrCodeRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QrAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final QrCodeRepository qrCodeRepository;

    // QR 출석체크 - 완전한 검증 로직 포함 (여러 과정 수강 지원)
    public AttendanceDTO checkIn(String userId, String classroomId, String courseId) {
        log.info("QR 출석체크: userId={}, classroomId={}, courseId={}", userId, classroomId, courseId);
        
        // 1. userId로 학생의 모든 수강 과정 조회
        List<Object[]> studentCourses = qrCodeRepository.findStudentCoursesById(userId);
        if (studentCourses.isEmpty()) {
            throw new RuntimeException("해당 사용자를 찾을 수 없거나 학생이 아닙니다: " + userId);
        }
        
        // 2. 학생 계정인지 확인 (첫 번째 레코드로 확인)
        String memberRole = (String) studentCourses.get(0)[0];
        if (!"ROLE_STUDENT".equals(memberRole)) {
            throw new RuntimeException("학생 계정만 QR 출석체크를 사용할 수 있습니다.");
        }
        
        log.info("학생의 수강 과정 수: {}", studentCourses.size());
        
        // 3. QR 코드의 courseId가 학생의 수강 과정 중 하나인지 확인
        boolean courseMatch = false;
        String matchedMemberId = null;
        String matchedClassId = null;
        
        for (Object[] courseData : studentCourses) {
            String studentCourseId = (String) courseData[2]; // courseId
            String studentClassId = (String) courseData[3]; // classId
            
            log.info("학생 수강 과정 확인: courseId={}, classId={}", studentCourseId, studentClassId);
            
            if (courseId.equals(studentCourseId)) {
                courseMatch = true;
                matchedMemberId = (String) courseData[1]; // memberId
                matchedClassId = studentClassId;
                log.info("과정 일치 발견: courseId={}, memberId={}, classId={}", 
                        courseId, matchedMemberId, matchedClassId);
                break;
            }
        }
        
        if (!courseMatch) {
            log.error("QR courseId({})가 학생의 수강 과정에 없음", courseId);
            throw new RuntimeException("QR 코드의 과정 정보와 학생의 수강 과정이 일치하지 않습니다.");
        }
        
        // 4. QR 코드의 classroomId와 매칭된 과정의 classId 일치 여부 확인
        if (!classroomId.equals(matchedClassId)) {
            log.error("QR classroomId({})와 학생 classId({}) 불일치", classroomId, matchedClassId);
            throw new RuntimeException("QR 코드의 강의실 정보와 학생의 수강 강의실이 일치하지 않습니다.");
        }
        
        log.info("QR 코드 검증 완료: courseId={}, classroomId={}", courseId, classroomId);
        
        // 5. 오늘 날짜의 해당 과정 출석 기록이 있는지 확인
        LocalDateTime today = LocalDateTime.now();
        AttendanceEntity existingAttendance = attendanceRepository.findByUserIdAndLectureDateAndCourseId(
            userId, today.toLocalDate(), courseId);
        
        if (existingAttendance != null) {
            // 출석 기록이 있는 경우 checkOut 여부 확인
            if (existingAttendance.getCheckOut() != null) {
                // 이미 퇴실까지 완료한 경우
                log.error("이미 출석 및 퇴실이 완료되었습니다: attendanceId={}, userId={}, courseId={}, checkOut={}", 
                        existingAttendance.getAttendanceId(), userId, courseId, existingAttendance.getCheckOut());
                throw new RuntimeException("이미 출석 및 퇴실이 완료되었습니다.");
            } else {
                // 출석했지만 퇴실하지 않은 경우
                log.error("이미 출석 기록이 존재합니다. 퇴실을 진행하세요: attendanceId={}, userId={}, courseId={}", 
                        existingAttendance.getAttendanceId(), userId, courseId);
                throw new RuntimeException("이미 해당 과정에 출석 기록이 있습니다. 퇴실을 진행하세요.");
            }
        } else {
            // 6. 새로운 출석 기록 생성
            AttendanceEntity newAttendance = AttendanceEntity.builder()
                    .userId(userId)
                    .memberId(matchedMemberId)
                    .courseId(courseId)
                    .lectureDate(today.toLocalDate())
                    .attendanceStatus("출석")
                    .checkIn(LocalDateTime.now())
                    .classId(classroomId)
                    .note("QR코드 출석체크")
                    .build();
            
            AttendanceEntity saved = attendanceRepository.save(newAttendance);
            log.info("QR 출석체크 완료: attendanceId={}, userId={}, courseId={}", 
                    saved.getAttendanceId(), userId, courseId);
            return toDTO(saved);
        }
    }

    // QR 퇴실체크 (과정별)
    public AttendanceDTO checkOut(String userId, String courseId) {
        log.info("QR 퇴실체크: userId={}, courseId={}", userId, courseId);
        
        // userId로 학생의 수강 과정 조회 (출석체크와 동일한 방식)
        List<Object[]> studentCourses = qrCodeRepository.findStudentCoursesById(userId);
        if (studentCourses.isEmpty()) {
            throw new RuntimeException("해당 사용자를 찾을 수 없거나 학생이 아닙니다: " + userId);
        }
        
        // 학생 계정인지 확인
        String memberRole = (String) studentCourses.get(0)[0];
        if (!"ROLE_STUDENT".equals(memberRole)) {
            throw new RuntimeException("학생 계정만 QR 퇴실체크를 사용할 수 있습니다.");
        }
        
        // courseId가 학생의 수강 과정 중 하나인지 확인
        String memberId = null;
        log.info("학생의 수강 과정 확인 시작: courseId={}", courseId);
        for (Object[] courseData : studentCourses) {
            String studentCourseId = (String) courseData[2]; // courseId
            String studentMemberId = (String) courseData[1]; // memberId
            log.info("학생 수강 과정: courseId={}, memberId={}", studentCourseId, studentMemberId);
            if (courseId.equals(studentCourseId)) {
                memberId = studentMemberId;
                log.info("과정 일치 발견: courseId={}, memberId={}", courseId, memberId);
                break;
            }
        }
        
        if (memberId == null) {
            log.error("QR 코드의 과정 정보와 학생의 수강 과정이 일치하지 않습니다. courseId={}", courseId);
            throw new RuntimeException("QR 코드의 과정 정보와 학생의 수강 과정이 일치하지 않습니다.");
        }
        
        log.info("memberId 확인 완료: memberId={}", memberId);
        
        // 오늘 날짜의 해당 과정 출석 기록 찾기 (AttendanceService와 동일한 방식)
        LocalDateTime today = LocalDateTime.now();
        AttendanceEntity attendance = attendanceRepository.findByUserIdAndLectureDateAndCourseId(
            userId, today.toLocalDate(), courseId);
        
        if (attendance == null) {
            throw new RuntimeException("오늘의 해당 과정 출석 기록을 찾을 수 없습니다: " + userId);
        }
        
        // 이미 퇴실한 경우 체크 (AttendanceService와 동일한 구조)
        if (attendance.getCheckOut() != null) {
            throw new RuntimeException("이미 퇴실 처리되었습니다.");
        }
        
        attendance.setCheckOut(LocalDateTime.now());    
        attendance.setNote(attendance.getNote() + " | 퇴실: " + LocalDateTime.now());
        
        AttendanceEntity saved = attendanceRepository.save(attendance);
        return toDTO(saved);
    }

    // Entity를 DTO로 변환
    private AttendanceDTO toDTO(AttendanceEntity entity) {
        return AttendanceDTO.builder()
                .attendanceId(entity.getAttendanceId())
                .userId(entity.getUserId())
                .courseId(entity.getCourseId())  // courseId 추가
                .lectureDate(entity.getLectureDate())
                .lectureStartTime(entity.getLectureStartTime())
                .lectureEndTime(entity.getLectureEndTime())
                .attendanceStatus(entity.getAttendanceStatus())
                .checkIn(entity.getCheckIn())
                .checkOut(entity.getCheckOut())
                .officialReason(entity.getOfficialReason()) 
                .note(entity.getNote())
                .build();
    }

    // 오늘 출석 상태 조회 (과정별) - AttendanceService와 동일한 구조
    public Map<String, Object> getTodayAttendanceStatus(String userId, String courseId) {
        log.info("오늘 출석 상태 조회: userId={}, courseId={}", userId, courseId);
        
        LocalDate today = LocalDate.now();
        log.info("현재 날짜: {}", today);
        
        // AttendanceService와 동일한 방식으로 조회
        AttendanceEntity todayAttendance = attendanceRepository.findByUserIdAndLectureDateAndCourseId(
            userId, today, courseId);
        
        log.info("출석 기록 조회 결과: todayAttendance={}", todayAttendance != null ? todayAttendance.getAttendanceId() : "null");
        
        // 로그인한 사용자와 DB 출석 기록의 userId/memberId 비교
        if (todayAttendance != null) {
            log.info("=== 사용자 정보 비교 ===");
            log.info("로그인한 userId: {}", userId);
            log.info("DB 출석 기록 userId: {}", todayAttendance.getUserId());
            log.info("DB 출석 기록 memberId: {}", todayAttendance.getMemberId());
            log.info("DB 출석 기록 courseId: {}", todayAttendance.getCourseId());
            log.info("요청된 courseId: {}", courseId);
            log.info("=========================");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("lectureDate", today);
        
        if (todayAttendance == null) {
            // 오늘 출석 기록이 없는 경우
            log.info("오늘 출석 기록이 없습니다: userId={}, courseId={}, date={}", userId, courseId, today);
            response.put("hasAttendance", false);
            response.put("attendanceStatus", "없음");
            response.put("action", "CHECK_IN");
            response.put("message", "오늘 출석 기록이 없습니다. 출석체크를 진행하세요.");
        } else {
            log.info("오늘 출석 기록이 있습니다: attendanceId={}, checkIn={}, checkOut={}", 
                    todayAttendance.getAttendanceId(), todayAttendance.getCheckIn(), todayAttendance.getCheckOut());
            // 오늘 출석 기록이 있는 경우
            response.put("hasAttendance", true);
            response.put("attendanceId", todayAttendance.getAttendanceId());
            response.put("checkInTime", todayAttendance.getCheckIn());
            response.put("checkOutTime", todayAttendance.getCheckOut());
            response.put("attendanceStatus", todayAttendance.getAttendanceStatus());
            
            if (todayAttendance.getCheckOut() != null) {
                // 이미 퇴실한 경우
                response.put("action", "ALREADY_CHECKED_OUT");
                response.put("message", "이미 퇴실 처리되었습니다.");
            } else {
                // 출석했지만 퇴실하지 않은 경우
                response.put("action", "CHECK_OUT");
                response.put("message", "출석 기록이 있습니다. 퇴실 화면으로 이동합니다.");
            }
        }
        
        log.info("오늘 출석 상태 조회 완료: userId={}, action={}", userId, response.get("action"));
        return response;
    }


} 