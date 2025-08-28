package com.jakdang.labs.api.chanwook.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.jakdang.labs.api.chanwook.DTO.AttendanceDTO;

import com.jakdang.labs.entity.AttendanceEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.chanwook.repository.AttendanceRepository;
import com.jakdang.labs.api.chanwook.repository.MemberManagementRepository;
import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.api.chanwook.repository.ClassroomRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.CourseRepository;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MemberManagementRepository memberManagementRepository;
    private final InstructorMemberRepository instructorMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final CourseRepository courseRepository;

    // 학생별 출석 조회
    public List<AttendanceDTO> getAttendancesByUserId(String userId) {
        log.info("학생별 출석 조회: {}", userId);
        List<AttendanceEntity> entities = attendanceRepository.findByUserId(userId);
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

    // 학생별 출석 조회 (프론트엔드 필터링용)
    public List<Map<String, Object>> getAttendancesByUserId(String userId, Map<String, String> params) {
        log.info("학생별 출석 조회: userId={}, params={}", userId, params);
        
        // 조인 쿼리로 상세 정보 조회
        List<Object[]> results = attendanceRepository.findByUserIdWithMemberAndCourseInfo(userId);
        
        // Object[]를 DTO로 변환
        List<AttendanceDTO> attendances = results.stream()
                .map(this::toDTOWithJoin)
                .collect(Collectors.toList());
        
        // 프론트엔드 호환성을 위한 Map 형태로 변환 (데이터 정제 포함)
        List<Map<String, Object>> frontendAttendances = attendances.stream()
                .map(this::convertToFrontendFormat)
                .collect(Collectors.toList());
        
        log.info("출석 조회 결과: {} 건", frontendAttendances.size());
        return frontendAttendances;
    }

    // Entity를 DTO로 변환
    private AttendanceDTO toDTO(AttendanceEntity entity) {
        return AttendanceDTO.builder()
                .attendanceId(entity.getAttendanceId())
                .userId(entity.getUserId())
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

    // 조인 쿼리 결과를 DTO로 변환
    private AttendanceDTO toDTOWithJoin(Object[] result) {
        if (result == null || result.length < 5) {
            throw new RuntimeException("출석 정보를 찾을 수 없습니다.");
        }
        
        AttendanceEntity entity = (AttendanceEntity) result[0];
        String memberEmail = (String) result[1];
        String memberName = (String) result[2];
        String courseName = (String) result[3];
        String classCode = (String) result[4];
        
        return AttendanceDTO.builder()
                .attendanceId(entity.getAttendanceId())
                .userId(entity.getUserId())
                .lectureDate(entity.getLectureDate())
                .lectureStartTime(entity.getLectureStartTime())
                .lectureEndTime(entity.getLectureEndTime())
                .attendanceStatus(entity.getAttendanceStatus())
                .checkIn(entity.getCheckIn())
                .checkOut(entity.getCheckOut())
                .officialReason(entity.getOfficialReason())
                .note(entity.getNote())
                .memberEmail(memberEmail)
                .memberName(memberName)
                .courseName(courseName != null ? courseName : "과정명 없음")
                .classCode(classCode != null ? classCode : "강의실 코드 없음")
                .build();
    }
    
    // 프론트엔드 호환성을 위한 데이터 변환
    private Map<String, Object> convertToFrontendFormat(AttendanceDTO attendance) {
        Map<String, Object> frontendData = new HashMap<>();
        frontendData.put("attendanceId", attendance.getAttendanceId() != null ? attendance.getAttendanceId() : "");
        frontendData.put("userId", attendance.getUserId() != null ? attendance.getUserId() : "");
        frontendData.put("lectureDate", attendance.getLectureDate() != null ? attendance.getLectureDate() : "");
        frontendData.put("lectureStartTime", attendance.getLectureStartTime() != null ? attendance.getLectureStartTime() : "");
        frontendData.put("lectureEndTime", attendance.getLectureEndTime() != null ? attendance.getLectureEndTime() : "");
        frontendData.put("attendanceStatus", attendance.getAttendanceStatus() != null ? attendance.getAttendanceStatus() : "");
        frontendData.put("checkIn", attendance.getCheckIn() != null ? attendance.getCheckIn() : "");
        frontendData.put("checkOut", attendance.getCheckOut() != null ? attendance.getCheckOut() : "");
        frontendData.put("officialReason", attendance.getOfficialReason() != null ? attendance.getOfficialReason() : "");
        frontendData.put("note", attendance.getNote() != null ? attendance.getNote() : "");
        frontendData.put("memberEmail", attendance.getMemberEmail() != null ? attendance.getMemberEmail() : "");
        frontendData.put("memberName", attendance.getMemberName() != null ? attendance.getMemberName() : "");
        frontendData.put("courseName", attendance.getCourseName() != null ? attendance.getCourseName() : "");
        
        // 프론트엔드 호환성을 위한 필드 매핑 (null 값 처리 포함)
        frontendData.put("classId", attendance.getClassCode() != null ? attendance.getClassCode() : "");
        frontendData.put("classroomName", attendance.getClassCode() != null ? attendance.getClassCode() : "");
        frontendData.put("instructorName", attendance.getMemberName() != null ? attendance.getMemberName() : "");
        
        return frontendData;
    }
    
    // PC용 출석 제출
    public Map<String, Object> submitPcAttendance(Map<String, Object> request, Map<String, Object> userInfo) {
        log.info("=== PC용 출석 제출 처리 시작 ===");
        log.info("요청 데이터: {}", request);
        log.info("세션 사용자 정보: {}", userInfo);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 요청 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> deviceInfo = (Map<String, Object>) request.get("deviceInfo");
            
            String userId = (String) userInfo.get("userId");
            String courseId = (String) request.get("courseId");
            String classId = (String) request.get("classroomId");
            String checkInTimeStr = (String) request.get("checkInTime");
            String platform = deviceInfo != null ? (String) deviceInfo.get("platform") : "Unknown";
            
            log.info("=== 데이터 추출 결과 ===");
            log.info("userId: {}", userId);
            log.info("courseId: {}", courseId);
            log.info("classId: {}", classId);
            log.info("checkInTimeStr: {}", checkInTimeStr);
            log.info("platform: {}", platform);
            
            // userId로 MemberEntity 조회하여 memberId 찾기
            log.info("=== MemberEntity 조회 시작 ===");
            List<MemberEntity> members = instructorMemberRepository.findByIdField(userId);
            
            if (members.isEmpty()) {
                throw new RuntimeException("사용자 정보를 찾을 수 없습니다: " + userId);
            }
            
            // courseId와 일치하는 memberId 찾기
            MemberEntity targetMember = null;
            for (MemberEntity member : members) {
                if (courseId.equals(member.getCourseId())) {
                    targetMember = member;
                    break;
                }
            }
            
            if (targetMember == null) {
                log.error("사용자 {}가 과정 {}에 등록되지 않았습니다.", userId, courseId);
                throw new RuntimeException("등록되지 않은 과정입니다. courseId: " + courseId);
            }
            
            log.info("MemberEntity 조회 성공: memberId={}, courseId={}", targetMember.getMemberId(), targetMember.getCourseId());
            
            String memberId = targetMember.getMemberId();
            log.info("memberId 확인: {}", memberId);
            
            // 사용자의 최근 출석 기록에서 실제 classId 조회
            log.info("=== 실제 classId 조회 시작 ===");
            String actualClassId = attendanceRepository.findLatestClassIdByUserId(userId);
            
            if (actualClassId != null && !actualClassId.isEmpty()) {
                log.info("최근 출석 기록에서 실제 classId 조회: {}", actualClassId);
            } else {
                actualClassId = classId; // 기본값은 프론트에서 보내준 값
                log.info("출석 기록이 없어 프론트 값 사용: {}", actualClassId);
            }
            
            // 현재 시간 파싱
            LocalDateTime checkInTime = parseDateTime(checkInTimeStr);
            LocalDateTime currentTime = LocalDateTime.now();
            
            log.info("=== 시간 파싱 결과 ===");
            log.info("checkInTime: {}", checkInTime);
            log.info("currentTime: {}", currentTime);
            log.info("lectureDate: {}", checkInTime.toLocalDate());
              
            // 출석 상태 결정 (시간 기반)
            String attendanceStatus = determineAttendanceStatus(checkInTime);
            log.info("출석 상태: {}", attendanceStatus);
            
            // 오늘 날짜의 해당 과정 출석 기록이 있는지 확인
            log.info("기존 출석 기록 조회 시작: userId={}, lectureDate={}, courseId={}", 
                    userId, checkInTime.toLocalDate(), courseId);
            AttendanceEntity existingAttendance = attendanceRepository.findByUserIdAndLectureDateAndCourseId(
                userId, checkInTime.toLocalDate(), courseId);
            log.info("기존 출석 기록 조회 결과: {}", existingAttendance != null ? "존재함" : "없음");
            
            AttendanceEntity savedAttendance;
            
            if (existingAttendance != null) {
                // 이미 해당 과정에 출석 기록이 있으면 퇴실로 안내
                log.info("해당 과정에 이미 출석 기록이 존재합니다. 퇴실로 안내합니다.");
                result.put("success", true);
                result.put("message", "이미 해당 과정에 출석 기록이 있습니다. 퇴실을 진행하세요.");
                result.put("attendanceId", existingAttendance.getAttendanceId());
                result.put("attendanceStatus", existingAttendance.getAttendanceStatus());
                result.put("checkInTime", existingAttendance.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                result.put("action", "CHECK_OUT");
                return result;
            }
            
            // 새로운 출석 기록 생성
            log.info("새로운 출석 기록 생성 시작");
            AttendanceEntity attendance = AttendanceEntity.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .classId(classId)
                    .memberId(memberId)
                    .lectureDate(checkInTime.toLocalDate())
                    .lectureStartTime(checkInTime)
                    .attendanceStatus(attendanceStatus)
                    .checkIn(checkInTime)
                    .checkOut(null) // 퇴실 시간은 나중에 업데이트
                    .note("PC 출석체크")
                    .build();
            
            log.info("새로운 출석 기록 생성 완료, 저장 시작");
            savedAttendance = attendanceRepository.save(attendance);
            log.info("새로운 출석 기록 저장 완료: attendanceId={}", savedAttendance.getAttendanceId());
            
            log.info("PC용 출석 제출 성공: 출석 ID {}, 상태 {}", 
                    savedAttendance.getAttendanceId(), attendanceStatus);
            
            result.put("success", true);
            result.put("message", "출석이 성공적으로 처리되었습니다.");
            result.put("attendanceId", savedAttendance.getAttendanceId());
            result.put("attendanceStatus", attendanceStatus);
            result.put("checkInTime", checkInTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("PC용 출석 제출 실패: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("message", "출석 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    // 출석 상태 결정 (시간 기반)
    private String determineAttendanceStatus(LocalDateTime checkInTime) {
        // 지각 기준은 별도 설정으로 변경 예정
        return "출석";
    }
    
    // PC용 퇴실 제출
    public Map<String, Object> submitPcCheckOut(Map<String, Object> request, Map<String, Object> userInfo) {
        log.info("=== PC용 퇴실 제출 처리 시작 ===");
        log.info("요청 데이터: {}", request);
        log.info("세션 사용자 정보: {}", userInfo);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 요청 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> deviceInfo = (Map<String, Object>) request.get("deviceInfo");
            
            String userId = (String) userInfo.get("userId");
            String checkOutTimeStr = (String) request.get("checkOutTime");
            String platform = deviceInfo != null ? (String) deviceInfo.get("platform") : "Unknown";
            
            // 현재 시간 파싱
            LocalDateTime checkOutTime = parseDateTime(checkOutTimeStr);
            
            // 오늘 날짜의 해당 과정 출석 기록 찾기
            String courseId = (String) request.get("courseId");
            if (courseId == null) {
                result.put("success", false);
                result.put("message", "과정 ID가 필요합니다.");
                return result;
            }
            
            // 1. userId로 memberId 찾기
            List<MemberEntity> members = instructorMemberRepository.findByIdField(userId);
            if (members.isEmpty()) {
                result.put("success", false);
                result.put("message", "사용자 정보를 찾을 수 없습니다.");
                return result;
            }
            
            // 2. courseId와 일치하는 memberId 찾기
            MemberEntity targetMember = null;
            for (MemberEntity member : members) {
                if (courseId.equals(member.getCourseId())) {
                    targetMember = member;
                    break;
                }
            }
            
            if (targetMember == null) {
                result.put("success", false);
                result.put("message", "사용자가 해당 과정에 등록되지 않았습니다.");
                return result;
            }
            
            // 3. 해당 memberId와 courseId로 출석 기록 조회
            List<AttendanceEntity> attendances = attendanceRepository.findByMemberIdAndCourseIdAndLectureDate(
                targetMember.getMemberId(), courseId, checkOutTime.toLocalDate());
            
            if (attendances.isEmpty()) {
                result.put("success", false);
                result.put("message", "오늘의 해당 과정 출석 기록을 찾을 수 없습니다. 먼저 출석체크를 진행해주세요.");
                return result;
            }
            
            AttendanceEntity attendance = attendances.get(0); // 첫 번째 출석 기록 사용
            
            if (attendance.getCheckOut() != null) {
                result.put("success", false);
                result.put("message", "이미 퇴실 처리되었습니다.");
                return result;
            }
            
            // 퇴실 처리
            attendance.setCheckOut(checkOutTime);
            attendance.setNote(attendance.getNote() + " | PC 퇴실: " + checkOutTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " (" + platform + ")");
            
            AttendanceEntity savedAttendance = attendanceRepository.save(attendance);
            
            log.info("PC용 퇴실 제출 성공: 출석 ID {}, 퇴실 시간 {}", 
                    savedAttendance.getAttendanceId(), checkOutTime);
            
            result.put("success", true);
            result.put("message", "퇴실이 성공적으로 처리되었습니다.");
            result.put("attendanceId", savedAttendance.getAttendanceId());
            result.put("checkOutTime", checkOutTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            log.error("PC용 퇴실 제출 실패: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("message", "퇴실 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    // PC용 오늘 출석 상태 조회 (과정별)
    public Map<String, Object> getPcTodayAttendanceStatus(String userId, String courseId) {
        log.info("PC용 오늘 출석 상태 조회: userId={}, courseId={}", userId, courseId);
        
        LocalDate today = LocalDate.now();
        
        // 1. userId로 memberId 찾기
        List<MemberEntity> members = instructorMemberRepository.findByIdField(userId);
        if (members.isEmpty()) {
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다: " + userId);
        }
        
        // 2. courseId와 일치하는 memberId 찾기
        MemberEntity targetMember = null;
        for (MemberEntity member : members) {
            if (courseId.equals(member.getCourseId())) {
                targetMember = member;
                break;
            }
        }
        
        if (targetMember == null) {
            throw new RuntimeException("사용자가 해당 과정에 등록되지 않았습니다: " + courseId);
        }
        
        // 3. 해당 memberId와 courseId로 출석 기록 조회
        List<AttendanceEntity> todayAttendances = attendanceRepository.findByMemberIdAndCourseIdAndLectureDate(
            targetMember.getMemberId(), courseId, today);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("lectureDate", today);
        
        if (todayAttendances.isEmpty()) {
            // 오늘 출석 기록이 없는 경우
            response.put("hasAttendance", false);
            response.put("attendanceStatus", "없음");
            response.put("action", "CHECK_IN");
            response.put("message", "오늘 출석 기록이 없습니다. 출석체크를 진행하세요.");
        } else {
            // 오늘 출석 기록이 있는 경우 (여러 과정 가능)
            response.put("hasAttendance", true);
            response.put("attendanceCount", todayAttendances.size());
            
            // 첫 번째 출석 기록 정보 (기존 호환성 유지)
            AttendanceEntity firstAttendance = todayAttendances.get(0);
            response.put("attendanceId", firstAttendance.getAttendanceId());
            response.put("checkInTime", firstAttendance.getCheckIn());
            response.put("checkOutTime", firstAttendance.getCheckOut());
            response.put("attendanceStatus", firstAttendance.getAttendanceStatus());
            
            if (firstAttendance.getCheckOut() != null) {
                // 이미 퇴실한 경우
                response.put("action", "ALREADY_CHECKED_OUT");
                response.put("message", "이미 퇴실 처리되었습니다.");
            } else {
                // 출석했지만 퇴실하지 않은 경우
                response.put("action", "CHECK_OUT");
                response.put("message", "출석 기록이 있습니다. 퇴실 화면으로 이동합니다.");
            }
        }
        
        log.info("PC용 오늘 출석 상태 조회 완료: userId={}, action={}", userId, response.get("action"));
        return response;
    }
    
    // 날짜/시간 문자열 파싱
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // ISO 8601 형식 파싱 (예: "2024-01-15T10:30:00.000Z")
            return LocalDateTime.parse(dateTimeStr.replace("Z", ""), 
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패, 현재 시간 사용: {}", dateTimeStr);
            return LocalDateTime.now();
        }
    }

    // 학생 수강 과목 및 강의실 정보 조회 (PC 출석용)
    public Map<String, Object> getStudentCoursesAndClassrooms(String studentId) {
        log.info("=== 학생 수강 과목 및 강의실 정보 조회 시작 ===");
        log.info("studentId: {}", studentId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. MemberEntity에서 학생 정보 조회 (여러 결과 처리)
            log.info("MemberEntity 조회 시작");
            List<MemberEntity> members = instructorMemberRepository.findByIdField(studentId);
            
            if (members.isEmpty()) {
                log.error("학생 정보를 찾을 수 없습니다: {}", studentId);
                throw new RuntimeException("학생 정보를 찾을 수 없습니다: " + studentId);
            }
            
            log.info("조회된 MemberEntity 개수: {}", members.size());
            
            // 2. 모든 과정 정보를 수집
            List<Map<String, Object>> courses = new ArrayList<>();
            
            for (MemberEntity member : members) {
                if (!"ROLE_STUDENT".equals(member.getMemberRole())) {
                    continue; // 학생이 아닌 경우 스킵
                }
                
                log.info("MemberEntity 처리: memberId={}, courseId={}, role={}", 
                        member.getMemberId(), member.getCourseId(), member.getMemberRole());
                
                String courseId = member.getCourseId();
                if (courseId == null || courseId.trim().isEmpty()) {
                    log.warn("courseId가 null이거나 비어있습니다: memberId={}", member.getMemberId());
                    continue;
                }
                
                // courseId를 기반으로 해당 과정의 강의실 정보 조회
                String classroomId = null;
                String classroomName = null;
                String courseName = null;
                
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("courseId", courseId);
                
                try {
                    // CourseEntity에서 courseId로 과정 정보 조회
                    var courseOpt = courseRepository.findById(courseId);
                    if (courseOpt.isPresent()) {
                        var course = courseOpt.get();
                        String classId = course.getClassId();
                        courseName = course.getCourseName(); // 과정명 조회
                        
                        if (classId != null) {
                            // ClassroomEntity에서 강의실 정보 조회
                            var classroomOpt = classroomRepository.findById(classId);
                            if (classroomOpt.isPresent()) {
                                var classroom = classroomOpt.get();
                                classroomId = classroom.getClassId();
                                classroomName = classroom.getClassCode();
                                log.info("강의실 정보 조회 성공: classroomId={}, classroomName={}", classroomId, classroomName);
                            } else {
                                log.warn("강의실 정보를 찾을 수 없습니다: classId={}", classId);
                                classroomId = classId; // classId를 그대로 사용
                                classroomName = "강의실 정보 없음";
                            }
                        } else {
                            log.warn("과정에 강의실 정보가 없습니다: courseId={}", courseId);
                            classroomId = "no_classroom";
                            classroomName = "강의실 미지정";
                        }
                        
                        // 과정명과 강의실 정보를 courseInfo에 설정
                        courseInfo.put("courseName", courseName != null ? courseName : "과정명 없음");
                        courseInfo.put("classroomId", classroomId);
                        courseInfo.put("classroomName", classroomName);
                        
                    } else {
                        log.warn("과정 정보를 찾을 수 없습니다: courseId={}", courseId);
                        classroomId = "unknown_course";
                        classroomName = "알 수 없는 과정";
                        courseInfo.put("courseName", "알 수 없는 과정");
                        courseInfo.put("classroomId", classroomId);
                        courseInfo.put("classroomName", classroomName);
                    }
                } catch (Exception e) {
                    log.error("강의실 정보 조회 중 오류 발생: {}", e.getMessage(), e);
                    classroomId = "error_classroom";
                    classroomName = "조회 오류";
                    courseInfo.put("courseName", "조회 오류");
                    courseInfo.put("classroomId", classroomId);
                    courseInfo.put("classroomName", classroomName);
                }
                
                log.info("MemberEntity 정보 사용: courseId={}, classroomId={}", 
                        courseId, classroomId);
                
                courses.add(courseInfo);
            }
            
            result.put("success", true);
            result.put("message", "학생 수강 과목 및 강의실 정보 조회 성공");
            result.put("studentId", studentId);
            result.put("courses", courses);
            result.put("totalCount", courses.size());
            
            log.info("학생 수강 과목 및 강의실 정보 조회 완료: courses={}", courses);
            
        } catch (Exception e) {
            log.error("학생 수강 과목 및 강의실 정보 조회 실패: {}", e.getMessage(), e);
            
            result.put("success", false);
            result.put("message", "수강 과목 및 강의실 정보 조회에 실패했습니다: " + e.getMessage());
        }
        
        return result;
    }
}
