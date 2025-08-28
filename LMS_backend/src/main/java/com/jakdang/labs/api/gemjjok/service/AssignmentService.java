package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.entity.AssignmentEntity;
import com.jakdang.labs.api.gemjjok.DTO.*;
import com.jakdang.labs.api.gemjjok.repository.AssignmentRepository;
import com.jakdang.labs.api.gemjjok.repository.CourseListRepository;
import com.jakdang.labs.api.chanwook.repository.InstructorCourseRepository;
import com.jakdang.labs.api.gemjjok.util.AssignmentUuidMapper;
import org.springframework.web.multipart.MultipartFile;
import com.jakdang.labs.entity.AssignmentSubmissionEntity;
import com.jakdang.labs.api.gemjjok.repository.AssignmentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.jakdang.labs.api.gemjjok.repository.MemberRepository;
import com.jakdang.labs.api.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import com.jakdang.labs.entity.MemberEntity;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {
    
    private final AssignmentRepository assignmentRepository;
    private final RubricService rubricService;
    private final CourseListRepository courseListRepository;
    private final InstructorCourseRepository instructorCourseRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Qualifier("gemjjokMemberRepository")
    private final com.jakdang.labs.api.gemjjok.repository.MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final com.jakdang.labs.api.gemjjok.service.AssignmentMaterialService assignmentMaterialService;
    private final AssignmentSubmissionFileService assignmentSubmissionFileService;
    
    // 강사가 담당하는 모든 과제 목록 조회 (memberId 기준)
    public List<AssignmentListResponseDTO> getAllAssignmentsByMemberId(String userId) {
        // 1. users.id → email → member.memberEmail/ROLE_INSTRUCTOR → member.memberId
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        List<String> memberIds = memberRepository.findAll().stream()
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()))
            .filter(m -> email.equalsIgnoreCase(m.getMemberEmail()))
            .map(m -> m.getMemberId())
            .toList();
        if (memberIds.isEmpty()) return List.of();
        List<AssignmentEntity> assignments = assignmentRepository.findAll().stream()
            .filter(a -> memberIds.contains(a.getMemberId()))
            .collect(Collectors.toList());
        return assignments.stream().map(this::convertToListResponseDTO).collect(Collectors.toList());
    }

    // 진행중 과제 목록 조회 (users.id → email → member → courseId → assignment)
    public List<AssignmentListResponseDTO> getActiveAssignmentsByMemberId(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        List<String> courseIds = memberRepository.findByMemberEmailIgnoreCase(email)
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()) && m.getCourseId() != null)
            .map(m -> List.of(m.getCourseId()))
            .orElse(List.of());
        if (courseIds.isEmpty()) return List.of();
        List<AssignmentEntity> assignments = assignmentRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
            .filter(a -> a.getAssignmentActive() == 0 && a.getDueDate() != null && a.getDueDate().isAfter(java.time.LocalDateTime.now().toLocalDate()))
            .collect(Collectors.toList());
        return assignments.stream().map(this::convertToListResponseDTO).collect(Collectors.toList());
    }

    // 마감된 과제 목록 조회 (users.id → email → member → courseId → assignment)
    public List<AssignmentListResponseDTO> getCompletedAssignmentsByMemberId(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        List<String> courseIds = memberRepository.findByMemberEmailIgnoreCase(email)
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()) && m.getCourseId() != null)
            .map(m -> List.of(m.getCourseId()))
            .orElse(List.of());
        if (courseIds.isEmpty()) return List.of();
        List<AssignmentEntity> assignments = assignmentRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
            .filter(a -> a.getAssignmentActive() == 1 || (a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDateTime.now().toLocalDate())))
            .collect(Collectors.toList());
        return assignments.stream().map(this::convertToListResponseDTO).collect(Collectors.toList());
    }
    
    // 과제 상세 정보 조회 (users.id → email → member.memberId → assignment.memberId)
    public AssignmentDetailResponseDTO getAssignmentDetail(String assignmentId, String userId) {
        // 1. users.id → email → member.memberId
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        String memberId = memberRepository.findAll().stream()
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()))
            .filter(m -> email.equalsIgnoreCase(m.getMemberEmail()))
            .map(m -> m.getMemberId())
            .findFirst().orElse(null);
        if (memberId == null) throw new RuntimeException("강사 memberId를 찾을 수 없습니다.");
        // 2. assignmentId + memberId로 과제 찾기
        Optional<AssignmentEntity> assignmentOpt = assignmentRepository.findAll().stream()
            .filter(a -> assignmentId.equals(a.getAssignmentId()))
            .filter(a -> memberId.equals(a.getMemberId()))
            .findFirst();
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("과제를 찾을 수 없습니다. ID: " + assignmentId);
        }
        AssignmentEntity entity = assignmentOpt.get();

        // 루브릭 항목 조회
        List<RubricItemDTO> rubricItems = rubricService.getRubricByAssignmentId(entity.getAssignmentId()) != null
                ? rubricService.getRubricByAssignmentId(entity.getAssignmentId()).getRubricitem()
                : List.of();

        // maxScore 계산 (루브릭이 있으면 합계, 없으면 100)
        int maxScore = rubricItems != null && !rubricItems.isEmpty()
                ? rubricItems.stream().mapToInt(RubricItemDTO::getMaxScore).sum()
                : 100;

        return AssignmentDetailResponseDTO.builder()
                .assignmentId(entity.getAssignmentId())
                .courseId(entity.getCourseId())
                .courseName("") // TODO: 과정명 필요시 조회
                .assignmentTitle(entity.getAssignmentTitle())
                .assignmentContent(entity.getAssignmentContent())
                .dueDate(entity.getDueDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .maxScore(maxScore)
                .assignmentType("INDIVIDUAL")
                .status(entity.getAssignmentActive() == 0 ? "ACTIVE" : "INACTIVE")
                .memberId(entity.getMemberId())
                .fileRequired(entity.getFileRequired())
                .codeRequired(entity.getCodeRequired())
                .assignmentActive(entity.getAssignmentActive())
                .attachments(List.of()) // TODO: 첨부파일 구현 시 변경
                .submissions(List.of()) // TODO: 제출현황 구현 시 변경
                .submissionCount(0)
                .totalStudents(0)
                .averageScore(0.0)
                .instructions("")
                .evaluationCriteria("")
                .rubricitem(rubricItems)
                .build();
    }
    
    // 과제 등록
    @Transactional
    public AssignmentDetailResponseDTO createAssignment(AssignmentRequestDTO requestDTO) {
        // log.info("[과제등록] requestDTO: {}", requestDTO);
        // log.info("[과제등록] memberId from request: {}", requestDTO.getMemberId());
        // log.info("[과제등록] courseId from request: '{}'", requestDTO.getCourseId());
        // log.info("[과제등록] courseId type: {}", requestDTO.getCourseId() != null ? requestDTO.getCourseId().getClass().getName() : "null");
        
        // 1. users.id로 email 조회
        String email = userRepository.findById(requestDTO.getMemberId())
            .map(u -> u.getEmail())
            .orElse(null);
        // log.info("[과제등록] found email: {}", email);
        
        if (email == null) {
            // log.error("[과제등록] 사용자 정보를 찾을 수 없음: userId={}", requestDTO.getMemberId());
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }
        
                // 2. email로 member 정보 조회 (강사 역할)
        List<MemberEntity> members = memberRepository.findByMemberEmailIgnoreCase(email)
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()))
            .map(List::of)
            .orElse(List.of());

        // log.info("[과제등록] found members: {}", members);
        // log.info("[과제등록] request courseId: {}", requestDTO.getCourseId());

        if (members.isEmpty()) throw new RuntimeException("강사 정보를 찾을 수 없습니다.");

        // 3. memberId를 사용해서 course 테이블에서 해당 강사가 담당하는 강의 조회
        String memberId = null;
        for (MemberEntity member : members) {
            // log.info("[과제등록] checking member: memberId={}, memberRole={}", member.getMemberId(), member.getMemberRole());
            
            // course 테이블에서 해당 memberId로 강의 조회
            List<Map<String, Object>> courseMaps = instructorCourseRepository.findCoursesByMemberId(member.getMemberId());
            // log.info("[과제등록] found courses for member {}: {}", member.getMemberId(), courseMaps);
            
            // 요청된 courseId와 일치하는 강의가 있는지 확인
            boolean hasMatchingCourse = courseMaps.stream()
                .anyMatch(courseMap -> {
                    String courseId = (String) courseMap.get("courseId");
                    String requestCourseId = requestDTO.getCourseId();
                    
                    // 직접 비교
                    if (requestCourseId.equals(courseId)) {
                        return true;
                    }
                    
                    // 숫자 변환 후 비교 (문자열 "3"과 숫자 3 모두 지원)
                    try {
                        if (requestCourseId != null && courseId != null) {
                            int requestNum = Integer.parseInt(requestCourseId);
                            int courseNum = Integer.parseInt(courseId);
                            return requestNum == courseNum;
                        }
                    } catch (NumberFormatException e) {
                        // 숫자 변환 실패 시 무시
                    }
                    
                    return false;
                });
            
            if (hasMatchingCourse) {
                memberId = member.getMemberId();
                // log.info("[과제등록] found matching course for memberId: {}", memberId);
                break;
            }
        }

        // log.info("[과제등록] found memberId: {}", memberId);

        if (memberId == null) {
            throw new RuntimeException("해당 강의를 담당하는 강사 정보를 찾을 수 없습니다.");
        }
        AssignmentEntity assignment = new AssignmentEntity();
        assignment.setAssignmentId(UUID.randomUUID().toString());
        assignment.setCourseId(requestDTO.getCourseId());
        assignment.setAssignmentTitle(requestDTO.getAssignmentTitle());
        assignment.setAssignmentContent(requestDTO.getAssignmentContent());
        assignment.setDueDate(requestDTO.getDueDate());
        assignment.setFileRequired(requestDTO.getFileRequired() != null ? requestDTO.getFileRequired() : false);
        assignment.setCodeRequired(requestDTO.getCodeRequired() != null ? requestDTO.getCodeRequired() : false);
        assignment.setIsLocked(false);
        assignment.setAssignmentActive(0); // 0: 활성, 1: 삭제
        assignment.setMemberId(memberId); // 변환된 memberId 저장

        AssignmentEntity savedAssignment = assignmentRepository.save(assignment);

        // rubricitem 저장
        if (requestDTO.getRubricitem() != null && !requestDTO.getRubricitem().isEmpty()) {
            try {
                rubricService.saveRubric(
                    savedAssignment.getAssignmentId(),
                    requestDTO.getRubricTitle() != null ? requestDTO.getRubricTitle() : "채점 기준",
                    requestDTO.getRubricitem()
                );
            } catch (Exception e) {
                // System.err.println("[루브릭 저장 실패] assignmentId: " + savedAssignment.getAssignmentId());
                // e.printStackTrace();
                throw new RuntimeException("루브릭 저장에 실패했습니다.", e);
            }
        }

        return convertToDetailResponseDTO(savedAssignment);
    }
    
    // 과제 수정
    @Transactional
    public AssignmentDetailResponseDTO updateAssignment(String assignmentId, AssignmentRequestDTO requestDTO) {
        Optional<AssignmentEntity> assignmentOpt = assignmentRepository.findByAssignmentId(assignmentId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }
        AssignmentEntity assignment = assignmentOpt.get();
        assignment.setCourseId(requestDTO.getCourseId());
        assignment.setAssignmentTitle(requestDTO.getAssignmentTitle());
        assignment.setAssignmentContent(requestDTO.getAssignmentContent());
        assignment.setDueDate(requestDTO.getDueDate());
        assignment.setFileRequired(requestDTO.getFileRequired());
        assignment.setCodeRequired(requestDTO.getCodeRequired());
        assignmentRepository.save(assignment);

        // rubricitem 전체 삭제 후 새로 insert
        rubricService.deleteRubric(assignmentId);
        if (requestDTO.getRubricitem() != null && !requestDTO.getRubricitem().isEmpty()) {
            rubricService.saveRubric(
                assignmentId,
                requestDTO.getRubricTitle() != null ? requestDTO.getRubricTitle() : "채점 기준",
                requestDTO.getRubricitem()
            );
        }
        return convertToDetailResponseDTO(assignment);
    }
    
    // 과제 삭제
    @Transactional
    public void deleteAssignment(String assignmentId) {
        assignmentRepository.deleteById(assignmentId);
        rubricService.deleteRubric(assignmentId);
    }
    
    // 과제별 제출 현황 조회
    public List<AssignmentSubmissionDTO> getAssignmentSubmissions(String assignmentId) {
        // assignmentId로 assignmentsubmission 테이블에서 제출 현황 필터링
        return assignmentSubmissionRepository.findAll().stream()
            .filter(sub -> assignmentId.equals(sub.getAssignmentId()))
            .map(this::convertToSubmissionDTO)
            .collect(Collectors.toList());
    }
    
    // 과제 통계 조회 (users.id → email → member → courseId → assignment)
    public AssignmentStatsResponseDTO getAssignmentStats(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return AssignmentStatsResponseDTO.builder().build();
        List<String> courseIds = memberRepository.findByMemberEmailIgnoreCase(email)
            .filter(m -> "ROLE_INSTRUCTOR".equalsIgnoreCase(m.getMemberRole()) && m.getCourseId() != null)
            .map(m -> List.of(m.getCourseId()))
            .orElse(List.of());
        if (courseIds.isEmpty()) return AssignmentStatsResponseDTO.builder().build();
        List<AssignmentEntity> assignments = assignmentRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds);
        int total = assignments.size();
        int active = (int) assignments.stream().filter(a -> a.getAssignmentActive() == 0 && a.getDueDate() != null && a.getDueDate().isAfter(java.time.LocalDateTime.now().toLocalDate())).count();
        int completed = (int) assignments.stream().filter(a -> a.getAssignmentActive() == 1 || (a.getDueDate() != null && a.getDueDate().isBefore(java.time.LocalDateTime.now().toLocalDate()))).count();
        return AssignmentStatsResponseDTO.builder()
            .totalAssignments(total)
            .activeAssignments(active)
            .completedAssignments(completed)
            .draftAssignments(0)
            .totalSubmissions(0)
            .gradedSubmissions(0)
            .pendingSubmissions(0)
            .averageScore(0.0)
            .submissionRate(0.0)
            .build();
    }
    
    // 학생별 과제 리스트 조회
    public List<AssignmentListResponseDTO> getAssignmentsByStudentId(String studentId) {
        // 1. 학생이 수강 중인 모든 courseId 조회
        List<com.jakdang.labs.entity.CourseEntity> courses = courseListRepository.findCoursesByStudentId(studentId);
        List<String> courseIds = courses.stream().map(com.jakdang.labs.entity.CourseEntity::getCourseId).toList();
        if (courseIds.isEmpty()) return List.of();
        // 2. 각 courseId별로 과제 조회
        List<AssignmentEntity> assignments = assignmentRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds);
        // 3. DTO 변환
        return assignments.stream()
            .map(this::convertToListResponseDTO)
            .collect(java.util.stream.Collectors.toList());
    }
    
    // 학생용 과제 상세 조회
    public AssignmentDetailResponseDTO getStudentAssignmentDetail(String assignmentId, String studentId) {
        // 1. 학생이 수강 중인 모든 courseId 조회
        List<com.jakdang.labs.entity.CourseEntity> courses = courseListRepository.findCoursesByStudentId(studentId);
        List<String> courseIds = courses.stream().map(com.jakdang.labs.entity.CourseEntity::getCourseId).toList();
        if (courseIds.isEmpty()) {
            throw new RuntimeException("학생이 수강 중인 과정이 없습니다.");
        }
        
        // 2. 과제 조회 및 권한 확인
        Optional<AssignmentEntity> assignmentOpt = assignmentRepository.findByAssignmentId(assignmentId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("과제를 찾을 수 없습니다. ID: " + assignmentId);
        }
        
        AssignmentEntity assignment = assignmentOpt.get();
        
        // 3. 학생이 해당 과제의 과정을 수강하고 있는지 확인
        if (!courseIds.contains(assignment.getCourseId())) {
            throw new RuntimeException("해당 과제에 접근할 권한이 없습니다.");
        }
        
        // 4. 강사 정보 조회
        String instructorName = "";
        if (assignment.getMemberId() != null) {
            var instructor = memberRepository.findById(assignment.getMemberId()).orElse(null);
            if (instructor != null) {
                instructorName = instructor.getMemberName();
            }
        }
        
        // 5. 루브릭 정보 조회
        List<RubricItemDTO> rubricItems = rubricService.getRubricByAssignmentId(assignment.getAssignmentId()) != null
                ? rubricService.getRubricByAssignmentId(assignment.getAssignmentId()).getRubricitem()
                : List.of();
        
        // 6. maxScore 계산
        int maxScore = rubricItems != null && !rubricItems.isEmpty()
                ? rubricItems.stream().mapToInt(RubricItemDTO::getMaxScore).sum()
                : 100;
        
        // 7. 과정 정보 조회
        com.jakdang.labs.entity.CourseEntity course = courseListRepository.findById(assignment.getCourseId()).orElse(null);
        
        return AssignmentDetailResponseDTO.builder()
                .assignmentId(assignment.getAssignmentId())
                .courseId(assignment.getCourseId())
                .courseName(course != null ? course.getCourseName() : "")
                .courseCode(course != null ? course.getCourseCode() : "")
                .courseStartDay(course != null ? course.getCourseStartDay() : null)
                .courseEndDay(course != null ? course.getCourseEndDay() : null)
                .assignmentTitle(assignment.getAssignmentTitle())
                .assignmentContent(assignment.getAssignmentContent())
                .dueDate(assignment.getDueDate())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .maxScore(maxScore)
                .assignmentType("INDIVIDUAL")
                .status(assignment.getAssignmentActive() == 0 ? "ACTIVE" : "INACTIVE")
                .memberId(assignment.getMemberId())
                .instructorName(instructorName) // 강사 이름 추가
                .fileRequired(assignment.getFileRequired())
                .codeRequired(assignment.getCodeRequired())
                .assignmentActive(assignment.getAssignmentActive())
                .attachments(List.of()) // 첨부파일 구현 시 변경
                .submissions(List.of()) // 제출현황 구현 시 변경
                .submissionCount(0)
                .totalStudents(0)
                .averageScore(0.0)
                .instructions("")
                .evaluationCriteria("")
                .rubricitem(rubricItems)
                .build();
    }
    
    // DTO 변환 메서드들
    private AssignmentListResponseDTO convertToListResponseDTO(AssignmentEntity entity) {
        return AssignmentListResponseDTO.builder()
                .assignmentId(entity.getAssignmentId())
                .courseId(entity.getCourseId())
                .courseName("") // TODO: CourseService에서 과정명 조회
                .assignmentTitle(entity.getAssignmentTitle())
                .assignmentContent(entity.getAssignmentContent())
                .dueDate(entity.getDueDate())
                .createdAt(entity.getCreatedAt())
                .maxScore(100) // 기본값
                .assignmentType("INDIVIDUAL") // 기본값
                .submissionCount(0) // TODO: 제출 수 계산 로직 추가
                .totalStudents(0) // TODO: 학생 수 계산 로직 추가
                .status(entity.getAssignmentActive() == 0 ? "ACTIVE" : "INACTIVE")
                .memberId(entity.getMemberId())
                .build();
    }
    
    private AssignmentDetailResponseDTO convertToDetailResponseDTO(AssignmentEntity entity) {
        // 루브릭 정보 조회
        RubricDTO rubric = rubricService.getRubricByAssignmentId(entity.getAssignmentId());
        Boolean hasRubric = rubric != null;
        List<RubricItemDTO> rubricItems = rubric != null ? rubric.getRubricitem() : List.of();
        int maxScore = rubricItems != null && !rubricItems.isEmpty() ? rubricItems.stream().mapToInt(RubricItemDTO::getMaxScore).sum() : 100;

        // courseId로 강의 정보 조회
        com.jakdang.labs.entity.CourseEntity course = null;
        if (entity.getCourseId() != null) {
            course = courseListRepository.findById(entity.getCourseId()).orElse(null);
        }

        return AssignmentDetailResponseDTO.builder()
                .assignmentId(entity.getAssignmentId())
                .courseId(entity.getCourseId())
                .courseName(course != null ? course.getCourseName() : "")
                .courseCode(course != null ? course.getCourseCode() : "")
                .courseStartDay(course != null ? course.getCourseStartDay() : null)
                .courseEndDay(course != null ? course.getCourseEndDay() : null)
                .assignmentTitle(entity.getAssignmentTitle())
                .assignmentContent(entity.getAssignmentContent())
                .dueDate(entity.getDueDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .maxScore(maxScore)
                .assignmentType("INDIVIDUAL")
                .status(entity.getAssignmentActive() == 0 ? "ACTIVE" : "INACTIVE")
                .memberId(entity.getMemberId())
                .fileRequired(entity.getFileRequired())
                .codeRequired(entity.getCodeRequired())
                .assignmentActive(entity.getAssignmentActive())
                .attachments(List.of()) // 첨부파일 구현 시 변경
                .submissions(List.of()) // 제출현황 구현 시 변경
                .submissionCount(0)
                .totalStudents(0)
                .averageScore(0.0)
                .instructions("")
                .evaluationCriteria("")
                .rubricitem(rubricItems)
                .build();
    }

    // 학생 과제 제출
    @Transactional
    public void submitAssignment(String assignmentId, String id, String answerText, String submissionType, MultipartFile file) {
        // 1. 필수 파라미터 검증
        if (assignmentId == null || assignmentId.isBlank() ||
            id == null || id.isBlank() ||
            answerText == null || answerText.isBlank() ||
            submissionType == null || submissionType.isBlank()) {
            throw new IllegalArgumentException("필수 파라미터 누락: assignmentId, id, answerText, submissionType 모두 필요");
        }
        if (assignmentId.length() > 100) throw new IllegalArgumentException("assignmentId 길이 초과 (100자)");
        if (id.length() > 100) throw new IllegalArgumentException("id 길이 초과 (100자)");

        // 2. assignmentId, id DB 존재 여부 체크 (필요시)
        var assignmentOpt = assignmentRepository.findByAssignmentId(assignmentId);
        if (assignmentOpt.isEmpty()) throw new IllegalArgumentException("해당 assignmentId로 과제를 찾을 수 없습니다: " + assignmentId);

        // 3. 중복 제출 방지: 이미 제출한 경우 예외
        var already = assignmentSubmissionRepository.findByAssignmentIdAndId(assignmentId, id);
        if (already.isPresent()) {
            throw new IllegalArgumentException("이미 제출한 과제입니다. 수정은 PUT 메서드를 사용하세요.");
        }

        // 4. submissionType 값 검증
        if (!List.of("서술", "코드", "essay", "text").contains(submissionType)) {
            throw new IllegalArgumentException("허용되지 않는 제출 타입: " + submissionType);
        }

        // 5. users.id → email → member.memberId 변환
        String memberId = null;
        String email = userRepository.findById(id)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email != null) {
            memberId = memberRepository.findAll().stream()
                .filter(m -> "ROLE_STUDENT".equalsIgnoreCase(m.getMemberRole()))
                .filter(m -> email.equalsIgnoreCase(m.getMemberEmail()))
                .map(m -> m.getMemberId())
                .findFirst().orElse(null);
        }

        // 6. file 무시 (없음)
        // 7. createdAt, updatedAt 자동 세팅
        String submissionId = java.util.UUID.randomUUID().toString();
        AssignmentSubmissionEntity entity = AssignmentSubmissionEntity.builder()
            .submissionId(submissionId)
            .assignmentId(assignmentId)
            .id(id)
            .memberId(memberId) // memberId 설정
            .answerText(answerText)
            .submissionType(submissionType)
            .submissionStatus(0)
            .submittedAt(new java.sql.Timestamp(System.currentTimeMillis()))
            .createdAt(new java.sql.Timestamp(System.currentTimeMillis()))
            .updatedAt(new java.sql.Timestamp(System.currentTimeMillis()))
            .build();
        assignmentSubmissionRepository.save(entity);
        
        // 8. 과제 제출 파일들의 submissionId 업데이트
        try {
            assignmentSubmissionFileService.updateSubmissionId(assignmentId, id, submissionId);
        } catch (Exception e) {
            log.warn("과제 제출 파일 submissionId 업데이트 실패 - assignmentId: {}, studentId: {}, error: {}", 
                    assignmentId, id, e.getMessage());
            // 파일 업데이트 실패해도 과제 제출은 성공으로 처리
        }
    }

    // 학생 과제 제출 수정
    @Transactional
    public void updateAssignmentSubmission(String submissionId, String answerText, String submissionType, MultipartFile file) {
        AssignmentSubmissionEntity entity = assignmentSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("해당 제출 내역이 존재하지 않습니다."));
        if (answerText != null) entity.setAnswerText(answerText.trim());
        if (submissionType != null) entity.setSubmissionType(submissionType.trim());
        // TODO: 파일 저장 로직 필요시 구현 (예: S3, 로컬 등)
        // if (file != null && !file.isEmpty()) { ... }
        entity.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        assignmentSubmissionRepository.save(entity);
    }

    // 학생 과제 제출 삭제
    @Transactional
    public void deleteAssignmentSubmission(String submissionId) {
        if (!assignmentSubmissionRepository.existsById(submissionId)) {
            throw new IllegalArgumentException("해당 제출 내역이 존재하지 않습니다.");
        }
        assignmentSubmissionRepository.deleteById(submissionId);
    }

    // 피드백/점수 수정
    @Transactional
    public void updateSubmissionFeedbackAndScore(String submissionId, String feedback, Integer score) {
        AssignmentSubmissionEntity entity = assignmentSubmissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("해당 제출 내역이 존재하지 않습니다."));
        entity.setFeedback(feedback);
        entity.setScore(score);
        assignmentSubmissionRepository.save(entity);
        // System.out.println("[피드백/점수 수정] submissionId=" + submissionId + ", score=" + score + ", feedback=" + feedback);
    }

    public AssignmentSubmissionDTO getSubmissionByAssignmentIdAndId(String assignmentId, String id) {
        var entity = assignmentSubmissionRepository.findByAssignmentIdAndId(assignmentId, id);
        return entity.map(this::convertToSubmissionDTO).orElse(null);
    }

    public List<AssignmentSubmissionDTO> getAllAssignmentSubmissions() {
        return assignmentSubmissionRepository.findAll().stream()
            .map(this::convertToSubmissionDTO)
            .collect(Collectors.toList());
    }

    private AssignmentSubmissionDTO convertToSubmissionDTO(AssignmentSubmissionEntity entity) {
        return AssignmentSubmissionDTO.builder()
            .submissionId(entity.getSubmissionId())
            .assignmentId(entity.getAssignmentId())
            .id(entity.getId())
            .memberId(entity.getMemberId())
            .answerText(entity.getAnswerText())
            .submittedAt(entity.getSubmittedAt())
            .score(entity.getScore())
            .feedback(entity.getFeedback())
            .submissionStatus(entity.getSubmissionStatus())
            .submissionType(entity.getSubmissionType())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public java.util.List<AssignmentSubmissionDTO> getSubmissionsById(String id) {
        return assignmentSubmissionRepository.findByIdIn(List.of(id)).stream()
            .map(this::convertToSubmissionDTO)
            .collect(java.util.stream.Collectors.toList());
    }

    // 사용자별 과제 제출 목록 조회 (users.id → email → member → courseId → assignment → submission)
    public List<AssignmentSubmissionDTO> getSubmissionsByUserId(String userId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return List.of();
        
        // memberEmail로 id(PK) 조회
        Optional<com.jakdang.labs.entity.MemberEntity> memberOpt = memberRepository.findAll().stream()
            .filter(m -> email.equals(m.getMemberEmail()))
            .findFirst();
        if (memberOpt.isEmpty()) return List.of();
        
        String id = memberOpt.get().getId();
        return assignmentSubmissionRepository.findByIdIn(List.of(id)).stream()
            .map(this::convertToSubmissionDTO)
            .collect(Collectors.toList());
    }
    
    // 사용자별 특정 과제 제출 조회 (users.id → email → member → courseId → assignment → submission)
    public AssignmentSubmissionDTO getSubmissionByUserIdAndAssignmentId(String userId, String assignmentId) {
        String email = userRepository.findById(userId)
            .map(u -> u.getEmail())
            .orElse(null);
        if (email == null) return null;
        
        // memberEmail로 memberId(PK) 조회
        Optional<com.jakdang.labs.entity.MemberEntity> memberOpt = memberRepository.findAll().stream()
            .filter(m -> email.equals(m.getMemberEmail()))
            .findFirst();
        if (memberOpt.isEmpty()) return null;
        
        String memberId = memberOpt.get().getMemberId();
        Optional<AssignmentSubmissionEntity> submissionOpt = assignmentSubmissionRepository.findByAssignmentIdAndId(assignmentId, memberId);
        return submissionOpt.map(this::convertToSubmissionDTO).orElse(null);
    }
    
    // 과정별 학생 목록 조회
    public Map<String, Object> getCourseStudents(String courseId, String instructorId) {
        // log.info("과정별 학생 목록 조회: courseId={}, instructorId={}", courseId, instructorId);
        
        try {
            // 1. 강사의 users.id를 memberId로 변환
            String email = userRepository.findById(instructorId)
                .map(u -> u.getEmail())
                .orElse(null);
            if (email == null) {
                throw new RuntimeException("강사 이메일을 찾을 수 없습니다.");
            }
            
            // log.info("강사 이메일: {}", email);
            
            // 2. 이메일로 강사 정보 조회
            var instructorOpt = memberRepository.findByMemberEmailIgnoreCase(email);
            if (instructorOpt.isEmpty()) {
                throw new RuntimeException("강사 정보를 찾을 수 없습니다. 이메일: " + email);
            }
            
            var instructor = instructorOpt.get();
            if (!"ROLE_INSTRUCTOR".equalsIgnoreCase(instructor.getMemberRole())) {
                throw new RuntimeException("해당 사용자는 강사가 아닙니다.");
            }
            
            // log.info("강사 정보: memberId={}, memberName={}", instructor.getMemberId(), instructor.getMemberName());
            
            // 3. 강사가 해당 과정을 담당하는지 확인
            var course = courseListRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("해당 과정을 찾을 수 없습니다."));
            
            // log.info("과정 정보: courseId={}, courseName={}, educationId={}, courseMemberId={}", 
            //     course.getCourseId(), course.getCourseName(), course.getEducationId(), course.getMemberId());
            // log.info("강사 정보: instructorMemberId={}, instructorEducationId={}", 
            //     instructor.getMemberId(), instructor.getEducationId());
            
            // 강사가 해당 과정을 담당하는지 확인 (memberId 또는 educationId로 확인)
            boolean hasAccess = instructor.getMemberId().equals(course.getMemberId()) || 
                               instructor.getEducationId().equals(course.getEducationId());
            
            if (!hasAccess) {
                throw new RuntimeException("해당 과정에 접근할 권한이 없습니다. 강사 memberId: " + instructor.getMemberId() + 
                    ", 과정 memberId: " + course.getMemberId() + ", 강사 educationId: " + instructor.getEducationId() + 
                    ", 과정 educationId: " + course.getEducationId());
            }
            
            // log.info("과정 정보: courseId={}, courseName={}, educationId={}", course.getCourseId(), course.getCourseName(), course.getEducationId());
            
            // 4. 해당 과정의 학생들 조회 (courseId로 직접 필터링)
            // log.info("=== 학생 조회 시작 ===");
            // log.info("검색 조건: courseId={}, memberRole=ROLE_STUDENT", courseId);
            
            var students = memberRepository.findByCourseIdAndMemberRole(courseId, "ROLE_STUDENT");
            
            // log.info("조회된 학생 수: {} (courseId: {})", students.size(), courseId);
            
            // 학생 목록 로그 (처음 5명만)
            // students.stream().limit(5).forEach(student -> 
            //     log.info("학생 정보: memberId={}, memberName={}, courseId={}, memberRole={}", 
            //         student.getMemberId(), student.getMemberName(), student.getCourseId(), student.getMemberRole())
            // );
            
            // 5. 학생 정보와 출석률 계산 (출석률은 임시로 0으로 설정)
            List<Map<String, Object>> studentList = students.stream()
                .map(student -> {
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("userId", student.getId());
                    studentMap.put("memberId", student.getMemberId());
                    studentMap.put("memberName", student.getMemberName());
                    studentMap.put("memberEmail", student.getMemberEmail());
                    studentMap.put("courseId", courseId);
                    studentMap.put("courseName", course.getCourseName());
                    studentMap.put("attendanceRate", 0.0); // 임시로 0으로 설정
                    return studentMap;
                })
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("students", studentList);
            response.put("totalCount", studentList.size());
            response.put("courseId", courseId);
            response.put("courseName", course.getCourseName());
            
            return response;
        } catch (Exception e) {
            // log.error("과정별 학생 목록 조회 실패: {}", e.getMessage());
            throw new RuntimeException("과정별 학생 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    // 과제 파일 업로드 처리
    @Transactional
    public void uploadAssignmentFile(String assignmentId, MultipartFile file, Map<String, Object> fileInfo) {
        try {
            // log.info("[파일업로드] assignmentId: {}, fileName: {}, fileSize: {}", 
            //     assignmentId, file.getOriginalFilename(), file.getSize());
            
            // AssignmentMaterialService를 사용하여 파일 업로드 처리
            String fileId = (String) fileInfo.get("fileId");
            String fileKey = (String) fileInfo.get("fileKey");
            String title = (String) fileInfo.get("title");
            String uploadedBy = (String) fileInfo.get("uploadedBy");
            Long fileSize = file.getSize();
            
            // FileEnum 타입 변환
            com.jakdang.labs.api.file.dto.FileEnum fileType = com.jakdang.labs.api.file.dto.FileEnum.valueOf(
                (String) fileInfo.get("fileType")
            );
            
            // AssignmentMaterialService의 uploadAssignmentMaterial 메서드 사용
            assignmentMaterialService.uploadAssignmentMaterial(
                assignmentId,
                fileId,
                file.getOriginalFilename(),
                fileKey,
                fileSize,
                fileType,
                uploadedBy,
                title
            );
            
            // log.info("[파일업로드] 파일 업로드 완료");
        } catch (Exception e) {
            // log.error("[파일업로드] 파일 업로드 실패: ", e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }
} 