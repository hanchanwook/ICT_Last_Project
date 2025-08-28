package com.jakdang.labs.api.lnuyasha.service;

import com.jakdang.labs.entity.QuestionEntity;
import com.jakdang.labs.entity.QuestionOptEntity;
import com.jakdang.labs.entity.SubjectDetailEntity;
import com.jakdang.labs.api.lnuyasha.dto.*;
import com.jakdang.labs.api.lnuyasha.repository.KySubjectDetailRepository;
import com.jakdang.labs.api.lnuyasha.repository.QuestionOptRepository;
import com.jakdang.labs.api.lnuyasha.repository.QuestionRepository;
import com.jakdang.labs.api.lnuyasha.validator.QuestionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.jakdang.labs.api.lnuyasha.util.TimeZoneUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionBankService {
    
    private final QuestionRepository questionRepository;
    private final QuestionOptRepository questionOptRepository;
    private final KySubjectDetailRepository subDetailRepository;
    private final MemberService memberService;
    private final QuestionValidator questionValidator;
    
    /**
     * 전체 문제 목록 조회 - 간단한 쿼리 사용 (기존 방식)
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO getAllQuestions(String educationId) {
        log.info("전체 문제 목록 조회 요청");
        
        try {
            List<QuestionEntity> questions = questionRepository.findAllActiveQuestions(educationId);
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("전체 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 전체 문제 목록 조회 (강사 정보 포함)
     * @param educationId 학원 ID
     * @return 문제과 강사 정보를 포함한 응답 DTO
     */
    public Map<String, Object> getAllQuestionsWithInstructors(String educationId) {
        log.info("전체 문제 목록 조회 요청 (강사 정보 포함) - educationId: {}", educationId);
        
        try {
            // 모든 문제 조회 (educationId 필터링 제거)
            log.info("questionRepository.findAllActiveQuestionsWithoutEducationFilter() 호출 시작");
            List<QuestionEntity> questions = questionRepository.findAllActiveQuestionsWithoutEducationFilter();
            log.info("문제 조회 완료: {}개", questions.size());
            
            QuestionListResponseDTO questionResponse = buildQuestionListResponse(questions, 1, questions.size());
            log.info("QuestionListResponseDTO 생성 완료");
            
            // 해당 학원의 강사 정보 조회 (educationId가 "1"인 경우 모든 학원)
            log.info("memberService.getInstructorListSimple() 호출 시작 - educationId: {}", educationId);
            List<MemberInfoDTO> instructors = memberService.getInstructorListSimple(educationId);
            log.info("강사 정보 조회 완료: {}명", instructors.size());
            
            // 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("questions", questionResponse);
            response.put("instructors", instructors);
            
            log.info("응답 구성 완료");
            return response;
            
        } catch (Exception e) {
            log.error("전체 문제 목록 조회 중 오류 발생: educationId={}, error={}", educationId, e.getMessage(), e);
            throw new RuntimeException("문제 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 특정 선생의 문제 목록 조회 - memberId와 id 둘 다 시도
     * @param memberId 선생 ID
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO getQuestionsByTeacher(String memberId) {
        log.info("선생별 문제 목록 조회 요청: memberId = {}", memberId);
        
        try {
            // memberId가 null이거나 빈 문자열인지 확인
            if (memberId == null || memberId.trim().isEmpty()) {
                log.error("memberId가 null이거나 빈 문자열입니다: memberId = {}", memberId);
                throw new IllegalArgumentException("memberId가 유효하지 않습니다: " + memberId);
            }
            
            List<QuestionEntity> questions = new ArrayList<>();
            
            // 1. 먼저 memberId로 시도
            log.info("데이터베이스에서 memberId = {}로 문제 조회 시작", memberId);
            try {
                questions = questionRepository.findByMemberIdSafe(memberId);
                log.info("memberId로 조회 완료: memberId = {}, 조회된 문제 수 = {}", memberId, questions.size());
                
                // 디버깅: 모든 문제의 memberId 확인
                if (questions.isEmpty()) {
                    log.info("해당 memberId로 조회된 문제가 없어서 모든 문제의 memberId를 확인합니다");
                    List<QuestionEntity> allQuestions = questionRepository.findAll();
                    log.info("전체 문제 수: {}", allQuestions.size());
                    allQuestions.forEach(q -> log.info("문제 ID: {}, memberId: {}", q.getQuestionId(), q.getMemberId()));
                }
                
            } catch (Exception dbException) {
                log.error("데이터베이스 조회 중 구체적 오류: memberId = {}, 오류 타입 = {}, 오류 메시지 = {}", 
                         memberId, dbException.getClass().getSimpleName(), dbException.getMessage(), dbException);
                throw dbException;
            }
            
            // 2. memberId로 조회된 문제가 없으면, 해당 member의 id 값으로도 시도
            if (questions.isEmpty()) {
                log.info("memberId로 조회된 문제가 없어서 id 값으로도 시도합니다");
                try {
                    // MemberService를 통해 해당 member의 id 값을 가져와서 시도
                    MemberInfoDTO memberInfo = memberService.getMemberInfo(memberId);
                    if (memberInfo != null && !memberId.equals(memberInfo.getId())) {
                        log.info("memberId {}로 조회된 member의 id 값 {}로 문제 조회 시도", memberId, memberInfo.getId());
                        questions = questionRepository.findByMemberIdSafe(memberInfo.getId());
                        log.info("id로 조회 완료: id = {}, 조회된 문제 수 = {}", memberInfo.getId(), questions.size());
                    }
                } catch (Exception e) {
                    log.warn("member 정보 조회 중 오류 발생: memberId = {}, 오류 = {}", memberId, e.getMessage());
                }
            }
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("선생별 문제 목록 조회 중 오류 발생: memberId = {}, 오류 타입 = {}, 오류 메시지 = {}", 
                     memberId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("선생별 문제 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 문제 유형별 문제 목록 조회
     * @param questionType 문제 유형
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO getQuestionsByType(String questionType) {
        log.info("문제 유형별 목록 조회 요청: questionType = {}", questionType);
        
        try {
            List<QuestionEntity> questions = questionRepository.findByQuestionType(questionType);
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("문제 유형별 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 유형별 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 세부과목별 문제 목록 조회 - 간단한 쿼리 사용
     * @param subDetailId 세부과목 ID
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO getQuestionsBySubDetail(String subDetailId) {
        log.info("세부과목별 문제 목록 조회 요청: subDetailId = {}", subDetailId);
        
        try {
            List<QuestionEntity> questions = questionRepository.findBySubDetailId(subDetailId);
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("세부과목별 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("세부과목별 문제 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 상위 과목별 문제 목록 조회 - 상위 과목 정보 포함
     * @param subjectId 상위 과목 ID
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO getQuestionsBySubject(String subjectId) {
        log.info("상위 과목별 문제 목록 조회 요청: subjectId = {}", subjectId);
        
        try {
            List<QuestionEntity> questions = questionRepository.findAll(); // 임시로 모든 문제 조회
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("상위 과목별 문제 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("상위 과목별 문제 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 문제 검색
     * @param searchKeyword 검색 키워드
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO searchQuestions(String searchKeyword) {
        log.info("문제 검색 요청: searchKeyword = {}", searchKeyword);
        
        try {
            List<QuestionEntity> questions = questionRepository.searchQuestions(searchKeyword);
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("문제 검색 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 검색 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 실시간 검색 (복합 조건) - 상위 과목 정보 포함
     * @param keyword 검색 키워드
     * @param questionType 문제 유형
     * @param memberId 선생 ID
     * @param subDetailId 세부과목 ID
     * @param educationId 학원 ID
     * @param sortBy 정렬 기준
     * @param sortDirection 정렬 방향
     * @return 문제 목록 응답 DTO
     */
    public QuestionListResponseDTO realTimeSearch(String keyword, String questionType, String memberId, 
                                                 String subDetailId, String educationId, String sortBy, String sortDirection) {
        log.info("실시간 검색 요청: keyword={}, type={}, memberId={}", keyword, questionType, memberId);
        
        try {
            // 기본값 설정
            if (sortBy == null) {
                sortBy = "createdAt";
                sortDirection = "desc";
            }
            
            // 복합 조건으로 문제 검색 (임시로 모든 문제 조회)
            List<QuestionEntity> questions = questionRepository.findAll();
            
            return buildQuestionListResponse(questions, 1, questions.size());
            
        } catch (Exception e) {
            log.error("실시간 검색 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("실시간 검색 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 실시간 검색 결과 개수 조회
     * @param keyword 검색 키워드
     * @param questionType 문제 유형
     * @param memberId 선생 ID
     * @param subDetailId 세부과목 ID
     * @param educationId 학원 ID
     * @return 검색 결과 개수
     */
    public long getRealTimeSearchCount(String keyword, String questionType, String memberId, 
                                     String subDetailId, String educationId) {
        log.info("실시간 검색 결과 개수 조회 요청: keyword={}, type={}, memberId={}", keyword, questionType, memberId);
        
        try {
            return questionRepository.countQuestionsWithFilters(
                    keyword, questionType, memberId, subDetailId, educationId);
            
        } catch (Exception e) {
            log.error("실시간 검색 결과 개수 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("실시간 검색 결과 개수 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 프론트엔드 검색 및 필터링 (텍스트 검색, 연도 필터, 상태 필터)
     * @param request 검색 요청 DTO
     * @param page 현재 페이지
     * @param limit 페이지당 항목 수
     * @return 검색된 문제 목록
     */
    public QuestionListResponseDTO searchQuestionsWithFilters(QuestionSearchRequestDTO request, int page, int limit) {
        log.info("프론트엔드 검색 요청: {}", request);
        
        try {
            List<QuestionEntity> questions = questionRepository.findQuestionsWithFrontendFilters(
                    request.getSearchTerm(),
                    request.getSelectedYear(),
                    request.getSelectedStatus(),
                    request.getMemberId(),
                    request.getQuestionType(),
                    request.getSubDetailId()
            );
            
            return buildQuestionListResponse(questions, page, limit);
            
        } catch (Exception e) {
            log.error("프론트엔드 검색 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("프론트엔드 검색 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 프론트엔드 검색 결과 개수 조회
     * @param request 검색 요청 DTO
     * @return 검색 결과 개수
     */
    public long getSearchResultCount(QuestionSearchRequestDTO request) {
        log.info("프론트엔드 검색 결과 개수 조회 요청: {}", request);
        
        try {
            return questionRepository.countQuestionsWithFrontendFilters(
                    request.getSearchTerm(),
                    request.getSelectedYear(),
                    request.getSelectedStatus(),
                    request.getMemberId(),
                    request.getQuestionType(),
                    request.getSubDetailId()
            );
            
        } catch (Exception e) {
            log.error("프론트엔드 검색 결과 개수 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("프론트엔드 검색 결과 개수 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 문제은행 통계 정보 조회
     * @param memberId 멤버 ID (선택적)
     * @return 문제은행 통계
     */
    public QuestionListResponseDTO.QuestionBankStats getQuestionBankStats(String memberId) {
        log.info("문제은행 통계 정보 조회 요청: memberId = {}", memberId);
        
        try {
            // memberId가 null이거나 빈 문자열인 경우 전체 통계 반환
            if (memberId == null || memberId.trim().isEmpty()) {
                log.info("memberId가 null이거나 빈 문자열이므로 전체 통계를 반환합니다: memberId = {}", memberId);
                return getQuestionBankStats();
            }
            
            // member 정보 조회
            MemberInfoDTO memberInfo = memberService.getMemberInfo(memberId);
            if (memberInfo == null) {
                log.error("memberId {}에 해당하는 member 정보를 찾을 수 없습니다.", memberId);
                throw new RuntimeException("memberId에 해당하는 member 정보를 찾을 수 없습니다: " + memberId);
            }
            
            if (memberInfo.getEducationId() == null) {
                log.error("memberId {}의 educationId가 null입니다.", memberId);
                throw new RuntimeException("memberId의 educationId가 null입니다: " + memberId);
            }
            
            String educationId = memberInfo.getEducationId();
            log.info("memberId {}의 educationId: {}", memberId, educationId);

            // 데이터베이스 상태 확인 (디버깅용)
            List<String> allEducationIds = questionRepository.findAllEducationIds();
            log.info("데이터베이스에 존재하는 모든 educationId: {}", allEducationIds);
            
            for (String eduId : allEducationIds) {
                long count = questionRepository.countByEducationId(eduId);
                log.info("educationId {}의 문제 수: {}", eduId, count);
            }

            // 전체 문제 수 확인 (educationId 필터링 없이)
            long totalQuestionsWithoutFilter = questionRepository.count();
            log.info("전체 문제 수 (educationId 필터링 없이): {}", totalQuestionsWithoutFilter);
            
            // educationId로 필터링된 문제 수 확인
            long totalQuestions = questionRepository.countByQuestionActive(0, educationId);
            long activeQuestions = questionRepository.countByQuestionActive(0, educationId);
            long inactiveQuestions = questionRepository.countByQuestionActive(1, educationId);
            
            log.info("educationId {}로 필터링된 문제 수 - totalQuestions: {}, activeQuestions: {}, inactiveQuestions: {}", 
                    educationId, totalQuestions, activeQuestions, inactiveQuestions);
            
            // 유형별 문제 개수 조회
            long multipleChoiceCount = questionRepository.countByQuestionTypeAndQuestionActive("객관식", 0, educationId);
            long essayCount = questionRepository.countByQuestionTypeAndQuestionActive("서술형", 0, educationId);
            long codeCount = questionRepository.countByQuestionTypeAndQuestionActive("코드형", 0, educationId);
            
            log.info("유형별 문제 수 - multipleChoiceCount: {}, essayCount: {}, codeCount: {}", 
                    multipleChoiceCount, essayCount, codeCount);
            
            // educationId가 "1"인 경우 모든 문제를 포함하도록 수정
            if ("1".equals(educationId)) {
                log.info("educationId가 '1'이므로 모든 문제를 포함합니다.");
                totalQuestions = questionRepository.countByQuestionActiveWithNull(0, null);
                activeQuestions = questionRepository.countByQuestionActiveWithNull(0, null);
                inactiveQuestions = questionRepository.countByQuestionActiveWithNull(1, null);
                multipleChoiceCount = questionRepository.countByQuestionTypeAndQuestionActiveWithNull("객관식", 0, null);
                essayCount = questionRepository.countByQuestionTypeAndQuestionActiveWithNull("서술형", 0, null);
                codeCount = questionRepository.countByQuestionTypeAndQuestionActiveWithNull("코드형", 0, null);
                
                log.info("educationId '1' 적용 후 - totalQuestions: {}, activeQuestions: {}, inactiveQuestions: {}, multipleChoiceCount: {}, essayCount: {}, codeCount: {}", 
                        totalQuestions, activeQuestions, inactiveQuestions, multipleChoiceCount, essayCount, codeCount);
            }
            
            log.info("최종 통계 조회 결과 - totalQuestions: {}, activeQuestions: {}, inactiveQuestions: {}, multipleChoiceCount: {}, essayCount: {}, codeCount: {}", 
                    totalQuestions, activeQuestions, inactiveQuestions, multipleChoiceCount, essayCount, codeCount);
            
            return QuestionListResponseDTO.QuestionBankStats.builder()
                    .totalQuestions((int) totalQuestions)
                    .activeQuestions((int) activeQuestions)
                    .inactiveQuestions((int) inactiveQuestions)
                    .objectiveCount((int) multipleChoiceCount)
                    .descriptiveCount((int) essayCount)
                    .codeCount((int) codeCount)
                    .build();
            
        } catch (Exception e) {
            log.error("문제은행 통계 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제은행 통계 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 문제은행 통계 정보 조회 (기본값 사용)
     * @return 문제은행 통계
     */
    public QuestionListResponseDTO.QuestionBankStats getQuestionBankStats() {
        log.info("문제은행 통계 정보 조회 요청 (기본값 사용)");
        
        try {
            // 전체 문제 수 확인 (educationId 필터링 없이)
            long totalQuestionsWithoutFilter = questionRepository.count();
            log.info("전체 문제 수 (educationId 필터링 없이): {}", totalQuestionsWithoutFilter);
            
            // 전체 문제 수 확인 (questionActive = 0인 것만, educationId = null로 모든 문제 포함)
            long totalQuestions = questionRepository.countByQuestionActiveWithNull(0, null);
            long activeQuestions = questionRepository.countByQuestionActiveWithNull(0, null);
            long inactiveQuestions = questionRepository.countByQuestionActiveWithNull(1, null);
            
            log.info("전체 문제 수 - totalQuestions: {}, activeQuestions: {}, inactiveQuestions: {}", 
                    totalQuestions, activeQuestions, inactiveQuestions);
            
            // 유형별 문제 개수 조회 (educationId = null로 모든 문제 포함)
            long multipleChoiceCount = questionRepository.countByQuestionTypeAndQuestionActiveWithNull("객관식", 0, null);
            long essayCount = questionRepository.countByQuestionTypeAndQuestionActiveWithNull("서술형", 0, null);
            long codeCount = questionRepository.countByQuestionTypeAndQuestionActiveWithNull("코드형", 0, null);
            
            log.info("유형별 문제 수 - multipleChoiceCount: {}, essayCount: {}, codeCount: {}", 
                    multipleChoiceCount, essayCount, codeCount);
            
            log.info("최종 통계 조회 결과 - totalQuestions: {}, activeQuestions: {}, inactiveQuestions: {}, multipleChoiceCount: {}, essayCount: {}, codeCount: {}", 
                    totalQuestions, activeQuestions, inactiveQuestions, multipleChoiceCount, essayCount, codeCount);
            
            return QuestionListResponseDTO.QuestionBankStats.builder()
                    .totalQuestions((int) totalQuestions)
                    .activeQuestions((int) activeQuestions)
                    .inactiveQuestions((int) inactiveQuestions)
                    .objectiveCount((int) multipleChoiceCount)
                    .descriptiveCount((int) essayCount)
                    .codeCount((int) codeCount)
                    .build();
            
        } catch (Exception e) {
            log.error("문제은행 통계 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제은행 통계 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /*
     * 문제은행 응답 DTO 생성 메서드들 (사용하지 않음 - QuestionListResponseDTO로 대체됨)
     */
    /*
    private QuestionBankResponseDTO buildQuestionBankResponse(List<QuestionEntity> questions, String message, String memberId) {
        List<QuestionBankResponseDTO.QuestionInfo> questionInfos = questions.stream()
                .map(this::convertToQuestionInfo)
                .collect(Collectors.toList());
        
        return QuestionBankResponseDTO.builder()
                .questions(questionInfos)
                .totalCount(questionInfos.size())
                .message(message)
                .stats(getQuestionBankStats(memberId))
                .build();
    }
    
    private QuestionBankResponseDTO buildQuestionBankResponse(List<QuestionEntity> questions, String message) {
        return buildQuestionBankResponse(questions, message, null);
    }
    
    private QuestionBankResponseDTO buildQuestionBankResponseWithSubjectInfo(List<Object[]> questions, String message) {
        List<QuestionBankResponseDTO.QuestionInfo> questionInfos = questions.stream()
                .map(this::convertToQuestionInfoWithSubjectInfo)
                .collect(Collectors.toList());
        
        return QuestionBankResponseDTO.builder()
                .questions(questionInfos)
                .totalCount(questionInfos.size())
                .message(message)
                .stats(getQuestionBankStats())
                .build();
    }
    */
    
    /**
     * 문제 목록 응답 DTO 생성 (새로운 방식)
     * @param questions 문제 목록
     * @param page 현재 페이지
     * @param limit 페이지당 항목 수
     * @return 문제 목록 응답 DTO
     */
    private QuestionListResponseDTO buildQuestionListResponse(List<QuestionEntity> questions, int page, int limit) {
        List<QuestionListResponseDTO.QuestionSummaryDTO> questionSummaries = questions.stream()
                .map(this::convertToQuestionSummary)
                .collect(Collectors.toList());
        
        int totalCount = questionSummaries.size();
        int totalPages = (int) Math.ceil((double) totalCount / limit);
        boolean hasNext = page < totalPages;
        boolean hasPrevious = page > 1;
        
        return QuestionListResponseDTO.builder()
                .questions(questionSummaries)
                .totalCount(totalCount)
                .currentPage(page)
                .limit(limit)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
    
    /*
     * QuestionEntity를 QuestionInfo로 변환 (기존 방식) - 사용하지 않음
     * @param question 문제 엔티티
     * @return 문제 정보 DTO
     */
    /*
    private QuestionBankResponseDTO.QuestionInfo convertToQuestionInfo(QuestionEntity question) {
        // 보기 개수 조회
        List<QuestionOptEntity> options = questionOptRepository.findByQuestionId(question.getQuestionId());
        int optionCount = options.size();
        
        // 객관식 문제의 경우 보기 옵션들을 DTO로 변환
        List<QuestionBankResponseDTO.QuestionOption> optionDTOs = null;
        if ("객관식".equals(question.getQuestionType()) && !options.isEmpty()) {
            optionDTOs = options.stream()
                    .map(opt -> QuestionBankResponseDTO.QuestionOption.builder()
                            .optId(opt.getOptId())
                            .optText(opt.getOptText())
                            .optIsCorrect(opt.getOptIsCorrect())
                            .build())
                    .collect(Collectors.toList());
        }
        
        // 세부과목 이름 조회
        String subDetailName = "";
        if (question.getSubDetailId() != null) {
            try {
                var subDetail = subDetailRepository.findById(question.getSubDetailId());
                if (subDetail.isPresent()) {
                    subDetailName = subDetail.get().getSubDetailName();
                }
            } catch (Exception e) {
                log.warn("세부과목 정보 조회 실패: subDetailId = {}, error = {}", question.getSubDetailId(), e.getMessage());
            }
        }
        
        // 강사 정보 조회
        String memberName = "";
        String memberEmail = "";
        String userId = "";
        if (question.getMemberId() != null && !question.getMemberId().trim().isEmpty()) {
            try {
                log.info("강사 정보 조회 시도: memberId = {}", question.getMemberId());
                
                // 1. 먼저 id 컬럼으로 조회 시도 (member 테이블의 id 컬럼)
                MemberInfoDTO memberInfo = memberService.getInstructorByQuestionId(question.getMemberId());
                
                // 2. id로 조회 실패시 memberId 컬럼으로도 시도
                if (memberInfo == null) {
                    log.info("id 컬럼으로 조회 실패, memberId 컬럼으로 조회 시도: {}", question.getMemberId());
                    memberInfo = memberService.getMemberInfoByMemberId(question.getMemberId());
                }
                
                if (memberInfo != null) {
                    memberName = memberInfo.getMemberName() != null ? memberInfo.getMemberName() : "";
                    memberEmail = memberInfo.getMemberEmail() != null ? memberInfo.getMemberEmail() : "";
                    // userId는 question.getMemberId()를 직접 사용 (MemberEntity의 id 필드와 매칭)
                    userId = question.getMemberId();
                    log.info("강사 정보 조회 성공: memberId = {}, memberName = {}, memberEmail = {}, userId = {}", 
                            question.getMemberId(), memberName, memberEmail, userId);
                } else {
                    log.warn("강사 정보 조회 결과가 null: memberId = {}", question.getMemberId());
                    // 강사 정보를 찾을 수 없어도 userId는 설정
                    userId = question.getMemberId();
                }
            } catch (Exception e) {
                log.warn("강사 정보 조회 실패: memberId = {}, error = {}", question.getMemberId(), e.getMessage());
                // 오류가 발생해도 userId는 설정
                userId = question.getMemberId();
            }
        }
        
        // 미리보기 텍스트 생성 (문제 내용의 일부만 표시)
        String previewText = question.getQuestionText();
        if (previewText != null && previewText.length() > 100) {
            previewText = previewText.substring(0, 100) + "...";
        }
        
        return QuestionBankResponseDTO.QuestionInfo.builder()
                .questionId(question.getQuestionId())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionAnswer(question.getQuestionAnswer())
                .explanation(question.getExplanation())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt() != null ? question.getUpdatedAt() : null)
                .memberId(question.getMemberId())
                .memberName(memberName)
                .memberEmail(memberEmail)
                .userId(userId)
                .subDetailId(question.getSubDetailId())
                .subDetailName(subDetailName)
                .educationId(question.getEducationId())
                .status(question.getQuestionActive() == 0 ? "활성" : "비활성")
                .optionCount(optionCount)
                .previewText(previewText)
                .subjectId("") // 기본값 설정  -- 삭제 해도 되지 않을까 싶긴 한데.
                .subjectName("") // 기본값 설정
                .subjectInfo("") // 기본값 설정
                .options(optionDTOs) // 보기 옵션 배열 추가
                .build();
    }
    */
    
    /**
     * QuestionEntity를 QuestionSummary로 변환 (새로운 방식)
     * @param question 문제 엔티티
     * @return 문제 요약 DTO
     */
    private QuestionListResponseDTO.QuestionSummaryDTO convertToQuestionSummary(QuestionEntity question) {
        // 보기 개수 조회
        List<QuestionOptEntity> options = questionOptRepository.findByQuestionId(question.getQuestionId());
        int optionCount = options.size();
        
        // 객관식 문제의 경우 보기 옵션들을 DTO로 변환
        List<QuestionListResponseDTO.QuestionOption> optionDTOs = null;
        if ("객관식".equals(question.getQuestionType()) && !options.isEmpty()) {
            optionDTOs = options.stream()
                    .map(opt -> QuestionListResponseDTO.QuestionOption.builder()
                            .optId(opt.getOptId())
                            .optText(opt.getOptText())
                            .optIsCorrect(opt.getOptIsCorrect())
                            .build())
                    .collect(Collectors.toList());
        }
        
        // 세부과목 이름 조회
        String subDetailName = "";
        if (question.getSubDetailId() != null) {
            try {
                var subDetail = subDetailRepository.findById(question.getSubDetailId());
                if (subDetail.isPresent()) {
                    subDetailName = subDetail.get().getSubDetailName();
                }
            } catch (Exception e) {
                log.warn("세부과목 정보 조회 실패: subDetailId = {}, error = {}", question.getSubDetailId(), e.getMessage());
            }
        }
        
        // 미리보기 텍스트 생성 (문제 내용의 일부만 표시)
        String previewText = question.getQuestionText();
        if (previewText != null && previewText.length() > 100) {
            previewText = previewText.substring(0, 100) + "...";
        }
        
        return QuestionListResponseDTO.QuestionSummaryDTO.builder()
                .questionId(question.getQuestionId())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionAnswer(question.getQuestionAnswer())
                .explanation(question.getExplanation())
                .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                .updatedAt(TimeZoneUtil.toKoreanTime(question.getUpdatedAt()))
                .memberId(question.getMemberId())
                .subDetailId(question.getSubDetailId())
                .subDetailName(subDetailName)
                .educationId(question.getEducationId())
                .questionActive(question.getQuestionActive())
                .optionCount(optionCount)
                .previewText(previewText)
                .options(optionDTOs)
                .build();
    }
    
    /**
     * QuestionEntity를 QuestionDetail로 변환 (새로운 방식)
     * @param question 문제 엔티티
     * @return 문제 상세 DTO
     */
    private QuestionDTO convertToQuestionDetail(QuestionEntity question) {
        // 보기 개수 조회
        List<QuestionOptEntity> options = questionOptRepository.findByQuestionId(question.getQuestionId());
        
        // 선택지 정보 변환
        List<QuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> QuestionOptionDTO.builder()
                        .optId(option.getOptId())
                        .optText(option.getOptText())
                        .optIsCorrect(option.getOptIsCorrect())
                        .build())
                .collect(Collectors.toList());
        
        // 선택지 텍스트만 추출
        List<String> optionTexts = options.stream()
                .map(QuestionOptEntity::getOptText)
                .collect(Collectors.toList());
        
        // 세부과목 이름 조회
        String subDetailName = "";
        if (question.getSubDetailId() != null) {
            try {
                var subDetail = subDetailRepository.findById(question.getSubDetailId());
                if (subDetail.isPresent()) {
                    subDetailName = subDetail.get().getSubDetailName();
                }
            } catch (Exception e) {
                log.warn("세부과목 정보 조회 실패: subDetailId = {}, error = {}", question.getSubDetailId(), e.getMessage());
            }
        }
        
        return QuestionDTO.builder()
                .questionId(question.getQuestionId())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionAnswer(question.getQuestionAnswer())
                .explanation(question.getExplanation())
                .codeLanguage(question.getCodeLanguage())
                .options(optionDTOs)
                .instructorId(question.getMemberId())
                .subDetailId(question.getSubDetailId())
                .subDetailName(subDetailName)
                .educationId(question.getEducationId())
                .questionActive(String.valueOf(question.getQuestionActive()))
                .createdAt(TimeZoneUtil.toKoreanTime(question.getCreatedAt()))
                .updatedAt(TimeZoneUtil.toKoreanTime(question.getUpdatedAt()))
                .build();
    }
    
    /*
     * Object[]를 QuestionInfo로 변환 (상위 과목 정보 포함) - 사용하지 않음
     * @param result 조인 결과 배열
     * @return 문제 정보 DTO
     */
    /*
    private QuestionBankResponseDTO.QuestionInfo convertToQuestionInfoWithSubjectInfo(Object[] result) {
        QuestionEntity question = (QuestionEntity) result[0];
        String subDetailName = (String) result[1];
        String subjectId = (String) result[2];
        String subjectName = (String) result[3];
        String subjectInfo = (String) result[4];
        
        // 보기 개수 조회
        List<QuestionOptEntity> options = questionOptRepository.findByQuestionId(question.getQuestionId());
        int optionCount = options.size();
        
        // 미리보기 텍스트 생성 (문제 내용의 일부만 표시)
        String previewText = question.getQuestionText();
        if (previewText != null && previewText.length() > 100) {
            previewText = previewText.substring(0, 100) + "...";
        }
        
        return QuestionBankResponseDTO.QuestionInfo.builder()
                .questionId(question.getQuestionId())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionAnswer(question.getQuestionAnswer())
                .explanation(question.getExplanation())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt() != null ? question.getUpdatedAt() : null)
                .memberId(question.getMemberId())
                .subDetailId(question.getSubDetailId())
                .subDetailName(subDetailName)
                .educationId(question.getEducationId())
                .status(question.getQuestionActive() == 0 ? "활성" : "비활성")
                .optionCount(optionCount)
                .previewText(previewText)
                .subjectId(subjectId)
                .subjectName(subjectName)
                .subjectInfo(subjectInfo)
                .build();
    }
    */

    // ==================== 문제 CRUD 메서드 ====================
    
    /**
     * 새 문제 생성 (새로운 요구사항에 맞게 수정)
     * @param instructorId 강사 ID
     * @param request 문제 생성 요청 DTO
     * @return 생성된 문제 정보
     */
    @Transactional
    public QuestionDTO createQuestion(String instructorId, QuestionCreateRequestDTO request) {
        log.info("새 문제 생성 요청: instructorId = {}, request = {}", instructorId, request);
        
        try {
            // 1. 유효성 검사
            validateQuestionCreateRequest(request);
            
            // 2. 세부과목명으로 세부과목 ID 조회
            String subDetailId = getSubDetailIdByName(request.getSubDetailName());
            
            // 3. 강사 정보 조회 (userId로 조회)
            MemberInfoDTO memberInfo = memberService.getMemberInfo(instructorId);
            if (memberInfo == null) {
                throw new IllegalArgumentException("강사 정보를 찾을 수 없습니다: " + instructorId);
            }
            
            String memberId = memberInfo.getMemberId();
            // TODO: 나중에 memberInfo.getEducationId()로 변경 예정
            String educationId =  memberInfo.getEducationId(); // 현재는 고정값 사용
            
            log.info("문제 생성 정보: memberId = {}, educationId = {}, questionType = {}, subDetailId = {}", 
                    memberId, educationId, request.getQuestionType(), subDetailId);
            
            // 4. 문제 엔티티 생성
            QuestionEntity question = QuestionEntity.builder()
                    .questionType(request.getQuestionType())
                    .questionText(request.getQuestionText())
                    .questionAnswer(request.getQuestionAnswer())
                    .explanation(request.getExplanation() != null ? request.getExplanation() : "")
                    .subDetailId(subDetailId)
                    .educationId(educationId)
                    .memberId(memberId)
                    .questionActive(0) // 활성 상태로 생성ㅊ 
                    .codeLanguage(request.getCodeLanguage())
                    .build();
            
            // 5. 객관식 문제의 경우 선택지 엔티티 생성 및 저장
            if ("객관식".equals(request.getQuestionType()) && request.getOptions() != null) {
                log.info("객관식 문제 선택지 처리 시작: {}개 선택지", request.getOptions().size());
                
                // 선택지 개수 검증 (4개 고정)
                if (request.getOptions().size() != 4) {
                    throw new IllegalArgumentException("객관식 문제는 정확히 4개의 선택지가 필요합니다. 현재: " + request.getOptions().size() + "개");
                }
                
                // 정답 일치성 검증
                String correctOptionText = null;
                for (QuestionOptionDTO optionDTO : request.getOptions()) {
                    if (optionDTO.getOptIsCorrect() != null && optionDTO.getOptIsCorrect() == 1) {
                        correctOptionText = optionDTO.getOptText();
                        break;
                    }
                }
                
                if (correctOptionText == null) {
                    throw new IllegalArgumentException("객관식 문제에는 정답 선택지가 하나 있어야 합니다.");
                }
                
                if (!correctOptionText.equals(request.getQuestionAnswer())) {
                    log.warn("정답 불일치: questionAnswer='{}', correctOptionText='{}'", 
                            request.getQuestionAnswer(), correctOptionText);
                    // questionAnswer를 정답 선택지로 업데이트
                    question.setQuestionAnswer(correctOptionText);
                }
                
                for (QuestionOptionDTO optionDTO : request.getOptions()) {
                    QuestionOptEntity option = QuestionOptEntity.builder()
                            .question(question)  // QuestionEntity 객체 설정
                            .optText(optionDTO.getOptText())
                            .optIsCorrect(optionDTO.getOptIsCorrect())
                            .build();
                    
                    // 선택지 엔티티 저장
                    QuestionOptEntity savedOption = questionOptRepository.save(option);
                    log.info("선택지 저장 완료: optText={}, optIsCorrect={}", 
                            savedOption.getOptText(), savedOption.getOptIsCorrect());
                }
                
                log.info("객관식 문제 선택지 처리 완료: {}개 선택지 저장됨", request.getOptions().size());
            }
            
            // 6. 문제와 선택지를 한 번에 저장 (cascade = ALL로 인해 선택지도 자동 저장)
            QuestionEntity savedQuestion = questionRepository.save(question);
            log.info("문제 및 선택지 저장 완료: questionId = {}", savedQuestion.getQuestionId());
            
            return convertToQuestionDetail(savedQuestion);
            
        } catch (Exception e) {
            log.error("문제 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 세부과목명으로 세부과목 ID 조회
     * @param subDetailName 세부과목명
     * @return 세부과목 ID
     */
    private String getSubDetailIdByName(String subDetailName) {
        List<SubjectDetailEntity> subDetails = subDetailRepository.findBySubDetailName(subDetailName);
        if (subDetails.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 세부과목명입니다: " + subDetailName);
        }
        return subDetails.get(0).getSubDetailId();
    }
    
    /**
     * 문제 생성 요청 유효성 검사
     * @param request 문제 생성 요청
     */
    private void validateQuestionCreateRequest(QuestionCreateRequestDTO request) {
        questionValidator.validateQuestionCreateRequest(request);
    }

    /**
     * 다중 문제 생성
     * @param instructorId 강사 ID
     * @param request 다중 문제 생성 요청
     * @return 다중 문제 생성 응답
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionBatchCreateResponseDTO createQuestionsBatch(String instructorId, QuestionBatchCreateRequestDTO request) {
        log.info("다중 문제 생성 요청: instructorId = {}, questionCount = {}", instructorId, request.getQuestions().size());
        
        List<QuestionBatchCreateResponseDTO.CreatedQuestionDTO> createdQuestions = new ArrayList<>();
        
        try {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                QuestionCreateRequestDTO questionRequest = request.getQuestions().get(i);
                log.info("문제 {} 생성 시작: {}", i + 1, questionRequest.getQuestionText());
                
                // 각 문제에 instructorId 설정
                questionRequest.setInstructorId(instructorId);
                
                // 단일 문제 생성
                QuestionDTO createdQuestion = createQuestion(instructorId, questionRequest);
                
                // 생성된 문제 정보를 응답에 추가
                QuestionBatchCreateResponseDTO.CreatedQuestionDTO createdQuestionDTO = 
                    QuestionBatchCreateResponseDTO.CreatedQuestionDTO.builder()
                        .questionId(createdQuestion.getQuestionId())
                        .questionText(createdQuestion.getQuestionText())
                        .build();
                
                createdQuestions.add(createdQuestionDTO);
                log.info("문제 {} 생성 완료: questionId = {}", i + 1, createdQuestion.getQuestionId());
            }
            
            log.info("다중 문제 생성 완료: {}개", createdQuestions.size());
            return QuestionBatchCreateResponseDTO.builder()
                    .createdQuestions(createdQuestions)
                    .build();
                    
        } catch (Exception e) {
            log.error("다중 문제 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("다중 문제 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 문제 삭제 (논리적 삭제)
     * @param questionId 문제 ID
     */
    @Transactional
    public void deleteQuestion(String questionId) {
        log.info("문제 삭제 요청: questionId = {}", questionId);
        
        try {
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + questionId));
            
            // 논리적 삭제 (questionActive를 1로 설정)
            question.setQuestionActive(1);
            questionRepository.save(question);
            
            log.info("문제 삭제 완료: questionId = {}", questionId);
            
        } catch (Exception e) {
            log.error("문제 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== 추가 통계 메서드 ====================
    
    /**
     * 선생별 문제 통계 조회
     * @return 선생별 문제 통계
     */
    public Map<String, Object> getInstructorStats() {
        log.info("선생별 문제 통계 조회 요청");
        
        try {
            List<Object[]> instructorStats = questionRepository.findInstructorStats();
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalInstructors", instructorStats.size());
            result.put("instructors", instructorStats.stream()
                    .map(this::convertToInstructorInfo)
                    .collect(Collectors.toList()));
            
            return result;
            
        } catch (Exception e) {
            log.error("선생별 문제 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("선생별 문제 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 과목별 문제 통계 조회
     * @return 과목별 문제 통계
     */
    public Map<String, Object> getSubjectStats() {
        log.info("과목별 문제 통계 조회 요청");
        
        try {
            List<Object[]> subjectStats = questionRepository.findSubjectStats();
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalSubjects", subjectStats.size());
            result.put("subjects", subjectStats.stream()
                    .map(this::convertToSubjectInfo)
                    .collect(Collectors.toList()));
            
            return result;
            
        } catch (Exception e) {
            log.error("과목별 문제 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("과목별 문제 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 문제 유형별 통계 조회
     * @return 문제 유형별 통계
     */
    public Map<String, Integer> getQuestionTypeStats() {
        log.info("문제 유형별 통계 조회 요청");
        
        try {
            List<Object[]> typeStats = questionRepository.findQuestionTypeStats();
            
            return typeStats.stream()
                    .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> ((Long) result[1]).intValue()
                    ));
            
        } catch (Exception e) {
            log.error("문제 유형별 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 유형별 통계 조회 중 오류가 발생했습니다.", e);
        }
    }


    /**
     * 과목별 문제 수 통계 조회 (실제 데이터 기반)
     * @return 과목별 문제 수
     */
    public Map<String, Integer> getQuestionsBySubjectStats() {
        log.info("과목별 문제 수 통계 조회 요청");
        
        try {
            List<Object[]> subjectStats = questionRepository.findQuestionsBySubjectStats();
            
            return subjectStats.stream()
                    .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> ((Long) result[1]).intValue()
                    ));
            
        } catch (Exception e) {
            log.error("과목별 문제 수 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 빈 맵 반환
            return new HashMap<>();
        }
    }

    /**
     * 선생별 문제 수 통계 조회 (실제 데이터 기반)
     * @return 선생별 문제 수
     */
    public Map<String, Integer> getQuestionsByInstructorStats() {
        log.info("선생별 문제 수 통계 조회 요청");
        
        try {
            List<Object[]> instructorStats = questionRepository.findQuestionsByInstructorStats();
            
            return instructorStats.stream()
                    .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> ((Long) result[1]).intValue()
                    ));
            
        } catch (Exception e) {
            log.error("선생별 문제 수 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 빈 맵 반환
            return new HashMap<>();
        }
    }

    // ==================== 추가 변환 메서드 ====================
    


    // ==================== 문제 상세 관리 메서드 ====================
    
    /**
     * 문제 상세 조회
     * @param questionId 문제 ID
     * @return 문제 상세 정보
     */
    public QuestionDTO getQuestionDetail(String questionId) {
        log.info("문제 상세 조회 요청: questionId = {}", questionId);
        
        try {
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + questionId));
            
            return convertToQuestionDetail(question);
            
        } catch (Exception e) {
            log.error("문제 상세 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 상세 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 문제 상세 정보 조회 (새로운 응답 형식)
     * @param questionId 문제 ID
     * @return 문제 상세 정보 (새로운 DTO 형식)
     */
    public QuestionDTO getQuestionDetailNew(String questionId) {
        log.info("문제 상세 정보 조회 요청 (새로운 형식): questionId = {}", questionId);
        
        try {
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다: " + questionId));
            
            return convertToQuestionDetail(question);
            
        } catch (Exception e) {
            log.error("문제 상세 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 상세 정보 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 문제 수정
     * @param questionId 문제 ID
     * @param request 수정 요청 DTO
     * @return 수정된 문제 정보
     */
    @Transactional
    public QuestionDTO updateQuestion(String questionId, QuestionUpdateRequestDTO request) {
        log.info("문제 수정 요청: questionId = {}, request = {}", questionId, request);
        
        try {
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + questionId));
            
            // 문제 정보 업데이트 (프론트엔드 요구사항에 맞춰)
            question.setQuestionType(request.getQuestionTypeForUpdate());
            question.setQuestionText(request.getQuestionTextForUpdate());
            question.setQuestionAnswer(request.getQuestionAnswerForUpdate());
            
            // 기존 필드들 (하위 호환성)
            if (request.getExplanation() != null) {
                question.setExplanation(request.getExplanation());
            }
            if (request.getSubDetailId() != null) {
                question.setSubDetailId(request.getSubDetailId());
            }
            
            // 코드 언어 필드 (코드형 문제)
            if (request.getCodeLanguage() != null) {
                question.setCodeLanguage(request.getCodeLanguage());
            }
            
            question.setUpdatedAt(Instant.now());
            
            questionRepository.save(question);
            
            // 객관식 문제의 경우 보기 옵션 업데이트
            if ("객관식".equals(request.getQuestionTypeForUpdate())) {
                // 기존 보기 옵션 삭제
                questionOptRepository.deleteByQuestionId(questionId);
                
                // 새로운 보기 옵션 추가
                if (request.getQuestionOptions() != null && !request.getQuestionOptions().isEmpty()) {
                    // QuestionOptionDTO 형태로 온 경우
                    
                    // 선택지 개수 검증 (4개 고정)
                    if (request.getQuestionOptions().size() != 4) {
                        throw new IllegalArgumentException("객관식 문제는 정확히 4개의 선택지가 필요합니다. 현재: " + request.getQuestionOptions().size() + "개");
                    }
                    
                    // 정답 개수 검증
                    long correctCount = request.getQuestionOptions().stream()
                            .filter(option -> option.getOptIsCorrect() == 1)
                            .count();
                    
                    if (correctCount != 1) {
                        throw new IllegalArgumentException("객관식 문제는 정답이 하나만 있어야 합니다. 현재: " + correctCount + "개");
                    }
                    
                    for (QuestionOptionDTO optionDTO : request.getQuestionOptions()) {
                        QuestionOptEntity option = new QuestionOptEntity();
                        option.setOptText(optionDTO.getOptText());
                        option.setOptIsCorrect(optionDTO.getOptIsCorrect());
                        option.setQuestion(question);
                        
                        questionOptRepository.save(option);
                    }
                    log.info("객관식 문제 보기 옵션 업데이트 완료 (QuestionOptionDTO): {}개 옵션", request.getQuestionOptions().size());
                } else if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                    // 기존 String 리스트 형태로 온 경우 (하위 호환성)
                    
                    // 선택지 개수 검증 (4개 고정)
                    if (request.getOptions().size() != 4) {
                        throw new IllegalArgumentException("객관식 문제는 정확히 4개의 선택지가 필요합니다. 현재: " + request.getOptions().size() + "개");
                    }
                    
                for (int i = 0; i < request.getOptions().size(); i++) {
                    String optionText = request.getOptions().get(i);
                    boolean isCorrect = optionText.equals(request.getQuestionAnswerForUpdate());
                    
                    QuestionOptEntity option = new QuestionOptEntity();
                    option.setOptText(optionText);
                    option.setOptIsCorrect(isCorrect ? 1 : 0);
                    option.setQuestion(question);
                    
                    questionOptRepository.save(option);
                }
                    log.info("객관식 문제 보기 옵션 업데이트 완료 (String 리스트): {}개 옵션", request.getOptions().size());
                }
            }
            
            return convertToQuestionDetail(question);
            
        } catch (Exception e) {
            log.error("문제 수정 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 문제 상태 업데이트 (논리적 삭제/복원)
     * @param questionId 문제 ID
     * @param questionActive 상태 (0: 활성, 1: 비활성)
     */
    @Transactional
    public void updateQuestionStatus(String questionId, int questionActive) {
        log.info("문제 상태 업데이트 요청: questionId = {}, questionActive = {}", questionId, questionActive);
        
        try {
            QuestionEntity question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + questionId));
            
            question.setQuestionActive(questionActive);
            question.setUpdatedAt(Instant.now());
            
            questionRepository.save(question);
            
            log.info("문제 상태 업데이트 완료: questionId = {}, questionActive = {}", questionId, questionActive);
            
        } catch (Exception e) {
            log.error("문제 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 상태 업데이트 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 특정 유형의 문제 개수 조회
     * @param questionType 문제 유형
     * @return 해당 유형의 문제 개수
     */
    public int getQuestionTypeCount(String questionType) {
        log.info("문제 유형별 개수 조회 요청: questionType = {}", questionType);
        
        try {
            String id = "instructor";
            MemberInfoDTO memberInfo = memberService.getMemberInfo(id);
            String educationId = memberInfo.getEducationId();
            log.info("educationId: {}", educationId);
            
            return (int) questionRepository.countByQuestionTypeAndQuestionActive(questionType, 0, educationId);
            
        } catch (Exception e) {
            log.error("문제 유형별 개수 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("문제 유형별 개수 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== 통계 및 메타데이터 메서드 ====================
    
    /**
     * 선생 목록 조회 (문제은행에 참여한 선생들)
     * @return 선생 목록
     */
    public List<MemberInfoDTO> getInstructors() {
        log.info("선생 목록 조회 요청");
        
        try {
            List<Object[]> instructorStats = questionRepository.findInstructorStats();
            
            return instructorStats.stream()
                    .map(this::convertToInstructorInfo)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("선생 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("선생 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 과목 목록 조회 (문제가 있는 과목들)
     * @return 과목 목록
     */
    public List<SubjectInfoDTO> getSubjects() {
        log.info("과목 목록 조회 요청");
        
        try {
            List<Object[]> subjectStats = questionRepository.findSubjectStats();
            
            return subjectStats.stream()
                    .map(this::convertToSubjectInfo)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("과목 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("과목 목록 조회 중 오류가 발생했습니다.", e);
        }
    }


    /**
     * 문제은행 대시보드 통계 조회
     * @return 대시보드 통계 정보
     */
    public Map<String, Object> getDashboardStats() {
        log.info("문제은행 대시보드 통계 조회 요청");
        
        try {
            String id = "instructor";
            MemberInfoDTO memberInfo = memberService.getMemberInfo(id);
            String educationId = memberInfo.getEducationId();
            log.info("educationId: {}", educationId);
            
            // 기본 통계
            long totalQuestions = questionRepository.countByQuestionActive(0, educationId);
            long activeQuestions = questionRepository.countByQuestionActive(0, educationId);
            
            // 선생 수 조회
            List<Object[]> instructorStats = questionRepository.findInstructorStats();
            int totalInstructors = instructorStats.size();
            
            // 과목 수 조회
            List<Object[]> subjectStats = questionRepository.findSubjectStats();
            int totalSubjects = subjectStats.size();
        
            
            // 문제 유형별 통계
            Map<String, Integer> questionsByType = getQuestionTypeStats();
            
            // 과목별 통계 (실제 데이터 기반)
            Map<String, Integer> questionsBySubject = getQuestionsBySubjectStats();
            
            // 선생별 통계 (실제 데이터 기반)
            Map<String, Integer> questionsByInstructor = getQuestionsByInstructorStats();
            
            Map<String, Object> dashboardStats = new HashMap<>();
            dashboardStats.put("totalQuestions", (int) totalQuestions);
            dashboardStats.put("activeQuestions", (int) activeQuestions);
            dashboardStats.put("totalInstructors", totalInstructors);
            dashboardStats.put("totalSubjects", totalSubjects);
            dashboardStats.put("questionsByType", questionsByType);
            dashboardStats.put("questionsBySubject", questionsBySubject);
            dashboardStats.put("questionsByInstructor", questionsByInstructor);
            
            return dashboardStats;
            
        } catch (Exception e) {
            log.error("대시보드 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("대시보드 통계 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== 변환 메서드 ====================
    
    /**
     * Object[]를 MemberInfoDTO로 변환 (강사 정보)
     * @param result 조인 결과 배열
     * @return 강사 정보 DTO
     */
    private MemberInfoDTO convertToInstructorInfo(Object[] result) {
        String memberId = (String) result[0];
        String memberName = (String) result[1];
        String memberEmail = (String) result[2];
        Long questionCount = (Long) result[3];
        String department = (String) result[4];
        String lastActiveDate = (String) result[5];
        
        return MemberInfoDTO.builder()
                .memberId(memberId)
                .memberName(memberName)
                .memberEmail(memberEmail)
                .questionCount(questionCount != null ? questionCount.intValue() : 0)
                .department(department)
                .profileImage(null) // 실제로는 프로필 이미지 URL을 가져와야 함
                .lastActiveDate(lastActiveDate)
                .memberRole("INSTRUCTOR") // 강사 역할 설정
                .build();
    }

    /**
     * Object[]를 SubjectInfoDTO로 변환
     * @param result 조인 결과 배열
     * @return 과목 정보 DTO
     */
    private SubjectInfoDTO convertToSubjectInfo(Object[] result) {
        String subjectId = (String) result[0];
        String subjectName = (String) result[1];
        String subjectInfo = (String) result[2];
        String subDetailId = (String) result[3];
        String subDetailName = (String) result[4];
        Long questionCount = (Long) result[5];
        String educationId = (String) result[6];
        String educationName = (String) result[7];
        
        return SubjectInfoDTO.builder()
                .subjectId(subjectId)
                .subjectName(subjectName)
                .subjectInfo(subjectInfo)
                .subDetailId(subDetailId)
                .subDetailName(subDetailName)
                .questionCount(questionCount != null ? questionCount.intValue() : 0)
                .educationId(educationId)
                .educationName(educationName)
                .build();
    }
}