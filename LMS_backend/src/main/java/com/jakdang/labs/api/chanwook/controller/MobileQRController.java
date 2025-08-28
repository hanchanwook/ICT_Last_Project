package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakdang.labs.api.chanwook.DTO.AttendanceDTO;
import com.jakdang.labs.api.chanwook.service.QrAttendanceService;
import com.jakdang.labs.api.chanwook.service.QrCodeService;
import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.CourseRepository;

// 학생용 모바일 출석 관련 컨트롤러

@RestController
@RequestMapping("/api/student/qr")
@Slf4j
@RequiredArgsConstructor
public class MobileQRController {

    private final QrAttendanceService qrAttendanceService;
    private final QrCodeService qrCodeService;
    private final InstructorMemberRepository instructorMemberRepository;
    private final CourseRepository courseRepository;



    // QR 스캔 후 로그인 페이지로 이동 (출석체크 없음)
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanQRForLogin(@RequestBody Map<String, Object> scanData) {
        log.info("=== QR 스캔 (로그인 페이지 이동용) ===");
        log.info("요청 데이터: {}", scanData);
        
        try {
            String sessionId = (String) scanData.get("sessionId");
            
            if (sessionId == null || sessionId.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "세션 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // QR 세션 조회만
            Map<String, Object> qrSession = qrCodeService.getQRSession(sessionId);
            String status = (String) qrSession.get("status");
            
            if (!"ACTIVE".equals(status)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "QR 세션이 비활성화되었습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 로그인 페이지로 이동하기 위한 정보만 반환
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "QR 코드가 유효합니다. 로그인 페이지로 이동합니다.");
            response.put("sessionId", sessionId);
            response.put("classId", qrSession.get("classId"));
            response.put("status", "REDIRECT_TO_LOGIN");
            response.put("loginUrl", "/api/mobile/qr/login");
            response.put("checkInUrl", "/api/mobile/qr/check-in");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "QR 스캔에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // QR 출석체크 (인증 없이 접근 가능)
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkInAfterLogin(@RequestBody Map<String, Object> checkInData) {
        log.info("=== QR 출석체크 요청 ===");
        log.info("요청 데이터: {}", checkInData);
        
        try {
            // 요청 본문에서 사용자 정보 가져오기
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) checkInData.get("userInfo");
            
            if (userInfo == null) {
                log.error("사용자 정보가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "사용자 정보가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String userId = (String) userInfo.get("userId");
            
            if (userId == null || userId.isEmpty()) {
                log.error("사용자 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "사용자 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("=== 사용자 정보 확인 ===");
            log.info("사용자 ID: {}", userId);
            log.info("사용자 정보: {}", userInfo);
            
            // 필수 데이터 검증
            String sessionId = (String) checkInData.get("sessionId");
            String classroomId = (String) checkInData.get("classroomId");
            
            log.info("=== 데이터 파싱 결과 ===");
            log.info("세션 ID: {}", sessionId);
            log.info("강의실 ID: {}", classroomId);
            
            // 데이터 유효성 검사
            if (sessionId == null || sessionId.isEmpty()) {
                log.error("세션 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "세션 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (classroomId == null || classroomId.isEmpty()) {
                log.error("강의실 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "강의실 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("=== QR 세션 조회 시작 ===");
            // 세션 ID로 QR 세션 조회
            Map<String, Object> qrSession = qrCodeService.getQRSession(sessionId);
            log.info("QR 세션 조회 결과: {}", qrSession);
            
            String status = (String) qrSession.get("status");
            log.info("QR 세션 상태: {}", status);
            
            if (!"ACTIVE".equals(status)) {
                log.error("QR 세션이 비활성화 상태입니다. 현재 상태: {}", status);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "QR 세션이 비활성화되었습니다. 현재 상태: " + status);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("=== 출석체크 처리 시작 ===");
            // 실제 출석체크 로직 구현
            String sessionClassroomId = (String) qrSession.get("classroomId");
            String sessionCourseId = (String) qrSession.get("courseId");  // courseId 추가
            // String userId = (String) userInfo.get("userId");  // users 테이블에서는 "userId"
            
            log.info("출석체크 대상 강의실 ID: {}", sessionClassroomId);
            log.info("출석체크 대상 과정 ID: {}", sessionCourseId);
            log.info("출석체크 대상 학생 ID: {}", userId);
            
            // 실제 출석체크 수행 (courseId 포함)
            AttendanceDTO attendance = qrAttendanceService.checkIn(userId, sessionClassroomId, sessionCourseId);
            log.info("출석체크 처리 완료: attendanceId={}, status={}", attendance.getAttendanceId(), attendance.getAttendanceStatus());
            
            log.info("=== 출석체크 완료 ===");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "출석체크가 완료되었습니다.");
            response.put("sessionId", sessionId);
            response.put("classroomId", classroomId);
            response.put("userInfo", userInfo);
            response.put("status", "ATTENDANCE_RECORDED");
            response.put("attendanceId", attendance.getAttendanceId());
            response.put("attendanceStatus", attendance.getAttendanceStatus());
            response.put("checkInTime", attendance.getCheckIn());
            response.put("lectureDate", attendance.getLectureDate());
            
            log.info("=== 출석체크 응답 데이터 ===");
            log.info("응답: {}", response);
            log.info("출석체크 성공 완료 - 세션: {}, 강의실: {}", sessionId, sessionClassroomId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("=== 출석체크 실패 ===");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 스택 트레이스:", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "출석체크에 실패했습니다: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            log.error("에러 응답: {}", errorResponse);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // QR 코드 검증 (프론트엔드 호출용) !!
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateQRCode(@RequestBody Map<String, Object> qrData) {
        log.info("=== QR 코드 검증 요청 시작 ===");
        log.info("요청 데이터 타입: {}", qrData.getClass().getSimpleName());
        log.info("요청 데이터 크기: {} 개의 키", qrData.size());
        log.info("요청 데이터 키들: {}", qrData.keySet());
        log.info("요청 데이터 전체: {}", qrData);
        
        // 요청 데이터가 비어있는지 확인
        if (qrData == null || qrData.isEmpty()) {
            log.error("요청 데이터가 비어있습니다.");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "요청 데이터가 비어있습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // 필수 데이터 검증 (프론트엔드 데이터 구조에 맞춤)
            log.info("=== 데이터 파싱 시작 ===");
            
            // qrData 키가 있는지 확인하고 그 안에서 데이터 추출
            Object qrDataObj = qrData.get("qrData");
            log.info("qrData 객체 타입: {}", qrDataObj != null ? qrDataObj.getClass().getSimpleName() : "null");
            
            Map<String, Object> actualQrData = null;
            
            if (qrDataObj instanceof Map) {
                // Map으로 직접 전송된 경우
                actualQrData = (Map<String, Object>) qrDataObj;
                log.info("qrData가 Map으로 전송됨: {}", actualQrData);
            } else if (qrDataObj instanceof String) {
                // Base64로 인코딩된 JSON 문자열인 경우
                String qrDataString = (String) qrDataObj;
                log.info("qrData가 String으로 전송됨 (길이: {}): {}", qrDataString.length(), qrDataString);
                
                try {
                    // Base64 디코딩
                    byte[] decodedBytes = Base64.getDecoder().decode(qrDataString);
                    String decodedString = new String(decodedBytes);
                    log.info("Base64 디코딩 결과: {}", decodedString);
                    
                    // URL 디코딩 (필요한 경우)
                    String urlDecodedString = decodedString;
                    if (decodedString.contains("%")) {
                        urlDecodedString = URLDecoder.decode(decodedString, StandardCharsets.UTF_8);
                        log.info("URL 디코딩 결과: {}", urlDecodedString);
                    }
                    
                    // JSON 파싱
                    ObjectMapper objectMapper = new ObjectMapper();
                    actualQrData = objectMapper.readValue(urlDecodedString, Map.class);
                    log.info("JSON 파싱 결과: {}", actualQrData);
                } catch (Exception e) {
                    log.error("Base64 디코딩 또는 JSON 파싱 실패: {}", e.getMessage());
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "QR 데이터 파싱에 실패했습니다: " + e.getMessage());
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            if (actualQrData == null) {
                log.error("qrData를 파싱할 수 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "QR 데이터 형식이 올바르지 않습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Object sessionIdObj = actualQrData.get("sessionId");
            Object classroomIdObj = actualQrData.get("classroomId");
                
                log.info("세션 ID 객체 타입: {}", sessionIdObj != null ? sessionIdObj.getClass().getSimpleName() : "null");
                log.info("강의실 ID 객체 타입: {}", classroomIdObj != null ? classroomIdObj.getClass().getSimpleName() : "null");
                
                String sessionId = sessionIdObj != null ? sessionIdObj.toString() : null;
                String classroomId = classroomIdObj != null ? classroomIdObj.toString() : null;
                
                log.info("=== 데이터 파싱 결과 ===");
                log.info("세션 ID: {}", sessionId);
                log.info("강의실 ID: {}", classroomId);
                
                // 데이터 유효성 검사
                if (sessionId == null || sessionId.isEmpty()) {
                    log.error("세션 ID가 없습니다.");
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "세션 ID가 필요합니다.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                if (classroomId == null || classroomId.isEmpty()) {
                    log.error("강의실 ID가 없습니다.");
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "강의실 ID가 필요합니다.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                // QR 코드 데이터만으로 검증 진행
                
                log.info("=== QR 세션 조회 시작 ===");
                // 세션 ID로 QR 세션 조회
                Map<String, Object> qrSession = qrCodeService.getQRSession(sessionId);
                log.info("QR 세션 조회 결과: {}", qrSession);
                
                String status = (String) qrSession.get("status");
                log.info("QR 세션 상태: {}", status);
                
                if (!"ACTIVE".equals(status)) {
                    log.error("QR 세션이 비활성화 상태입니다. 현재 상태: {}", status);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "QR 세션이 비활성화되었습니다. 현재 상태: " + status);
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                log.info("=== QR 코드 검증 완료 ===");
                String classId = (String) qrSession.get("classId");
                log.info("검증된 강의실 ID: {}", classId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "QR 코드 검증이 완료되었습니다.");
                response.put("sessionId", sessionId);
                response.put("classroomId", classroomId);
                response.put("status", "VALIDATED");
                response.put("classId", classId);
                
                log.info("=== QR 코드 검증 응답 데이터 ===");
                log.info("응답: {}", response);
                log.info("QR 코드 검증 성공 완료 - 세션: {}, 강의실: {}", sessionId, classroomId);
                
                return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("=== QR 코드 검증 실패 ===");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 스택 트레이스:", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "QR 코드 검증에 실패했습니다: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            log.error("에러 응답: {}", errorResponse);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // QR 세션 상태 조회 !!
    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable String sessionId) {
        log.info("=== QR 세션 상태 조회 요청 === sessionId: {}", sessionId);
        
        try {
            // 세션 ID로 QR 세션 조회
            Map<String, Object> qrSession = qrCodeService.getQRSession(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("status", qrSession.get("status"));
            response.put("classId", qrSession.get("classId"));
            response.put("qrUrl", qrSession.get("qrUrl"));
            response.put("message", "QR 세션 상태 조회 완료");
            
            log.info("QR 세션 상태 조회 성공: sessionId={}, status={}", sessionId, qrSession.get("status"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("QR 세션 상태 조회 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "QR 세션 상태 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // QR 코드 정보 조회 
    @GetMapping("/info/{qrCodeId}")
    public ResponseEntity<Map<String, Object>> getQRInfo(@PathVariable String qrCodeId) {
        log.info("=== 휴대폰 QR 정보 조회 요청 === qrCodeId: {}", qrCodeId);
        
        try {
            Map<String, Object> qrSession = qrCodeService.getQRSession(qrCodeId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("qrCodeId", qrCodeId);
            response.put("status", qrSession.get("status"));
            response.put("classId", qrSession.get("classId"));
            response.put("qrUrl", qrSession.get("qrUrl"));
            response.put("message", "QR 코드가 유효합니다. 출석체크를 진행하세요.");
            
            log.info("휴대폰 QR 정보 조회 성공: {}", qrCodeId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("휴대폰 QR 정보 조회 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "QR 정보 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // QR 퇴실체크 (인증 없이 접근 가능)
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkOut(@RequestBody Map<String, Object> checkoutData) {
        log.info("=== QR 퇴실체크 요청 === data: {}", checkoutData);
        
        try {
            // 요청 본문에서 사용자 정보 가져오기
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) checkoutData.get("userInfo");
            
            if (userInfo == null) {
                log.error("사용자 정보가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "사용자 정보가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String userId = (String) userInfo.get("userId");
            
            if (userId == null || userId.isEmpty()) {
                log.error("사용자 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "사용자 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("=== 사용자 정보 확인 ===");
            log.info("사용자 ID: {}", userId);
            log.info("사용자 정보: {}", userInfo);
            
            // QR 데이터에서 정보 추출
            String sessionId = (String) checkoutData.get("sessionId");
            String classroomId = (String) checkoutData.get("classroomId");
            
            log.info("=== QR 퇴실체크 데이터 파싱 ===");
            log.info("세션 ID: {}", sessionId);
            log.info("강의실 ID: {}", classroomId);
            log.info("사용자 ID: {}", userId);
            
            // 데이터 유효성 검사
            if (sessionId == null || sessionId.isEmpty()) {
                log.error("세션 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "세션 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (classroomId == null || classroomId.isEmpty()) {
                log.error("강의실 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "강의실 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // QR 세션 검증
            Map<String, Object> qrSession = qrCodeService.getQRSession(sessionId);
            String status = (String) qrSession.get("status");
            
            if (!"ACTIVE".equals(status)) {
                log.error("QR 세션이 비활성화 상태입니다. 현재 상태: {}", status);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "QR 세션이 비활성화되었습니다. 현재 상태: " + status);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 강의실 정보 확인 (classroomId로 조회)
            String sessionClassroomId = (String) qrSession.get("classroomId");
            if (sessionClassroomId != null && !classroomId.equals(sessionClassroomId)) {
                log.error("QR 세션의 강의실과 요청 강의실이 일치하지 않습니다. 세션: {}, 요청: {}", sessionClassroomId, classroomId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "잘못된 강의실의 QR코드입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // classroomId가 null인 경우 경고 로그
            if (sessionClassroomId == null) {
                log.warn("QR 세션의 classroomId가 null입니다. 강의실 검증을 우회합니다. sessionId: {}", sessionId);
            }
            
            // courseId 추출 (QR 세션에서 가져오거나 요청에서 가져오기)
            String courseId = (String) qrSession.get("courseId");
            if (courseId == null) {
                courseId = (String) checkoutData.get("courseId");
            }
            
            if (courseId == null || courseId.isEmpty()) {
                log.error("과정 ID가 없습니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "과정 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 퇴실체크 처리
            AttendanceDTO attendance = qrAttendanceService.checkOut(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "QR 퇴실체크가 완료되었습니다.");
            response.put("sessionId", sessionId);
            response.put("classroomId", classroomId);
            response.put("userInfo", userInfo);
            response.put("status", "CHECKOUT_COMPLETED");
            response.put("attendanceId", attendance.getAttendanceId());
            response.put("checkOutTime", attendance.getCheckOut());
            
            log.info("QR 퇴실체크 성공: userId={}, attendanceId={}", userId, attendance.getAttendanceId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("QR 퇴실체크 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "퇴실체크에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 학생별 오늘 출석 상태 조회 (userId로 자동으로 courseId, classId, memberId 조회)
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getTodayAttendanceStatus(@PathVariable String userId) {
        log.info("=== 휴대폰 오늘 출석 상태 조회 요청 === userId: {}", userId);
        
        try {
                         // userId로 MemberEntity 직접 조회
             var memberEntity = instructorMemberRepository.findByIdField(userId);
             if (memberEntity == null || memberEntity.isEmpty()) {
                 log.error("사용자 정보를 찾을 수 없습니다: userId={}", userId);
                 Map<String, Object> errorResponse = new HashMap<>();
                 errorResponse.put("success", false);
                 errorResponse.put("message", "사용자 정보를 찾을 수 없습니다. 관리자에게 문의해주세요.");
                 return ResponseEntity.badRequest().body(errorResponse);
             }
             
             // 첫 번째 MemberEntity에서 memberId와 courseId 추출
             var member = memberEntity.get(0);
             String memberId = member.getMemberId();
             String courseId = member.getCourseId();
             
             log.info("사용자 수강 과정 자동 조회: userId={}, memberId={}, courseId={}", userId, memberId, courseId);
             
                          // courseId가 null이거나 비어있는지 확인
             if (courseId == null || courseId.isEmpty()) {
                 log.error("사용자의 수강 과정을 찾을 수 없습니다: userId={}", userId);
                 Map<String, Object> errorResponse = new HashMap<>();
                 errorResponse.put("success", false);
                 errorResponse.put("message", "수강 과정 정보를 찾을 수 없습니다.");
                 return ResponseEntity.badRequest().body(errorResponse);
             }
            
                         // courseId로 CourseEntity 조회하여 classId 가져오기
             var courseEntity = courseRepository.findById(courseId);
             if (courseEntity.isEmpty()) {
                 log.error("과정 정보를 찾을 수 없습니다: courseId={}", courseId);
                 Map<String, Object> errorResponse = new HashMap<>();
                 errorResponse.put("success", false);
                 errorResponse.put("message", "과정 정보를 찾을 수 없습니다.");
                 return ResponseEntity.badRequest().body(errorResponse);
             }
             
             String classId = courseEntity.get().getClassId();
             if (classId == null || classId.isEmpty()) {
                 log.error("강의실 정보를 찾을 수 없습니다: courseId={}", courseId);
                 Map<String, Object> errorResponse = new HashMap<>();
                 errorResponse.put("success", false);
                 errorResponse.put("message", "강의실 정보를 찾을 수 없습니다.");
                 return ResponseEntity.badRequest().body(errorResponse);
             }
            
            log.info("강의실 정보 조회: courseId={}, classId={}", courseId, classId);
            
            // 실제 DB에서 오늘 출석 상태 조회
            Map<String, Object> response = qrAttendanceService.getTodayAttendanceStatus(userId, courseId);
            
            // 응답에 추가 정보 포함
            response.put("memberId", memberId);
            response.put("classId", classId);
            
            log.info("휴대폰 오늘 출석 상태 조회 성공: userId={}, courseId={}, classId={}, action={}", 
                    userId, courseId, classId, response.get("action"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("휴대폰 오늘 출석 상태 조회 실패: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "출석 상태 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}