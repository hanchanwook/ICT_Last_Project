package com.jakdang.labs.api.chanwook.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.jakdang.labs.api.chanwook.DTO.QrCodeDTO;
import com.jakdang.labs.entity.QrCodeEntity;
import com.jakdang.labs.entity.ClassroomEntity;
import com.jakdang.labs.entity.AttendanceEntity;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.api.chanwook.repository.QrCodeRepository;
import com.jakdang.labs.api.chanwook.repository.ClassroomRepository;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final ClassroomRepository classroomRepository;

    // 강의실별 QR 코드 조회
    public List<QrCodeDTO> getQrCodesByClassId(String classId) {
        log.info("강의실별 QR 코드 조회: {}", classId);
        List<QrCodeEntity> entities = qrCodeRepository.findByClassId(classId);
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

    // QR 코드 생성
    public QrCodeDTO createQrCode(QrCodeDTO qrCodeDTO) {
        log.info("QR 코드 생성: classId={}", qrCodeDTO.getClassId());
        
        // classId 유효성 검사
        if (qrCodeDTO.getClassId() == null || qrCodeDTO.getClassId().isEmpty()) {
            throw new RuntimeException("classId는 필수입니다.");
        }
        
        // classroom 존재 여부 확인
        ClassroomEntity classroom = classroomRepository.findById(qrCodeDTO.getClassId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 classroom입니다: " + qrCodeDTO.getClassId()));
        
        // classroom이 활성화 상태인지 확인 (classActive = 0)
        if (classroom.getClassActive() != 0) {
            throw new RuntimeException("비활성화된 classroom입니다: " + classroom.getClassCode());
        }
        
        log.info("유효한 classroom 확인: {}", classroom.getClassCode());
        
        // 동일 강의실의 기존 QR 코드들을 비활성화
        List<QrCodeEntity> existingQrCodes = qrCodeRepository.findByClassId(qrCodeDTO.getClassId());
        for (QrCodeEntity existing : existingQrCodes) {
            existing.setQrCodeActive(0);
            qrCodeRepository.save(existing);
        }
        
        // 새로운 QR 코드 생성
        QrCodeEntity qrCode = QrCodeEntity.builder()
                .classId(qrCodeDTO.getClassId())
                .courseId(qrCodeDTO.getCourseId())  // courseId 추가
                .qrUrl(qrCodeDTO.getQrUrl())
                .qrCodeActive(qrCodeDTO.getQrCodeActive())
                .build();
        
        QrCodeEntity saved = qrCodeRepository.save(qrCode);
        log.info("QR 코드 생성 완료: classroom={}", classroom.getClassCode());
        return toDTO(saved);
    }


    // QR 코드 삭제
    public void deleteQrCode(String id) {
        log.info("QR 코드 삭제: {}", id);
        
        QrCodeEntity qrCode = qrCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("QR 코드를 찾을 수 없습니다: " + id));
        
        // 실제 삭제 대신 비활성화 처리 (데이터 보존)
        qrCode.setQrCodeActive(0);
        qrCodeRepository.save(qrCode);
    }



    // 강의실별 활성화된 QR 코드 조회
    public List<QrCodeDTO> getActiveQrCodesByClassId(String classId) {
        log.info("강의실별 활성화된 QR 코드 조회: {}", classId);
        List<QrCodeEntity> entities = qrCodeRepository.findByClassIdAndQrCodeActive(classId, 1);
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }
    
    // 강의실 정보와 함께 QR 코드 조회
    public List<QrCodeDTO> getQrCodesWithClassroomInfo(String classId) {
        log.info("강의실 정보와 함께 QR 코드 조회: {}", classId);
        List<QrCodeEntity> entities = qrCodeRepository.findByClassId(classId);
        
        return entities.stream()
                .map(entity -> {
                    QrCodeDTO dto = toDTO(entity);
                    // 강의실 정보가 로드되어 있다면 추가 정보 설정
                    if (entity.getClassroom() != null) {
                        log.info("강의실 정보: {}", entity.getClassroom().getClassCode());
                    }
                    return dto;
                })
                .toList();
    }

    // ===== QR 출석 세션 관리 =====
    
    // QR 출석 세션 생성
    public Map<String, Object> createQRSession(Map<String, Object> sessionData) {
        log.info("QR 출석 세션 생성: {}", sessionData);
        
        try {
            String classId = (String) sessionData.get("classroomId"); // 프론트에서 보내는 classroomId 사용
            
            // QR 코드 생성 (classroomId 직접 사용)
            if (classId == null || classId.isEmpty()) {
                classId = getDefaultClassId();
            }
            
            // classId로 courseId 조회
            String courseId = null;
            try {
                List<CourseEntity> courses = qrCodeRepository.findCoursesByClassId(classId);
                if (!courses.isEmpty()) {
                    courseId = courses.get(0).getCourseId();
                    log.info("classId {}로 courseId 조회 성공: {}", classId, courseId);
                } else {
                    log.warn("classId {}에 해당하는 과정이 없습니다.", classId);
                }
            } catch (Exception e) {
                log.warn("courseId 조회 중 오류 발생: {}", e.getMessage());
            }
            
            // 먼저 QR 코드 생성 (UUID는 자동 생성됨)
            QrCodeDTO qrCodeDTO = QrCodeDTO.builder()
                    .classId(classId)
                    .courseId(courseId)  // courseId 추가
                    .qrUrl("") // 임시로 비워둠
                    .qrCodeActive(0)
                    .build();
            
            QrCodeDTO createdQrCode = createQrCode(qrCodeDTO);
            
            // 생성된 QR Code ID를 사용하여 실제 URL 생성 (동적 IP 사용)
            String qrUrl = "http://localhost:19091/api/mobile/qr/info/" + createdQrCode.getQrCodeId();
            
            // QR 코드 URL 직접 업데이트
            QrCodeEntity qrCodeEntity = qrCodeRepository.findById(createdQrCode.getQrCodeId())
                    .orElseThrow(() -> new RuntimeException("생성된 QR 코드를 찾을 수 없습니다: " + createdQrCode.getQrCodeId()));
            qrCodeEntity.setQrUrl(qrUrl);
            qrCodeRepository.save(qrCodeEntity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", createdQrCode.getQrCodeId());
            response.put("classroomId", classId); // 프론트와 일치하도록 classroomId로 반환
            response.put("courseId", courseId);   // courseId 추가
            response.put("startTime", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            response.put("qrUrl", qrUrl); // 업데이트된 URL 사용
            response.put("message", "QR 출석 세션이 성공적으로 생성되었습니다.");
            

            return response;
        } catch (Exception e) {
            log.error("QR 출석 세션 생성 실패: {}", e.getMessage());
            throw new RuntimeException("QR 출석 세션 생성에 실패했습니다.", e);
        }
    }
    
    // QR 출석 세션 조회 !!
    public Map<String, Object> getQRSession(String sessionId) {
        log.info("QR 출석 세션 조회: {}", sessionId);
        
        try {
            QrCodeEntity qrCode = qrCodeRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("QR 세션을 찾을 수 없습니다: " + sessionId));
            
            // 디버깅을 위한 상세 로그 추가
            log.info("=== QR 코드 엔티티 조회 결과 ===");
            log.info("qrCodeId: {}", qrCode.getQrCodeId());
            log.info("classId: {}", qrCode.getClassId());
            log.info("courseId: {}", qrCode.getCourseId());
            log.info("qrUrl: {}", qrCode.getQrUrl());
            log.info("qrCodeActive: {}", qrCode.getQrCodeActive());
            log.info("createdAt: {}", qrCode.getCreatedAt());
            log.info("updatedAt: {}", qrCode.getUpdatedAt());
            
            Map<String, Object> session = new HashMap<>();
            session.put("sessionId", qrCode.getQrCodeId());
            session.put("classroomId", qrCode.getClassId()); // 프론트와 일치하도록 classroomId로 반환
            session.put("courseId", qrCode.getCourseId());   // courseId 추가
            session.put("qrUrl", qrCode.getQrUrl());
            session.put("status", qrCode.getQrCodeActive() == 0 ? "ACTIVE" : "ENDED");  // 0은 활성화, 1은 비활성화
            
            log.info("=== 세션 응답 데이터 ===");
            log.info("session: {}", session);
            
            return session;
        } catch (Exception e) {
            log.error("QR 출석 세션 조회 실패: {}", e.getMessage());
            throw new RuntimeException("QR 출석 세션 조회에 실패했습니다.", e);
        }
    }
    
    // QR 출석 세션 종료
    public Map<String, Object> endQRSession(String sessionId) {
        log.info("QR 출석 세션 종료: {}", sessionId);
        
        try {
            QrCodeEntity qrCode = qrCodeRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("QR 세션을 찾을 수 없습니다: " + sessionId));
            
            // QR 코드 비활성화
            qrCode.setQrCodeActive(0);
            qrCodeRepository.save(qrCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("endTime", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            response.put("status", "ENDED");
            response.put("message", "QR 출석 세션이 성공적으로 종료되었습니다.");
            
            
            return response;
        } catch (Exception e) {
            log.error("QR 출석 세션 종료 실패: {}", e.getMessage());
            throw new RuntimeException("QR 출석 세션 종료에 실패했습니다.", e);
        }
    }
    
    // QR 출석 실시간 현황 조회
    public Map<String, Object> getQRAttendanceStatus(String sessionId) {
        log.info("QR 출석 실시간 현황 조회: {}", sessionId);
        
        try {
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("message", "QR 출석 실시간 현황 조회 기능이 준비 중입니다.");
            response.put("status", "PENDING");
            
            return response;
        } catch (Exception e) {
            log.error("QR 출석 실시간 현황 조회 실패: {}", e.getMessage());
            throw new RuntimeException("QR 출석 실시간 현황 조회에 실패했습니다.", e);
        }
    }

    // 특정 강의실의 QR 출석 현황 조회 (강사 권한 확인 포함)
    public Map<String, Object> getClassroomQRAttendance(String classroomId, Map<String, String> params) {
        log.info("특정 강의실 QR 출석 현황 조회: classroomId={}, params={}", classroomId, params);
        
        try {
            // 0. 강사 권한 확인 (userId 파라미터에서 추출)
            String userId = params.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                throw new RuntimeException("강사 권한 확인을 위해 userId가 필요합니다.");
            }
            
            // 해당 강사가 담당하는 강의실인지 확인
            List<ClassroomEntity> instructorClassrooms = qrCodeRepository.findClassroomsByInstructorId(userId);
            boolean hasPermission = instructorClassrooms.stream()
                    .anyMatch(classroom -> classroom.getClassId().equals(classroomId));
            
            if (!hasPermission) {
                log.warn("강사 {}가 강의실 {}에 대한 권한이 없습니다.", userId, classroomId);
                throw new RuntimeException("해당 강의실에 대한 조회 권한이 없습니다.");
            }
            
            log.info("강사 권한 확인 완료: userId={}, classroomId={}", userId, classroomId);
            
            // 1. 해당 강의실의 활성화된 QR 코드 조회
            List<QrCodeEntity> activeQrCodes = qrCodeRepository.findActiveQrCodesByClassroomId(classroomId);
            log.info("활성화된 QR 코드 수: {}", activeQrCodes.size());
            
            // 2. 해당 강의실의 출석 기록 조회 (Member 정보 포함)
            List<Object[]> attendanceData = qrCodeRepository.findAttendanceWithMemberInfo(classroomId);
            log.info("강의실 출석 기록 수: {}", attendanceData.size());
            
            // 출석 기록을 DTO로 변환 (Member 정보 포함)
            List<Map<String, Object>> attendanceList = attendanceData.stream()
                    .map(result -> {
                        AttendanceEntity attendance = (AttendanceEntity) result[0];
                        String memberEmail = (String) result[1];
                        String memberName = (String) result[2];
                        
                        Map<String, Object> attendanceMap = new HashMap<>();
                        attendanceMap.put("attendanceId", attendance.getAttendanceId());
                        attendanceMap.put("userId", attendance.getUserId());
                        attendanceMap.put("lectureDate", attendance.getLectureDate());
                        attendanceMap.put("attendanceStatus", attendance.getAttendanceStatus());
                        attendanceMap.put("checkIn", attendance.getCheckIn());
                        attendanceMap.put("checkOut", attendance.getCheckOut());
                        attendanceMap.put("note", attendance.getNote());
                        attendanceMap.put("classId", attendance.getClassId());
                        attendanceMap.put("memberEmail", memberEmail);
                        attendanceMap.put("memberName", memberName);
                        
                        return attendanceMap;
                    })
                    .toList();
            
            // 3. 강의실 정보 조회
            ClassroomEntity classroom = classroomRepository.findById(classroomId)
                    .orElse(null);
            
            // 4. 해당 강의실의 과정 정보 조회
            List<CourseEntity> courses = qrCodeRepository.findCoursesByClassId(classroomId);
            Map<String, Object> courseInfo = null;
            if (!courses.isEmpty()) {
                CourseEntity course = courses.get(0); // 첫 번째 과정 사용
                courseInfo = Map.of(
                    "courseId", course.getCourseId(),
                    "courseName", course.getCourseName(),
                    "courseStartDay", course.getCourseStartDay(),
                    "courseEndDay", course.getCourseEndDay(),
                    "courseDays", course.getCourseDays(),
                    "startTime", course.getStartTime(),
                    "endTime", course.getEndTime()
                );
                log.info("과정 정보 조회 성공: courseId={}, courseName={}", 
                        course.getCourseId(), course.getCourseName());
            } else {
                log.warn("강의실 {}에 해당하는 과정이 없습니다.", classroomId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("classroomId", classroomId);
            response.put("classroomInfo", classroom != null ? Map.of(
                "classCode", classroom.getClassCode(),
                "classNumber", classroom.getClassNumber(),
                "classCapacity", classroom.getClassCapacity()
            ) : null);
            response.put("courseInfo", courseInfo);
            response.put("activeQrCodes", activeQrCodes.stream().map(this::toDTO).toList());
            response.put("attendanceRecords", attendanceList);
            response.put("attendanceCount", attendanceData.size());
            response.put("status", "SUCCESS");
            response.put("message", "강의실 QR 출석 현황 조회가 완료되었습니다.");
            
            return response;
        } catch (Exception e) {
            log.error("특정 강의실 QR 출석 현황 조회 실패: {}", e.getMessage());
            throw new RuntimeException("특정 강의실 QR 출석 현황 조회에 실패했습니다.", e);
        }
    }


    
    // classroom ID 조회 (유효한 classroom이 없으면 예외 발생)
    private String getDefaultClassId() {
        // 사용 가능한 classroom 중 첫 번째 것을 사용 (활성화 + 빈 강의실)
        List<ClassroomEntity> activeClassrooms = classroomRepository.findAvailableClassrooms();
        
        if (!activeClassrooms.isEmpty()) {
            log.info("활성화된 classroom 사용: {}", activeClassrooms.get(0).getClassCode());
            return activeClassrooms.get(0).getClassId();
        }
        
        // 활성화된 classroom이 없으면 예외 발생
        log.error("활성화된 classroom이 없습니다. QR 코드 생성을 위해 classroom을 먼저 등록해주세요.");
        throw new RuntimeException("QR 코드 생성을 위해 활성화된 classroom이 필요합니다. classroom을 먼저 등록해주세요.");
    }

    // 강사별 강의실 목록 조회

    // 강의실 전체 목록 조회
    public Map<String, Object> getAllClassrooms() {
        log.info("강의실 전체 목록 조회 시작");
        
        try {
            List<ClassroomEntity> classrooms = qrCodeRepository.findAllClassrooms();
            
            List<Map<String, Object>> classroomList = classrooms.stream()
                    .map(classroom -> {
                        Map<String, Object> classroomMap = new HashMap<>();
                        classroomMap.put("classId", classroom.getClassId());
                        classroomMap.put("classCode", classroom.getClassCode());
                        classroomMap.put("classNumber", classroom.getClassNumber());
                        classroomMap.put("classCapacity", classroom.getClassCapacity());
                        classroomMap.put("classArea", classroom.getClassArea());
                        classroomMap.put("classActive", classroom.getClassActive());
                        
                        // 해당 강의실의 과정 정보 조회
                        List<CourseEntity> courses = qrCodeRepository.findCoursesByClassId(classroom.getClassId());
                        if (!courses.isEmpty()) {
                            CourseEntity course = courses.get(0);
                            classroomMap.put("courseId", course.getCourseId());
                            classroomMap.put("courseName", course.getCourseName());
                            classroomMap.put("courseStartDay", course.getCourseStartDay());
                            classroomMap.put("courseEndDay", course.getCourseEndDay());
                            classroomMap.put("courseDays", course.getCourseDays());
                            classroomMap.put("startTime", course.getStartTime());
                            classroomMap.put("endTime", course.getEndTime());
                            log.info("강의실 {}에 과정 정보 추가: courseId={}, courseName={}", 
                                    classroom.getClassCode(), course.getCourseId(), course.getCourseName());
                        } else {
                            log.info("강의실 {}에 해당하는 과정이 없습니다.", classroom.getClassCode());
                        }
                        
                        return classroomMap;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("classrooms", classroomList);
            response.put("totalCount", classroomList.size());
            response.put("status", "SUCCESS");
            response.put("message", "강의실 전체 목록 조회가 완료되었습니다.");
            
            log.info("강의실 전체 목록 조회 성공: {} 건", classroomList.size());
            return response;
            
        } catch (Exception e) {
            log.error("강의실 전체 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("강의실 전체 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    // 강사별 강의실 목록 조회 (강사가 담당하는 과정의 강의실만 조회)
    public Map<String, Object> getInstructorClassrooms(String userId) {
        log.info("강사별 강의실 목록 조회 시작: userId={}", userId);
        
        try {
            List<ClassroomEntity> classrooms = qrCodeRepository.findClassroomsByInstructorId(userId);
            
            List<Map<String, Object>> classroomList = classrooms.stream()
                    .map(classroom -> {
                        Map<String, Object> classroomMap = new HashMap<>();
                        classroomMap.put("classId", classroom.getClassId());
                        classroomMap.put("classCode", classroom.getClassCode());
                        classroomMap.put("classNumber", classroom.getClassNumber());
                        classroomMap.put("classCapacity", classroom.getClassCapacity());
                        classroomMap.put("classArea", classroom.getClassArea());
                        classroomMap.put("classActive", classroom.getClassActive());
                        
                        // 해당 강의실의 과정 정보 조회
                        List<CourseEntity> courses = qrCodeRepository.findCoursesByClassId(classroom.getClassId());
                        if (!courses.isEmpty()) {
                            CourseEntity course = courses.get(0);
                            classroomMap.put("courseId", course.getCourseId());
                            classroomMap.put("courseName", course.getCourseName());
                            classroomMap.put("courseStartDay", course.getCourseStartDay());
                            classroomMap.put("courseEndDay", course.getCourseEndDay());
                            classroomMap.put("courseDays", course.getCourseDays());
                            classroomMap.put("startTime", course.getStartTime());
                            classroomMap.put("endTime", course.getEndTime());
                            log.info("강의실 {}에 과정 정보 추가: courseId={}, courseName={}", 
                                    classroom.getClassCode(), course.getCourseId(), course.getCourseName());
                        } else {
                            log.info("강의실 {}에 해당하는 과정이 없습니다.", classroom.getClassCode());
                        }
                        
                        return classroomMap;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("classrooms", classroomList);
            response.put("totalCount", classroomList.size());
            response.put("status", "SUCCESS");
            response.put("message", "강사별 강의실 목록 조회가 완료되었습니다.");
            
            log.info("강사별 강의실 목록 조회 성공: {} 건", classroomList.size());
            return response;
            
        } catch (Exception e) {
            log.error("강사별 강의실 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("강사별 강의실 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    // 과정 전체 목록 조회
    public Map<String, Object> getAllCourses() {
        log.info("과정 전체 목록 조회 시작");
        
        try {
            List<CourseEntity> courses = qrCodeRepository.findAllCourses();
            
            List<Map<String, Object>> courseList = courses.stream()
                    .map(course -> {
                        Map<String, Object> courseMap = new HashMap<>();
                        courseMap.put("courseId", course.getCourseId());
                        courseMap.put("courseName", course.getCourseName());
                        courseMap.put("courseCode", course.getCourseCode());
                        courseMap.put("courseStartDay", course.getCourseStartDay());
                        courseMap.put("courseEndDay", course.getCourseEndDay());
                        courseMap.put("courseDays", course.getCourseDays());
                        courseMap.put("startTime", course.getStartTime());
                        courseMap.put("endTime", course.getEndTime());
                        courseMap.put("minCapacity", course.getMinCapacity());
                        courseMap.put("maxCapacity", course.getMaxCapacity());
                        courseMap.put("courseActive", course.getCourseActive());
                        courseMap.put("classId", course.getClassId());
                        courseMap.put("memberId", course.getMemberId());
                        courseMap.put("educationId", course.getEducationId());
                        
                        // 해당 과정의 강의실 정보 조회
                        ClassroomEntity classroom = classroomRepository.findById(course.getClassId()).orElse(null);
                        if (classroom != null) {
                            courseMap.put("classCode", classroom.getClassCode());
                            courseMap.put("classNumber", classroom.getClassNumber());
                            courseMap.put("classCapacity", classroom.getClassCapacity());
                            courseMap.put("classArea", classroom.getClassArea());
                            log.info("과정 {}에 강의실 정보 추가: classCode={}, classNumber={}", 
                                    course.getCourseName(), classroom.getClassCode(), classroom.getClassNumber());
                        } else {
                            log.warn("과정 {}에 해당하는 강의실이 없습니다.", course.getCourseName());
                        }
                        
                        // 해당 과정의 강사 정보 조회
                        List<Object[]> instructorData = qrCodeRepository.findInstructorByMemberId(course.getMemberId());
                        if (!instructorData.isEmpty()) {
                            Object[] instructor = instructorData.get(0);
                            courseMap.put("instructorId", instructor[0]);
                            courseMap.put("instructorName", instructor[1]);
                            courseMap.put("instructorEmail", instructor[2]);
                            log.info("과정 {}에 강사 정보 추가: instructorName={}", 
                                    course.getCourseName(), instructor[1]);
                        } else {
                            log.warn("과정 {}에 해당하는 강사가 없습니다.", course.getCourseName());
                        }
                        
                        return courseMap;
                    })
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("courses", courseList);
            response.put("totalCount", courseList.size());
            response.put("status", "SUCCESS");
            response.put("message", "과정 전체 목록 조회가 완료되었습니다.");
            
            log.info("과정 전체 목록 조회 성공: {} 건", courseList.size());
            return response;
            
        } catch (Exception e) {
            log.error("과정 전체 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("과정 전체 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    


    // Entity를 DTO로 변환
    private QrCodeDTO toDTO(QrCodeEntity entity) {
        return QrCodeDTO.builder()
                .qrCodeId(entity.getQrCodeId())
                .classId(entity.getClassId())
                .courseId(entity.getCourseId())  // courseId 추가
                .qrUrl(entity.getQrUrl())
                .qrCodeActive(entity.getQrCodeActive())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDate() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toLocalDate() : null)
                .build();
    }
}

