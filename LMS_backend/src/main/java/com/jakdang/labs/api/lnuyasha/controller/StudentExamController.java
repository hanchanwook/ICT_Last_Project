package com.jakdang.labs.api.lnuyasha.controller;

import com.jakdang.labs.api.lnuyasha.dto.AnswerDTO;
import com.jakdang.labs.api.lnuyasha.dto.QuestionDTO;
import com.jakdang.labs.api.lnuyasha.repository.KyMemberRepository;
import com.jakdang.labs.api.lnuyasha.service.StudentExamService;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.security.jwt.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import com.jakdang.labs.api.lnuyasha.dto.CourseDTO;
import com.jakdang.labs.api.lnuyasha.dto.ExamDTO;

@Slf4j
@RestController
@RequestMapping("/api/student/exam")
@RequiredArgsConstructor
public class StudentExamController {

    private final StudentExamService studentExamService;
    private final JwtUtil jwtUtil;
    private final KyMemberRepository memberRepository;

    /**
     * 학생 답안 조회 API
     * GET /api/student/exam/answers/{templateId}
     */
    @GetMapping("/answers/{templateId}")
    public ResponseEntity<ResponseDTO<List<AnswerDTO>>> getStudentAnswers(
            @PathVariable String templateId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        
        try {
            log.info("학생 답안 조회 요청 - templateId: {}, userId: {}", templateId, userId);
            
            // 학생 ID 추출 (쿼리 파라미터 우선, 없으면 JWT 토큰에서)
            String studentId = userId != null ? userId : extractStudentIdFromRequest(request);
            log.info("사용된 studentId: {} (쿼리 파라미터: {}, JWT 토큰: {})", 
                    studentId, userId, userId == null ? "사용됨" : "사용안됨");
            
            List<AnswerDTO> answers = studentExamService.getStudentAnswers(templateId, studentId);
            
            return ResponseEntity.ok(ResponseDTO.<List<AnswerDTO>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(answers)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("학생 답안 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.<List<AnswerDTO>>builder()
                    .resultCode("400")
                    .resultMessage("학생 답안 조회 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("학생 답안 조회 실패", e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<List<AnswerDTO>>builder()
                    .resultCode("500")
                    .resultMessage("학생 답안 조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 학생 답안 조회 API (templateId 사용)
     * GET /api/student/exam/answers/template/{templateId}
     */
    // @GetMapping("/answers/template/{templateId}")
    // public ResponseEntity<ResponseDTO<List<AnswerDTO>>> getStudentAnswersByTemplateId(
    //         @PathVariable String templateId,
    //         HttpServletRequest request) {
        
    //     try {
    //         log.info("학생 답안 조회 요청 (templateId) - templateId: {}", templateId);
            
    //         // JWT 토큰에서 학생 ID 추출
    //         String studentId = extractStudentIdFromRequest(request);
    //         log.info("JWT 토큰에서 추출한 studentId: {}", studentId);
            
    //         List<AnswerDTO> answers = studentExamService.getStudentAnswers(templateId, studentId);
            
    //         return ResponseEntity.ok(ResponseDTO.<List<AnswerDTO>>builder()
    //                 .resultCode("200")
    //                 .resultMessage("성공")
    //                 .data(answers)
    //                 .build());
                    
    //     } catch (IllegalArgumentException e) {
    //         log.error("학생 답안 조회 실패 - 잘못된 요청: {}", e.getMessage());
    //         return ResponseEntity.badRequest().body(ResponseDTO.<List<AnswerDTO>>builder()
    //                 .resultCode("400")
    //                 .resultMessage("학생 답안 조회 실패: " + e.getMessage())
    //                 .build());
    //     } catch (Exception e) {
    //         log.error("학생 답안 조회 실패", e);
    //         return ResponseEntity.internalServerError().body(ResponseDTO.<List<AnswerDTO>>builder()
    //                 .resultCode("500")
    //                 .resultMessage("학생 답안 조회 실패: " + e.getMessage())
    //                 .build());
    //     }
    // }

    /**
     * 시험 문제 상세 정보 조회 API
     * GET /api/student/exam/questions/{templateId}
    
     */
    @GetMapping("/questions/{templateId}")
    public ResponseEntity<ResponseDTO<List<QuestionDTO>>> getExamQuestions(
            @PathVariable String templateId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        
        try {
            log.info("시험 문제 조회 요청 - templateId: {}, userId: {}", templateId, userId);
            
            // JWT 토큰에서 학생 이메일 추출
            String studentEmail = extractStudentEmailFromRequest(request);
            log.info("JWT 토큰에서 추출한 studentEmail: {}", studentEmail);
            
            List<QuestionDTO> questions = studentExamService.getExamQuestionsByEmail(templateId, studentEmail);
            
            return ResponseEntity.ok(ResponseDTO.<List<QuestionDTO>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(questions)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("시험 문제 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.<List<QuestionDTO>>builder()
                    .resultCode("400")
                    .resultMessage("시험 문제 조회 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("시험 문제 조회 실패", e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<List<QuestionDTO>>builder()
                    .resultCode("500")
                    .resultMessage("시험 문제 조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 성적 확인 제출 API
     * POST /api/student/exam/confirm-grade
     */
    @PostMapping("/confirm-grade")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> confirmGrade(
            @RequestBody AnswerDTO request,
            @RequestParam(required = false) String userId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("성적 확인 제출 요청 - templateId: {}, courseId: {}", 
                    request.getTemplateId(), request.getCourseId());
            
            // JWT 토큰에서 학생 이메일 추출
            String studentEmail = extractStudentEmailFromRequest(httpRequest);
            log.info("JWT 토큰에서 추출한 studentEmail: {}", studentEmail);
            
            // 이메일과 courseId를 사용하여 성적 확인
            Map<String, Object> result = studentExamService.confirmGrade(request, studentEmail, request.getCourseId());
            
            return ResponseEntity.ok(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(result)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("성적 확인 제출 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("400")
                    .resultMessage("성적 확인 제출 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("성적 확인 제출 실패", e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("500")
                    .resultMessage("성적 확인 제출 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 학생 시험 과정 목록 조회 API
     * GET /api/student/exam/courses?userId={userId}
     */
    @GetMapping("/courses")
    public ResponseEntity<ResponseDTO<List<CourseDTO>>> getStudentCourses(
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        
        try {
            log.info("학생 시험 과정 목록 조회 요청 - 파라미터 userId: {} (사용하지 않음)", userId);
            
            // JWT 토큰에서 학생 이메일 추출 (파라미터는 사용하지 않음)
            String studentEmail = extractStudentEmailFromRequest(request);
            log.info("JWT 토큰에서 추출한 studentEmail: {}", studentEmail);
            
            List<CourseDTO> courses = studentExamService.getStudentCoursesByEmail(studentEmail);
            log.info("조회된 과정 수: {}", courses.size());
            
            // 각 과정의 상세 정보 로그 출력
            for (int i = 0; i < courses.size(); i++) {
                CourseDTO course = courses.get(i);
                log.info("과정 {}: courseId={}, courseName={}, courseCode={}", 
                    i + 1, course.getCourseId(), course.getCourseName(), course.getCourseCode());
            }
            
            return ResponseEntity.ok(ResponseDTO.<List<CourseDTO>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(courses)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("학생 시험 과정 목록 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.<List<CourseDTO>>builder()
                    .resultCode("400")
                    .resultMessage("학생 시험 과정 목록 조회 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("학생 시험 과정 목록 조회 실패", e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<List<CourseDTO>>builder()
                    .resultCode("500")
                    .resultMessage("학생 시험 과정 목록 조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 학생 시험 목록 조회 API
     * GET /api/student/exam/exams
     */
    @GetMapping("/exams")
    public ResponseEntity<ResponseDTO<List<ExamDTO>>> getStudentExams(
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        
        try {
            log.info("=== 학생 시험 목록 조회 API 호출됨 ===");
            log.info("요청된 userId: {}", userId);
            
            String studentId;
            if (userId != null && !userId.trim().isEmpty()) {
                // userId 파라미터가 있으면 해당 userId 사용
                studentId = userId;
                log.info("userId 파라미터 사용: {}", studentId);
            } else {
                // userId 파라미터가 없으면 JWT 토큰에서 이메일 추출
                String studentEmail = extractStudentEmailFromRequest(request);
                log.info("JWT 토큰에서 추출한 studentEmail: {}", studentEmail);
                studentId = studentEmail;
            }
            
            log.info("=== getStudentExams 메서드 호출 전 - 최종 studentId: {} ===", studentId);
            List<ExamDTO> exams = studentExamService.getStudentExams(studentId);
            log.info("=== getStudentExams 메서드 호출 후 - 결과 크기: {} ===", exams.size());
            
            return ResponseEntity.ok(ResponseDTO.<List<ExamDTO>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(exams)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("학생 시험 목록 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.<List<ExamDTO>>builder()
                    .resultCode("400")
                    .resultMessage("학생 시험 목록 조회 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("학생 시험 목록 조회 실패", e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<List<ExamDTO>>builder()
                    .resultCode("500")
                    .resultMessage("학생 시험 목록 조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 시험 시작 API
     * POST /api/student/exam/start/{templateId}
     */
    @PostMapping("/start/{templateId}")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> startExam(
            @PathVariable String templateId,
            HttpServletRequest request) {
        
        try {
            log.info("시험 시작 요청 - templateId: {}", templateId);
            
            // JWT 토큰에서 학생 이메일 추출
            String studentEmail = extractStudentEmailFromRequest(request);
            log.info("JWT 토큰에서 추출한 studentEmail: {}", studentEmail);
            
            Map<String, Object> examData = studentExamService.startExam(templateId, studentEmail);
            
            return ResponseEntity.ok(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(examData)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("시험 시작 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("400")
                    .resultMessage("시험 시작 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("시험 시작 실패", e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("500")
                    .resultMessage("시험 시작 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 학생 시험 답안 제출 API
     * POST /api/student/exam/submit/{templateId}
     */
    @PostMapping("/submit/{templateId}")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> submitExam(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        
        try {
            log.info("=== 학생 시험 답안 제출 요청 시작 ===");
            log.info("templateId: {}", templateId);
            log.info("requestBody: {}", requestBody);
            
            // 1. requestBody 검증
            if (requestBody == null) {
                log.error("requestBody가 null입니다.");
                return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                        .resultCode("400")
                        .resultMessage("요청 본문이 없습니다.")
                        .build());
            }
            
            // 2. 요청 본문에서 데이터 추출
            String courseId = (String) requestBody.get("courseId");
            String userId = (String) requestBody.get("userId");
            Object answersObj = requestBody.get("answers");
            
            log.info("추출된 데이터 - courseId: '{}', userId: '{}', answersObj: {}", 
                    courseId, userId, answersObj);
            
            // 3. 필수 데이터 검증
            if (courseId == null || courseId.trim().isEmpty()) {
                log.error("courseId가 null이거나 빈 문자열입니다.");
                return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                        .resultCode("400")
                        .resultMessage("courseId는 필수입니다.")
                        .build());
            }
            
            if (answersObj == null) {
                log.error("answers가 null입니다.");
                return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                        .resultCode("400")
                        .resultMessage("answers는 필수입니다.")
                        .build());
            }
            
            // 4. answers 데이터 타입 검증 및 변환
            Map<String, String> answers;
            if (answersObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rawAnswers = (Map<String, Object>) answersObj;
                answers = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : rawAnswers.entrySet()) {
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    answers.put(entry.getKey(), value);
                }
                log.info("변환된 answers: {}", answers);
            } else {
                log.error("answers가 Map이 아닙니다. 실제 타입: {}", answersObj.getClass().getSimpleName());
                return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                        .resultCode("400")
                        .resultMessage("answers는 Map 형태여야 합니다. 현재 타입: " + answersObj.getClass().getSimpleName())
                        .build());
            }
            
            // 5. 학생 이메일 조회
            String studentEmail;
            if (userId != null && !userId.trim().isEmpty() && courseId != null && !courseId.trim().isEmpty()) {
                log.info("userId와 courseId로 학생 이메일 조회 시도");
                try {
                    studentEmail = getStudentEmailByUserIdAndCourseId(userId, courseId);
                    log.info("userId/courseId로 조회된 studentEmail: {}", studentEmail);
                } catch (Exception e) {
                    log.error("userId/courseId로 학생 이메일 조회 실패: {}", e.getMessage());
                    return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                            .resultCode("400")
                            .resultMessage("학생 정보 조회 실패: " + e.getMessage())
                            .build());
                }
            } else {
                log.info("JWT 토큰에서 학생 이메일 추출 시도");
                try {
                    studentEmail = extractStudentEmailFromRequest(request);
                    log.info("JWT 토큰으로 조회된 studentEmail: {}", studentEmail);
                } catch (Exception e) {
                    log.error("JWT 토큰에서 학생 이메일 추출 실패: {}", e.getMessage());
                    return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                            .resultCode("400")
                            .resultMessage("인증 토큰 처리 실패: " + e.getMessage())
                            .build());
                }
            }
            
            // 6. 시험 제출 처리
            Map<String, Object> result;
            if (userId != null && !userId.trim().isEmpty() && courseId != null && !courseId.trim().isEmpty()) {
                log.info("userId/courseId로 시험 제출 처리");
                result = studentExamService.submitExamByUserIdAndCourseId(templateId, courseId, answers, userId);
            } else {
                log.info("이메일로 시험 제출 처리");
                result = studentExamService.submitExam(templateId, courseId, answers, studentEmail);
            }
            
            log.info("=== 학생 시험 답안 제출 완료 ===");
            return ResponseEntity.ok(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("200")
                    .resultMessage("성공")
                    .data(result)
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("학생 시험 답안 제출 실패 - 잘못된 요청: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("400")
                    .resultMessage("학생 시험 답안 제출 실패: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("학생 시험 답안 제출 실패 - 예상치 못한 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResponseDTO.<Map<String, Object>>builder()
                    .resultCode("500")
                    .resultMessage("학생 시험 답안 제출 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * JWT 토큰에서 학생 ID 추출
     */
    private String extractStudentIdFromRequest(HttpServletRequest request) {
        log.info("=== 학생 시험 JWT 토큰 추출 시작 ===");

        try {
            String authHeader = request.getHeader("Authorization");
            log.info("Authorization 헤더: {}", authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    log.info("Authorization 헤더에서 토큰 추출 성공");
                    return jwtUtil.getUserId(token);
                }
            }

            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                            log.info("쿠키에서 토큰 추출 성공: {}", cookie.getName());
                            return jwtUtil.getUserId(token);
                        }
                    }
                }
            }

            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    log.info("SecurityContext에서 인증 정보 발견: {}", authentication.getName());
                    if (authentication.getPrincipal() instanceof com.jakdang.labs.api.auth.dto.CustomUserDetails) {
                        com.jakdang.labs.api.auth.dto.CustomUserDetails userDetails =
                            (com.jakdang.labs.api.auth.dto.CustomUserDetails) authentication.getPrincipal();
                        String userId = userDetails.getUserId();
                        log.info("SecurityContext에서 추출한 사용자 ID: {}", userId);
                        return userId;
                    }
                }
            } catch (Exception e) {
                log.error("SecurityContext에서 사용자 정보 추출 실패: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("학생 시험: JWT 토큰에서 학생 ID 추출 중 오류: {}", e.getMessage(), e);
        }

        throw new IllegalArgumentException("JWT 토큰을 찾을 수 없습니다.");
    }

    /**
     * userId와 courseId로 학생 이메일 조회
     */
    private String getStudentEmailByUserIdAndCourseId(String userId, String courseId) {
        try {
            // MemberRepository를 통해 userId와 courseId로 학생 정보 조회
            MemberEntity student = memberRepository.findByMemberEmailAndCourseId(userId, courseId);
            if (student == null) {
                // 다른 방법으로 시도: id 컬럼으로 조회
                List<MemberEntity> students = memberRepository.findByIdColumn(userId);
                student = students.stream()
                    .filter(s -> courseId.equals(s.getCourseId()))
                    .findFirst()
                    .orElse(null);
            }
            
            if (student == null) {
                throw new IllegalArgumentException("해당 userId와 courseId의 학생을 찾을 수 없습니다: userId=" + userId + ", courseId=" + courseId);
            }
            
            String email = student.getMemberEmail();
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("해당 학생의 이메일 정보가 없습니다: userId=" + userId + ", courseId=" + courseId);
            }
            
            return email;
        } catch (Exception e) {
            log.error("userId와 courseId로 학생 이메일 조회 실패: {}", e.getMessage());
            throw new IllegalArgumentException("학생 정보 조회 실패: " + e.getMessage());
        }
    }

    /**
     * userId로 학생 이메일 조회 (기존 메서드 - 호환성 유지)
     */
    private String getStudentEmailByUserId(String userId) {
        try {
            // MemberRepository를 통해 userId(id 컬럼)로 학생 정보 조회
            List<MemberEntity> students = memberRepository.findByIdColumn(userId);
            if (students.isEmpty()) {
                throw new IllegalArgumentException("해당 userId의 학생을 찾을 수 없습니다: " + userId);
            }
            
            MemberEntity student = students.get(0);
            String email = student.getMemberEmail();
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("해당 학생의 이메일 정보가 없습니다: " + userId);
            }
            
            return email;
        } catch (Exception e) {
            log.error("userId로 학생 이메일 조회 실패: {}", e.getMessage());
            throw new IllegalArgumentException("학생 정보 조회 실패: " + e.getMessage());
        }
    }

    /**
     * JWT 토큰에서 학생 이메일 추출
     */
    private String extractStudentEmailFromRequest(HttpServletRequest request) {
        log.info("=== 학생 시험 JWT 토큰에서 이메일 추출 시작 ===");

        try {
            String authHeader = request.getHeader("Authorization");
            log.info("Authorization 헤더: {}", authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!token.isEmpty() && !token.equals("undefined")) {
                    log.info("Authorization 헤더에서 토큰 추출 성공");
                    return jwtUtil.getUserEmail(token);
                }
            }

            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName()) || "access_token".equals(cookie.getName()) || "refresh".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty() && !token.equals("undefined")) {
                            log.info("쿠키에서 토큰 추출 성공: {}", cookie.getName());
                            return jwtUtil.getUserEmail(token);
                        }
                    }
                }
            }

            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    log.info("SecurityContext에서 인증 정보 발견: {}", authentication.getName());
                    if (authentication.getPrincipal() instanceof com.jakdang.labs.api.auth.dto.CustomUserDetails) {
                        com.jakdang.labs.api.auth.dto.CustomUserDetails userDetails =
                            (com.jakdang.labs.api.auth.dto.CustomUserDetails) authentication.getPrincipal();
                        String email = userDetails.getEmail();
                        log.info("SecurityContext에서 추출한 사용자 이메일: {}", email);
                        return email;
                    }
                }
            } catch (Exception e) {
                log.error("SecurityContext에서 사용자 정보 추출 실패: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("학생 시험: JWT 토큰에서 학생 이메일 추출 중 오류: {}", e.getMessage(), e);
        }

        throw new IllegalArgumentException("JWT 토큰을 찾을 수 없습니다.");
    }
} 