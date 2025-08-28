package com.jakdang.labs.api.chanwook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.api.chanwook.repository.AttendanceRepository;
import com.jakdang.labs.api.chanwook.repository.QrCodeRepository;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.QrCodeEntity;
import com.jakdang.labs.api.auth.repository.AuthRepository;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MobileAuthService {

    private final InstructorMemberRepository instructorMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final QrCodeRepository qrCodeRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenUtils tokenUtils;

    // QR 출석체크용 사용자 정보 조회 (로그인 대신)
    public Map<String, Object> getUserInfoForQR(String userId) {
        log.info("QR 출석체크용 사용자 정보 조회: {}", userId);
        
        try {
            // 1. userId로 학생 조회
            Optional<MemberEntity> memberOpt = instructorMemberRepository.findById(userId);
            
            if (memberOpt.isEmpty()) {
                throw new RuntimeException("존재하지 않는 사용자입니다.");
            }
            
            MemberEntity member = memberOpt.get();
            
            // 2. 학생 계정인지 확인
            if (!"ROLE_STUDENT".equals(member.getMemberRole())) {
                throw new RuntimeException("학생 계정만 QR 출석체크를 사용할 수 있습니다.");
            }
            
            // 3. 사용자 정보 반환 (QR 출석체크용)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "사용자 정보 조회 성공");
            response.put("userInfo", Map.of(
                "userId", member.getId(),
                "memberName", member.getMemberName(),
                "memberRole", member.getMemberRole(),
                "educationId", member.getEducationId() != null ? member.getEducationId() : ""
            ));
            
            log.info("QR 출석체크용 사용자 정보 조회 성공: {}", userId);
            return response;
            
        } catch (RuntimeException e) {
            log.error("QR 출석체크용 사용자 정보 조회 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("QR 출석체크용 사용자 정보 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("사용자 정보 조회 중 오류가 발생했습니다.");
        }
    }

    // 1. 모바일 학생 로그인 (users 테이블 사용) - 일반 로그인만 처리
    public Map<String, Object> mobileStudentLogin(String username, String password, String sessionId) {
        log.info("모바일 학생 로그인: username={}, sessionId={}", username, sessionId);
        
        try {
            // 1. DB에서 학생 계정 확인 (users 테이블에서 이메일로 조회)
            UserEntity student = authRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 계정입니다."));
            
            // 2. 학생 계정인지 확인
            if (!"ROLE_STUDENT".equals(student.getRole().toString())) {
                throw new RuntimeException("학생 계정만 로그인 가능합니다.");
            }
            
            // 3. 비밀번호 확인 (Argon2id 해시 검증)
            if (!passwordEncoder.matches(password, student.getPassword())) {
                log.info("비밀번호 불일치: 입력='{}', DB='{}'", password, student.getPassword());
                throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            log.info("비밀번호 일치 확인됨");
            
            // 4. 일반 로그인 성공 응답 (QR 코드와 무관)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인이 완료되었습니다.");
            response.put("userInfo", Map.of(
                "userId", student.getId(),
                "userEmail", student.getEmail(),
                "userRole", student.getRole().toString()
            ));
            
            // sessionId가 있으면 QR 코드 정보도 포함하고 과정 수강 여부 검증
            if (sessionId != null && !sessionId.isEmpty()) {
                try {
                    QrCodeEntity qrCodeEntity = qrCodeRepository.findQrCodeBySessionId(sessionId);
                    if (qrCodeEntity != null) {
                        String qrCourseId = qrCodeEntity.getCourseId();
                        String qrClassId = qrCodeEntity.getClassId();
                        
                        log.info("QR 코드 정보 조회: sessionId={}, courseId={}, classId={}", sessionId, qrCourseId, qrClassId);
                        
                        // 해당 학생이 QR 코드의 과정에 수강하는지 검증
                        List<Object[]> studentCourses = qrCodeRepository.findStudentCoursesById(student.getId());
                        boolean courseMatch = false;
                        
                        for (Object[] courseData : studentCourses) {
                            String studentCourseId = (String) courseData[2]; // courseId
                            if (qrCourseId.equals(studentCourseId)) {
                                courseMatch = true;
                                log.info("학생이 QR 과정에 수강 중: userId={}, courseId={}", student.getId(), qrCourseId);
                                break;
                            }
                        }
                        
                        if (!courseMatch) {
                            log.error("학생이 QR 과정에 수강하지 않음: userId={}, qrCourseId={}", student.getId(), qrCourseId);
                            throw new RuntimeException("해당 QR 코드의 과정에 수강하지 않는 학생입니다.");
                        }
                        
                        response.put("sessionId", sessionId);
                        response.put("courseId", qrCourseId);
                        response.put("classId", qrClassId);
                        log.info("QR 코드 정보 포함 및 검증 완료: sessionId={}, courseId={}", sessionId, qrCourseId);
                    }
                } catch (Exception e) {
                    log.error("QR 코드 검증 실패: {}", e.getMessage());
                    throw new RuntimeException("QR 코드 검증에 실패했습니다: " + e.getMessage());
                }
            }
            
            response.put("status", "LOGIN_SUCCESS");
            log.info("모바일 학생 로그인 성공: userId={}", student.getId());
            return response;
            
        } catch (RuntimeException e) {
            log.error("모바일 학생 로그인 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("모바일 학생 로그인 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("로그인 중 오류가 발생했습니다.");
        }
    }

    // 2. 모바일 로그아웃
    public Map<String, Object> mobileLogout() {
        log.info("모바일 로그아웃");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "로그아웃 성공");
        return response;
    }

    // 3. 모바일 로그인 상태 확인
    public Map<String, Object> checkMobileLoginStatus(HttpServletRequest request) {
        log.info("모바일 로그인 상태 확인");
        
        try {
            // 1. 요청에서 토큰 추출
            String accessToken = extractAccessToken(request);
            
            if (accessToken == null) {
                log.info("토큰이 없어 로그인되지 않은 상태");
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("isLoggedIn", false);
                response.put("message", "로그인이 필요합니다.");
                return response;
            }
            
            // 2. 토큰 유효성 검증
            boolean isValidToken = false;
            try {
                isValidToken = tokenUtils.isAccessTokenValid(accessToken) || 
                              (accessToken != null && !jwtUtil.isExpired(accessToken) && 
                               ("refresh".equals(jwtUtil.getCategory(accessToken)) || "access".equals(jwtUtil.getCategory(accessToken))));
            } catch (Exception e) {
                log.warn("토큰 검증 중 오류 발생: {}", e.getMessage());
            }
            
            if (!isValidToken) {
                log.info("토큰이 유효하지 않음");
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("isLoggedIn", false);
                response.put("message", "토큰이 만료되었거나 유효하지 않습니다.");
                return response;
            }
            
            // 3. 토큰에서 사용자 정보 추출
            String userEmail = jwtUtil.getUserEmail(accessToken);
            String userId = jwtUtil.getUserId(accessToken);
            String role = jwtUtil.getRole(accessToken);
            
            log.info("로그인 상태 확인 성공: email={}, userId={}, role={}", userEmail, userId, role);
            
            // 4. 로그인된 상태 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isLoggedIn", true);
            response.put("message", "로그인된 상태입니다.");
            response.put("userInfo", Map.of(
                "userId", userId,
                "userEmail", userEmail,
                "userRole", role
            ));
            return response;
            
        } catch (Exception e) {
            log.error("로그인 상태 확인 중 오류 발생: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("isLoggedIn", false);
            response.put("message", "로그인 상태 확인 중 오류가 발생했습니다.");
            return response;
        }
    }
    
    // 토큰 추출 메서드 (JWTFilter와 동일한 로직)
    private String extractAccessToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 (우선순위 1)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!token.isEmpty() && !token.equals("undefined")) {
                log.debug("Authorization 헤더에서 토큰 추출");
                return token;
            }
        }
        
        // 2. 쿠키에서 토큰 추출 (우선순위 2)
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                        log.debug("쿠키에서 토큰 추출: {}", cookie.getName());
                        return token;
                    }
                }
            }
        }
        
        log.debug("토큰을 찾을 수 없음");
        return null;
    }
} 