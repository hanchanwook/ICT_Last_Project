package com.jakdang.labs.api.chanwook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jakdang.labs.api.chanwook.repository.InstructorCourseRepository;
import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.api.chanwook.repository.AttendanceRepository;
import com.jakdang.labs.api.chanwook.repository.ClassroomRepository;
import com.jakdang.labs.api.chanwook.DTO.AttendanceDTO;
import com.jakdang.labs.entity.AttendanceEntity;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.ClassroomEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceManagementService {

    private final InstructorCourseRepository instructorCourseRepository;
    private final InstructorMemberRepository instructorMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassroomRepository classroomRepository;

    // 강사 담당 과정별 출석 현황 조회
    public Map<String, Object> getAttendanceStatus(String userId, Map<String, String> params) {
        log.info("강사 담당 과정별 출석 현황 조회: {}, params: {}", userId, params);

        // 날짜 파라미터 처리 (기본값: 오늘)
        LocalDate targetDate = LocalDate.now();
        if (params.containsKey("date")) {
            try {
                targetDate = LocalDate.parse(params.get("date"));
            } catch (Exception e) {
                log.warn("날짜 파라미터 파싱 실패, 오늘 날짜 사용: {}", params.get("date"));
            }
        }

        try {
            // DB에서 실제 데이터 조회 (classCode 포함)
            List<Map<String, Object>> courseAttendanceList = 
                instructorMemberRepository.findCourseAttendanceByUserId(userId, targetDate);
            
            // 출석률 계산 및 응답 데이터 구성
            List<Map<String, Object>> attendanceStatus = courseAttendanceList.stream()
                .map(course -> {
                    int totalStudents = ((Number) course.get("totalStudents")).intValue();
                    int presentCount = ((Number) course.get("presentCount")).intValue();
                    int absentCount = ((Number) course.get("absentCount")).intValue();
                    int lateCount = ((Number) course.get("lateCount")).intValue();
                    
                    // 출석률 계산
                    double attendanceRate = totalStudents > 0 ? 
                        ((double) presentCount / totalStudents) * 100 : 0.0;
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("courseId", course.get("courseId"));
                    result.put("courseName", course.get("courseName"));
                    
                    // 강의 일정 정보 추가
                    result.put("courseStartDay", course.get("courseStartDay"));
                    result.put("courseEndDay", course.get("courseEndDay"));
                    result.put("courseDays", course.get("courseDays"));
                    result.put("startTime", course.get("startTime"));
                    result.put("endTime", course.get("endTime"));
                    
                    // classId, classCode, classNumber를 추가 (쿼리에서 이미 가져온 데이터 사용)
                    Object classIdObj = course.get("classId");
                    Object classCodeObj = course.get("classCode");
                    Object classNumberObj = course.get("classNumber");
                    
                    // classId, classCode는 VARCHAR이므로 String으로 처리
                    String classId = classIdObj != null ? classIdObj.toString() : "";
                    String classCode = classCodeObj != null ? classCodeObj.toString() : "";
                    
                    // classNumber는 INT이므로 Integer로 처리
                    Integer classNumber = null;
                    if (classNumberObj != null) {
                        if (classNumberObj instanceof Number) {
                            classNumber = ((Number) classNumberObj).intValue();
                        } else {
                            try {
                                classNumber = Integer.parseInt(classNumberObj.toString());
                            } catch (NumberFormatException e) {
                                log.warn("classNumber 파싱 실패: {}", classNumberObj);
                                classNumber = null;
                            }
                        }
                    }
                    
                    result.put("classId", classId);
                    result.put("classCode", classCode);
                    result.put("classNumber", classNumber != null ? classNumber.toString() : "");
                    
                    if (classId != null && !classId.isEmpty() && classCode != null && !classCode.isEmpty()) {
                        log.info("과정 {}에 강의실 정보 추가: classId={}, classCode={}, classNumber={}", 
                                course.get("courseName"), classId, classCode, classNumber);
                    }
                    
                    // 출석 통계
                    result.put("totalStudents", totalStudents);
                    result.put("presentCount", presentCount);
                    result.put("absentCount", absentCount);
                    result.put("lateCount", lateCount);
                    result.put("attendanceRate", Math.round(attendanceRate * 10.0) / 10.0); // 소수점 첫째자리까지
                    
                    return result;
                })
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("attendanceStatus", attendanceStatus);
            response.put("totalCount", attendanceStatus.size());
            response.put("targetDate", targetDate.toString());
            
            log.info("강사 담당 과정별 출석 현황 조회 성공: {} 건", attendanceStatus.size());
            return response;
            
        } catch (Exception e) {
            log.error("강사 담당 과정별 출석 현황 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("출석 현황 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 특정 과정의 출석 기록 조회
    public Map<String, Object> getLectureAttendance(String courseId, Map<String, String> params) {
        log.info("특정 과정 출석 기록 조회: {}, params: {}", courseId, params);

        // 날짜 파라미터 처리 (기본값: 오늘)
        LocalDate targetDate = LocalDate.now();
        if (params.containsKey("date")) {
            try {
                targetDate = LocalDate.parse(params.get("date"));
            } catch (Exception e) {
                log.warn("날짜 파라미터 파싱 실패, 오늘 날짜 사용: {}", params.get("date"));
            }
        }

        try {
            // DB에서 실제 데이터 조회
            List<Map<String, Object>> attendanceRecords = 
                instructorMemberRepository.findAttendanceByCourseId(courseId, targetDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("courseId", courseId);
            response.put("lectureDate", targetDate.toString());
            response.put("attendanceRecords", attendanceRecords);
            response.put("totalCount", attendanceRecords.size());
            
            log.info("특정 과정 출석 기록 조회 성공: {} 건", attendanceRecords.size());
            return response;
            
        } catch (Exception e) {
            log.error("특정 과정 출석 기록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("출석 기록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 학생 출석 이력 조회 
    public Map<String, Object> getStudentAttendance(String studentId, String instructorId, Map<String, String> params) {
        log.info("학생 출석 이력 조회: studentId={}, instructorId={}, params={}", studentId, instructorId, params);

        try {
            // 1. 강사 권한 확인 (instructorId가 제공된 경우에만)
            if (instructorId != null && !instructorId.trim().isEmpty()) {
                List<MemberEntity> instructorMembers = instructorMemberRepository.findByIdField(instructorId);
                if (instructorMembers.isEmpty()) {
                    throw new RuntimeException("강사를 찾을 수 없습니다: " + instructorId);
                }
                log.info("✅ 강사 권한 확인 완료: {}", instructorId);
            } else {
                log.info("⚠️ instructorId가 제공되지 않아 권한 확인을 건너뜁니다.");
            }
            
            // 2. 학생 상세 정보 조회 (강사 권한 확인 포함)
            List<Map<String, Object>> studentDetails = 
                instructorMemberRepository.findStudentDetailById(studentId);
            
            if (studentDetails.isEmpty()) {
                throw new RuntimeException("학생을 찾을 수 없습니다: " + studentId);
            }
            
            // 첫 번째 결과 사용 (여러 과정이 있을 경우 첫 번째 과정 정보 사용)
            Map<String, Object> detail = studentDetails.get(0);
            log.info("🔍 학생 상세 정보 조회 결과:");
            log.info("   - userId: {}", detail.get("userId"));
            log.info("   - memberName: {}", detail.get("memberName"));
            log.info("   - courseName: {}", detail.get("courseName"));
            log.info("   - attendanceRate: {}", detail.get("attendanceRate"));
            
            // 3. 출석 이력 조회 (강사 권한 확인 포함)
            List<Map<String, Object>> attendanceHistory;
            if (instructorId != null && !instructorId.trim().isEmpty()) {
                // 강사 권한 확인이 있는 경우
                attendanceHistory = instructorMemberRepository.findStudentAttendanceHistory(studentId, instructorId);
                log.info("🔍 강사 권한 확인과 함께 출석 이력 조회: {} 건", attendanceHistory.size());
            } else {
                // 강사 권한 확인 없이 조회 (기존 방식)
                attendanceHistory = instructorMemberRepository.findStudentAttendanceHistoryWithoutAuth(studentId);
                log.info("🔍 권한 확인 없이 출석 이력 조회: {} 건", attendanceHistory.size());
            }
            
            // 디버깅: 출석 이력 로그
            log.info("🔍 출석 이력 조회 결과: {} 건", attendanceHistory.size());
            if (!attendanceHistory.isEmpty()) {
                Map<String, Object> firstRecord = attendanceHistory.get(0);
                log.info("   - 첫 번째 기록:");
                log.info("     * date: {}", firstRecord.get("date"));
                log.info("     * courseName: {}", firstRecord.get("courseName"));
                log.info("     * status: {}", firstRecord.get("status"));
            }
            
            // 출석 이력에서 null 값 처리 (courseId 포함)
            List<Map<String, Object>> cleanedAttendanceHistory = attendanceHistory.stream()
                .map(record -> {
                    Map<String, Object> cleanedRecord = new HashMap<>();
                    cleanedRecord.put("date", record.get("date") != null ? record.get("date") : "");
                    cleanedRecord.put("courseName", record.get("courseName") != null ? record.get("courseName") : "");
                    cleanedRecord.put("courseId", record.get("courseId") != null ? record.get("courseId") : "");
                    cleanedRecord.put("status", record.get("status") != null ? record.get("status") : "");
                    cleanedRecord.put("checkInTime", record.get("checkInTime") != null ? record.get("checkInTime") : "");
                    cleanedRecord.put("checkOutTime", record.get("checkOutTime") != null ? record.get("checkOutTime") : "");
                    return cleanedRecord;
                })
                .toList();
            
            // 총 출석 일수 계산
            int totalAttendanceDays = (int) cleanedAttendanceHistory.stream()
                .filter(record -> "출석".equals(record.get("status")))
                .count();
            
            // 출석률 계산 - 올바른 방식으로 수정
            double attendanceRate = 0.0;
            
            // 1. 학생의 과정 정보 가져오기
            // studentDetail에서 courseId를 직접 가져오거나, MemberEntity에서 조회
            String courseId = null;
            
            // MemberEntity에서 학생의 courseId 조회
            List<MemberEntity> studentMembers = instructorMemberRepository.findByIdField(studentId);
            if (!studentMembers.isEmpty()) {
                courseId = studentMembers.get(0).getCourseId();
                
                if (courseId != null && !courseId.isEmpty()) {
                    // CourseEntity에서 과정 정보 조회
                    Optional<CourseEntity> studentCourseOpt = instructorCourseRepository.findById(courseId);
                    
                    if (studentCourseOpt.isPresent()) {
                        CourseEntity studentCourse = studentCourseOpt.get();
                        
                        // 2. 과정의 전체 강의일 수 계산 (시작일~종료일)
                        LocalDate courseStart = studentCourse.getCourseStartDay();
                        LocalDate courseEnd = studentCourse.getCourseEndDay();
                        
                        if (courseStart != null && courseEnd != null) {
                            long totalLectureDays = 0;
                            LocalDate currentDate = courseStart;
                            
                            // 과정 요일에 따라 강의일 계산
                            String courseDays = studentCourse.getCourseDays();
                            
                            while (!currentDate.isAfter(courseEnd)) {
                                String dayOfWeek = currentDate.getDayOfWeek().toString();
                                String koreanDayOfWeek = convertToKoreanDay(dayOfWeek);
                                
                                if (courseDays != null && courseDays.contains(koreanDayOfWeek)) {
                                    totalLectureDays++;
                                }
                                currentDate = currentDate.plusDays(1);
                            }
                            
                            // 3. 실제 출석한 날짜 수 계산
                            long attendedDays = cleanedAttendanceHistory.stream()
                                .filter(record -> "출석".equals(record.get("status")))
                                .count();
                            
                            // 4. 출석률 계산
                            if (totalLectureDays > 0) {
                                attendanceRate = (attendedDays * 100.0) / totalLectureDays;
                            }
                        }
                    }
                }
            }
            
            final double calculatedAttendanceRate = attendanceRate;
            
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", studentId);
            response.put("studentName", detail.get("memberName") != null ? detail.get("memberName") : "");
            response.put("courseName", detail.get("courseName") != null ? detail.get("courseName") : "");
            response.put("totalCount", cleanedAttendanceHistory.size());
            response.put("totalAttendanceDays", totalAttendanceDays);
            response.put("attendanceRate", Math.round(attendanceRate * 10.0) / 10.0);
            
            // attendanceHistory에 모든 정보를 담아서 보내기
            List<Map<String, Object>> enrichedAttendanceHistory = cleanedAttendanceHistory.stream()
                .map(record -> {
                    Map<String, Object> enrichedRecord = new HashMap<>(record);
                    // 각 기록에 학생 정보와 과정 정보 추가
                    enrichedRecord.put("studentId", studentId);
                    enrichedRecord.put("studentName", detail.get("memberName") != null ? detail.get("memberName") : "");
                    enrichedRecord.put("courseName", detail.get("courseName") != null ? detail.get("courseName") : "");
                    enrichedRecord.put("totalAttendanceDays", totalAttendanceDays);
                    enrichedRecord.put("attendanceRate", Math.round(calculatedAttendanceRate * 10.0) / 10.0);
                    return enrichedRecord;
                })
                .toList();
            
            response.put("attendanceHistory", enrichedAttendanceHistory);
            
            log.info("학생 출석 이력 조회 성공: {} 건", attendanceHistory.size());
            return response;
            
        } catch (Exception e) {
            log.error("학생 출석 이력 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("학생 출석 이력 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 영어 요일을 한글 요일로 변환하는 메서드
    private String convertToKoreanDay(String englishDay) {
        return switch (englishDay) {
            case "MONDAY" -> "월";
            case "TUESDAY" -> "화";
            case "WEDNESDAY" -> "수";
            case "THURSDAY" -> "목";
            case "FRIDAY" -> "금";
            case "SATURDAY" -> "토";
            case "SUNDAY" -> "일";
            default -> englishDay;
        };
    }

    // 출석 기록 검색
    public Map<String, Object> searchAttendance(Map<String, String> searchParams) {
        log.info("출석 기록 검색: {}", searchParams);

        try {
            // 검색 파라미터 추출
            String userId = searchParams.get("userId");
            String courseId = searchParams.get("courseId");
            LocalDate date = null;
            String status = searchParams.get("status");
            String studentName = searchParams.get("studentName");
            
            // 날짜 파라미터 처리
            if (searchParams.containsKey("date")) {
                try {
                    date = LocalDate.parse(searchParams.get("date"));
                } catch (Exception e) {
                    log.warn("날짜 파라미터 파싱 실패: {}", searchParams.get("date"));
                }
            }
            
            // DB에서 실제 데이터 조회
            List<Map<String, Object>> attendanceRecords = 
                instructorMemberRepository.searchAttendanceRecords(userId, courseId, date, status, studentName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("attendanceRecords", attendanceRecords);
            response.put("totalCount", attendanceRecords.size());
            response.put("searchParams", searchParams);
            
            log.info("출석 기록 검색 성공: {} 건", attendanceRecords.size());
            return response;
            
        } catch (Exception e) {
            log.error("출석 기록 검색 실패: {}", e.getMessage(), e);
            throw new RuntimeException("출석 기록 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 출석 데이터 엑셀 내보내기용 데이터 조회
    public List<Map<String, Object>> getAttendanceDataForExport(Map<String, String> params) {
        log.info("출석 데이터 엑셀 내보내기용 데이터 조회: {}", params);
        
        try {
            // 검색 파라미터 추출
            String userId = params.get("userId");
            String courseId = params.get("courseId");
            LocalDate date = null;
            String status = params.get("status");
            String studentName = params.get("studentName");
            
            // 날짜 파라미터 처리
            if (params.containsKey("date")) {
                try {
                    date = LocalDate.parse(params.get("date"));
                } catch (Exception e) {
                    log.warn("날짜 파라미터 파싱 실패: {}", params.get("date"));
                }
            }
            
            // 디버깅: 파라미터 로그
            log.info("🔍 엑셀 내보내기 파라미터 분석:");
            log.info("   - userId: {}", userId);
            log.info("   - courseId: {}", courseId);
            log.info("   - date: {}", date);
            log.info("   - status: {}", status);
            log.info("   - studentName: {}", studentName);
            
            // 디버깅: 과정 정보 확인
            log.info("🔍 디버깅 - 과정 정보 확인:");
            try {
                // 해당 과정의 학생 수 확인
                Long studentCount = instructorMemberRepository.countStudentsByUserId(userId);
                log.info("   - 해당 강사의 총 학생 수: {}", studentCount);
                
                // 해당 날짜의 전체 출석 데이터 확인
                List<Map<String, Object>> allAttendance = instructorMemberRepository.findAttendanceByCourseId(courseId, date);
                log.info("   - 해당 날짜의 전체 출석 데이터 수: {}", allAttendance.size());
                
                if (!allAttendance.isEmpty()) {
                    Map<String, Object> firstRecord = allAttendance.get(0);
                    log.info("   - 첫 번째 출석 기록:");
                    log.info("     * userId: {}", firstRecord.get("userId"));
                    log.info("     * memberName: {}", firstRecord.get("memberName"));
                    log.info("     * status: {}", firstRecord.get("status"));
                }
                
                // 디버깅: 엑셀용 쿼리 결과 상세 분석
                List<Map<String, Object>> debugRecords = instructorMemberRepository.searchAttendanceRecords(userId, courseId, date, status, studentName);
                log.info("   - 엑셀용 쿼리 결과 수: {}", debugRecords.size());
                
                if (!debugRecords.isEmpty()) {
                    Map<String, Object> firstExcelRecord = debugRecords.get(0);
                    log.info("   - 첫 번째 엑셀용 기록 상세:");
                    log.info("     * userId: {}", firstExcelRecord.get("userId"));
                    log.info("     * memberName: {}", firstExcelRecord.get("memberName"));
                    log.info("     * courseName: {}", firstExcelRecord.get("courseName"));
                    log.info("     * date: {}", firstExcelRecord.get("date"));
                    log.info("     * status: {}", firstExcelRecord.get("status"));
                }
            } catch (Exception e) {
                log.warn("   - 디버깅 정보 조회 실패: {}", e.getMessage());
            }
            
            // DB에서 실제 데이터 조회
            List<Map<String, Object>> attendanceRecords = 
                instructorMemberRepository.searchAttendanceRecords(userId, courseId, date, status, studentName);
            
            // 시간 형식 변환 (마이크로초 제거)
            List<Map<String, Object>> processedRecords = new ArrayList<>();
            for (Map<String, Object> record : attendanceRecords) {
                // 읽기 전용 Map을 수정 가능한 HashMap으로 복사
                Map<String, Object> newRecord = new HashMap<>(record);
                
                // 시간 형식 변환 (마이크로초 제거)
                Object checkInTime = newRecord.get("checkInTime");
                if (checkInTime != null) {
                    String checkInStr = checkInTime.toString();
                    if (checkInStr.contains(".")) {
                        checkInStr = checkInStr.substring(0, checkInStr.indexOf("."));
                    }
                    newRecord.put("checkInTime", checkInStr);
                }
                
                Object checkOutTime = newRecord.get("checkOutTime");
                if (checkOutTime != null) {
                    String checkOutStr = checkOutTime.toString();
                    if (checkOutStr.contains(".")) {
                        checkOutStr = checkOutStr.substring(0, checkOutStr.indexOf("."));
                    }
                    newRecord.put("checkOutTime", checkOutStr);
                }
                
                processedRecords.add(newRecord);
            }
            
            // 처리된 레코드로 교체
            attendanceRecords = processedRecords;
            
            // 디버깅: 조회 결과 로그
            log.info("🔍 엑셀 내보내기 조회 결과: {} 건", attendanceRecords.size());
            if (!attendanceRecords.isEmpty()) {
                Map<String, Object> firstRecord = attendanceRecords.get(0);
                log.info("   - 첫 번째 기록:");
                log.info("     * userId: {}", firstRecord.get("userId"));
                log.info("     * memberName: {}", firstRecord.get("memberName"));
                log.info("     * courseId: {}", firstRecord.get("courseId"));
                log.info("     * courseName: {}", firstRecord.get("courseName"));
                log.info("     * date: {}", firstRecord.get("date"));
                log.info("     * status: {}", firstRecord.get("status"));
                log.info("     * checkInTime: {}", firstRecord.get("checkInTime"));
                log.info("     * checkOutTime: {}", firstRecord.get("checkOutTime"));
                
                // 모든 필드 확인
                log.info("   - 모든 필드 확인:");
                for (Map.Entry<String, Object> entry : firstRecord.entrySet()) {
                    log.info("     * {}: {}", entry.getKey(), entry.getValue());
                }
            }
            
            log.info("출석 데이터 엑셀 내보내기용 데이터 조회 성공: {} 건", attendanceRecords.size());
            return attendanceRecords;
            
        } catch (Exception e) {
            log.error("출석 데이터 엑셀 내보내기용 데이터 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("출석 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    // 출석 기록 수정 (courseId 기반 정확한 구분)
    public AttendanceDTO updateAttendance(String attendanceId, AttendanceDTO attendanceDTO) {
        log.info("출석 기록 수정: {}", attendanceId);
        log.info("수정할 데이터: {}", attendanceDTO);
        
        AttendanceEntity attendanceEntity = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("출석 기록을 찾을 수 없습니다: " + attendanceId));
        
        log.info("기존 Entity: {}", attendanceEntity);
        log.info("기존 courseId: {}", attendanceEntity.getCourseId());
        
        // courseId 일치 여부 확인 (보안 강화)
        if (attendanceDTO.getCourseId() != null && !attendanceDTO.getCourseId().equals(attendanceEntity.getCourseId())) {
            log.error("courseId 불일치: 요청된 courseId={}, 실제 courseId={}", 
                    attendanceDTO.getCourseId(), attendanceEntity.getCourseId());
            throw new RuntimeException("요청된 과정과 실제 출석 기록의 과정이 일치하지 않습니다.");
        }
        
        // DTO의 값으로 Entity 업데이트 (null 체크 제거하여 강제 업데이트)
        if (attendanceDTO.getCheckIn() != null) {
            attendanceEntity.setCheckIn(attendanceDTO.getCheckIn());
            log.info("checkIn 업데이트: {}", attendanceDTO.getCheckIn());
        }
        if (attendanceDTO.getCheckOut() != null) {
            attendanceEntity.setCheckOut(attendanceDTO.getCheckOut());
            log.info("checkOut 업데이트: {}", attendanceDTO.getCheckOut());
        }
        if (attendanceDTO.getAttendanceStatus() != null) {
            attendanceEntity.setAttendanceStatus(attendanceDTO.getAttendanceStatus());
            log.info("attendanceStatus 업데이트: {}", attendanceDTO.getAttendanceStatus());
        }
        if (attendanceDTO.getOfficialReason() != null) {
            attendanceEntity.setOfficialReason(attendanceDTO.getOfficialReason());
            log.info("officialReason 업데이트: {}", attendanceDTO.getOfficialReason());
        }
        if (attendanceDTO.getNote() != null) {
            attendanceEntity.setNote(attendanceDTO.getNote());
            log.info("note 업데이트: {}", attendanceDTO.getNote());
        }
        
        log.info("업데이트 후 Entity: {}", attendanceEntity);
        
        AttendanceEntity saved = attendanceRepository.save(attendanceEntity);
        log.info("저장된 Entity: {}", saved);
        
        return toDTO(saved);
    }
    
    // 출석 기록 수정 (userId, courseId 검증 포함)
    public AttendanceDTO updateAttendanceWithValidation(String attendanceId, String userId, String courseId, AttendanceDTO attendanceDTO) {
        log.info("출석 기록 수정 (검증 포함): attendanceId={}, userId={}, courseId={}", attendanceId, userId, courseId);
        log.info("수정할 데이터: {}", attendanceDTO);
        
        AttendanceEntity attendanceEntity = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("출석 기록을 찾을 수 없습니다: " + attendanceId));
        
        log.info("기존 Entity: {}", attendanceEntity);
        log.info("기존 userId: {}, 기존 courseId: {}", attendanceEntity.getUserId(), attendanceEntity.getCourseId());
        
        // courseId가 제공된 경우에만 courseId 일치 여부 확인
        if (courseId != null && !courseId.trim().isEmpty()) {
            if (!courseId.equals(attendanceEntity.getCourseId())) {
                log.error("courseId 불일치: 요청된 courseId={}, 실제 courseId={}", courseId, attendanceEntity.getCourseId());
                throw new RuntimeException("요청된 과정과 실제 출석 기록의 과정이 일치하지 않습니다.");
            }
            log.info("✅ courseId 검증 완료: 일치");
        } else {
            log.info("courseId가 제공되지 않아 courseId 검증을 건너뜁니다.");
        }
        
        log.info("✅ 검증 완료");
        
        // DTO의 값으로 Entity 업데이트
        if (attendanceDTO.getCheckIn() != null) {
            attendanceEntity.setCheckIn(attendanceDTO.getCheckIn());
            log.info("checkIn 업데이트: {}", attendanceDTO.getCheckIn());
        }
        if (attendanceDTO.getCheckOut() != null) {
            attendanceEntity.setCheckOut(attendanceDTO.getCheckOut());
            log.info("checkOut 업데이트: {}", attendanceDTO.getCheckOut());
        }
        if (attendanceDTO.getAttendanceStatus() != null) {
            attendanceEntity.setAttendanceStatus(attendanceDTO.getAttendanceStatus());
            log.info("attendanceStatus 업데이트: {}", attendanceDTO.getAttendanceStatus());
        }
        if (attendanceDTO.getOfficialReason() != null) {
            attendanceEntity.setOfficialReason(attendanceDTO.getOfficialReason());
            log.info("officialReason 업데이트: {}", attendanceDTO.getOfficialReason());
        }
        if (attendanceDTO.getNote() != null) {
            attendanceEntity.setNote(attendanceDTO.getNote());
            log.info("note 업데이트: {}", attendanceDTO.getNote());
        }
        
        log.info("업데이트 후 Entity: {}", attendanceEntity);
        
        AttendanceEntity saved = attendanceRepository.save(attendanceEntity);
        log.info("저장된 Entity: {}", saved);
        
        return toDTO(saved);
    }

    // 전체 학생 출석 기록 조회 (직원/강사용)
    public List<AttendanceDTO> getAllAttendances(Map<String, String> params) {
        log.info("전체 학생 출석 기록 조회: params={}", params);
        
        // userId 파라미터 추출
        String userId = params.get("userId");
        log.info("추출된 userId: {}", userId);
        
        // userId가 없으면 에러 처리
        if (userId == null || userId.trim().isEmpty()) {
            log.error("userId가 제공되지 않았습니다. params: {}", params);
            throw new RuntimeException("userId가 제공되지 않았습니다. 사용자 ID를 입력해주세요.");
        }
        
        // userId로 educationId 조회
        final String educationId;
        try {
            List<MemberEntity> userMembers = instructorMemberRepository.findByIdField(userId);
            if (!userMembers.isEmpty()) {
                educationId = userMembers.get(0).getEducationId();
                log.info("userId {}로 조회된 educationId: {}", userId, educationId);
            } else {
                educationId = null;
            }
        } catch (Exception e) {
            log.error("userId로 educationId 조회 실패: {}", e.getMessage());
            throw new RuntimeException("사용자 정보 조회에 실패했습니다: " + e.getMessage());
        }
        
        // educationId가 없으면 에러 처리
        if (educationId == null || educationId.trim().isEmpty()) {
            log.error("userId {}에 해당하는 educationId를 찾을 수 없습니다.", userId);
            throw new RuntimeException("해당 사용자의 교육기관 정보를 찾을 수 없습니다.");
        }
        
        List<Object[]> results;
        
        // 해당 교육기관의 출석 기록만 조회
        log.info("educationId로 출석 기록 조회: {}", educationId);
        results = attendanceRepository.findAllWithMemberAndCourseInfoByEducationId(educationId);
        
        return results.stream()
                .map(result -> {
                    AttendanceEntity entity = (AttendanceEntity) result[0];
                    String memberEmail = (String) result[1];
                    String memberName = (String) result[2];
                    String courseName = (String) result[3];
                    
                    // DTO에 별도 필드로 설정
                    AttendanceDTO dto = toDTO(entity);
                    dto.setMemberEmail(memberEmail);
                    dto.setMemberName(memberName);
                    dto.setCourseName(courseName);
                    
                    return dto;
                })
                .toList();
    }

    // 특정 학생 출석 기록 조회 (직원/강사용) - courseId 기반 구분
    public List<AttendanceDTO> getAttendancesByUserId(String userId) {
        log.info("특정 학생 출석 기록 조회: {}", userId);
        
        // userId로 모든 출석 기록 조회
        List<AttendanceEntity> entities = attendanceRepository.findByUserId(userId);
        
        // courseId별로 그룹화하여 각 과정의 출석 기록을 구분
        Map<String, List<AttendanceEntity>> courseGroupedAttendances = entities.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    entity -> entity.getCourseId() != null ? entity.getCourseId() : "unknown_course"
                ));
        
        log.info("학생 {}의 과정별 출석 기록: {}개 과정", userId, courseGroupedAttendances.size());
        
        // 각 과정별로 출석 기록을 DTO로 변환
        List<AttendanceDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<AttendanceEntity>> entry : courseGroupedAttendances.entrySet()) {
            String courseId = entry.getKey();
            List<AttendanceEntity> courseAttendances = entry.getValue();
            
            log.info("과정 {}의 출석 기록: {}건", courseId, courseAttendances.size());
            
            // 각 과정의 출석 기록을 DTO로 변환
            List<AttendanceDTO> courseDTOs = courseAttendances.stream()
                    .map(this::toDTO)
                    .toList();
            
            result.addAll(courseDTOs);
        }
        
        return result;
    }
    
    // 특정 학생의 특정 과정 출석 기록 조회 (courseId 기반 정확한 구분)
    public List<AttendanceDTO> getAttendancesByUserIdAndCourseId(String userId, String courseId) {
        log.info("특정 학생의 특정 과정 출석 기록 조회: userId={}, courseId={}", userId, courseId);
        
        if (courseId == null || courseId.trim().isEmpty()) {
            log.warn("courseId가 제공되지 않아 전체 출석 기록을 조회합니다.");
            return getAttendancesByUserId(userId);
        }
        
        // userId와 courseId로 정확한 출석 기록 조회
        List<AttendanceEntity> entities = attendanceRepository.findByUserIdAndCourseId(userId, courseId);
        
        log.info("학생 {}의 과정 {} 출석 기록: {}건", userId, courseId, entities.size());
        
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

    // Entity를 DTO로 변환
    private AttendanceDTO toDTO(AttendanceEntity entity) {
        return AttendanceDTO.builder()
                .attendanceId(entity.getAttendanceId())
                .userId(entity.getUserId())
                .courseId(entity.getCourseId()) // courseId 추가
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
} 