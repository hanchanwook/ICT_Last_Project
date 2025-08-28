package com.jakdang.labs.api.common;

import com.jakdang.labs.api.chanwook.repository.InstructorMemberRepository;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.EducationEntity;
import com.jakdang.labs.api.jaegyeom.registration.repository.EduRegisterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EducationId {
    
    private final InstructorMemberRepository instructorMemberRepository;
    private final EduRegisterRepository eduRegisterRepository;
    
    /**
     * 사용자 ID로부터 educationId를 조회
     * @param userId 사용자 ID (users 테이블의 id = member 테이블의 id)
     * @return educationId (Optional)
     */
    public Optional<String> getEducationIdByUserId(String userId) {
        try {
            log.info("EducationId 조회 시작: userId={}", userId);
            
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("userId가 null이거나 비어있음");
                return Optional.empty();
            }
            
            // 1. MemberEntity에서 id 필드로 조회 (users 테이블의 id = member 테이블의 id)
            log.info("Member 조회 시도: id={}", userId);
            List<MemberEntity> members = instructorMemberRepository.findByIdField(userId);
            
            if (members == null) {
                log.warn("Member 조회 결과가 null: userId={}", userId);
                return Optional.empty();
            }
            
            MemberEntity member = members.isEmpty() ? null : members.get(0);
            
            if (member == null) {
                log.warn("Member를 찾을 수 없음: userId={}", userId);
                return Optional.empty();
            }
            
            log.info("Member 조회 성공: id={}, educationId={}", member.getId(), member.getEducationId());
            
            // 2. member의 educationId가 있으면 반환
            if (member.getEducationId() != null && !member.getEducationId().isEmpty()) {
                log.info("EducationId 조회 성공 (Member에서): userId={}, educationId={}", userId, member.getEducationId());
                return Optional.of(member.getEducationId());
            }
            
            // 3. educationId가 없으면 매핑 로직으로 조회
            String mappedEducationId = getEducationIdByMapping(userId);
            if (mappedEducationId != null) {
                log.info("EducationId 조회 성공 (매핑): userId={}, educationId={}", userId, mappedEducationId);
                return Optional.of(mappedEducationId);
            }
            
            log.warn("EducationId를 찾을 수 없음: userId={}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("EducationId 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 매핑 로직으로 educationId 조회
     * @param userId 사용자 ID
     * @return educationId 또는 null
     */
    private String getEducationIdByMapping(String userId) {
        try {
            // 여기에 매핑 로직 구현
            // 예: 특정 사용자 ID에 대한 educationId 매핑
            Map<String, String> educationIdMapping = new HashMap<>();
            educationIdMapping.put("580aaa70-7d1f-4799-bcef-52ff282ee482", "12345"); // 예시 매핑
            educationIdMapping.put("26c6c05e-0813-4920-a488-5a1eba3adfae", "67890"); // 예시 매핑
            
            String mappedEducationId = educationIdMapping.get(userId);
            if (mappedEducationId != null) {
                log.debug("매핑에서 EducationId 찾음: userId={}, educationId={}", userId, mappedEducationId);
                return mappedEducationId;
            }
            
            log.debug("매핑에서 EducationId를 찾을 수 없음: userId={}", userId);
            return null;
        } catch (Exception e) {
            log.error("매핑 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 사용자 ID로부터 educationId를 조회 (기본값 반환)
     * @param userId 사용자 ID
     * @param defaultValue 기본값
     * @return educationId 또는 기본값
     */
    public String getEducationIdByUserId(String userId, String defaultValue) {
        return getEducationIdByUserId(userId).orElse(defaultValue);
    }
    
    /**
     * 사용자 ID로부터 educationId를 조회 (null 반환)
     * @param userId 사용자 ID
     * @return educationId 또는 null
     */
    public String getEducationIdByUserIdOrNull(String userId) {
        return getEducationIdByUserId(userId).orElse(null);
    }

    public Optional<ResponseGetEducationDTO> findById(String id) {
        Optional<MemberEntity> member = instructorMemberRepository.findByIdColumn(id); // id 컬럼으로 조회
        if (member.isPresent() && member.get().getEducationId() != null) {
            return Optional.of(new ResponseGetEducationDTO(member.get().getEducationId(), member.get().getId()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<ResponseGetEducationDTO> findByEmail(String email) {
        try {
            log.info("이메일로 EducationId 조회 시작: email={}", email);
            
            // 이메일로 멤버 조회
            Optional<MemberEntity> member = instructorMemberRepository.findByMemberEmail(email);
            
            if (member.isPresent() && member.get().getEducationId() != null) {
                log.info("이메일로 EducationId 조회 성공: email={}, educationId={}, userId={}", 
                    email, member.get().getEducationId(), member.get().getId());
                return Optional.of(new ResponseGetEducationDTO(member.get().getEducationId(), member.get().getId()));
            } else {
                log.warn("이메일에 해당하는 멤버를 찾을 수 없음: email={}", email);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("이메일로 EducationId 조회 중 오류 발생: email={}, error={}", email, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * educationId로부터 educationName을 조회
     * @param educationId 교육기관 ID
     * @return educationName 또는 null
     */
    public String getEducationNameByEducationId(String educationId) {
        try {
            if (educationId == null || educationId.isEmpty()) {
                log.warn("educationId가 null이거나 비어있음");
                return null;
            }
            
            EducationEntity education = eduRegisterRepository.findByEducationId(educationId);
            if (education != null) {
                log.debug("EducationName 조회 성공: educationId={}, educationName={}", educationId, education.getEducationName());
                return education.getEducationName();
            } else {
                log.warn("Education을 찾을 수 없음: educationId={}", educationId);
                return null;
            }
        } catch (Exception e) {
            log.error("EducationName 조회 중 오류 발생: educationId={}, error={}", educationId, e.getMessage());
            return null;
        }
    }

} 