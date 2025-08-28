package com.jakdang.labs.api.jaegyeom.registration.service;

import com.jakdang.labs.api.jaegyeom.registration.dto.EduRegisterDto;
import com.jakdang.labs.api.jaegyeom.registration.dto.FindEduAndMemberDto;
import com.jakdang.labs.entity.EducationEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.jaegyeom.registration.repository.EduRegisterRepository;
import com.jakdang.labs.api.jaegyeom.registration.repository.PeopleRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class EducationRegisterService {

    private final EduRegisterRepository educationRepository;
    private final PeopleRegisterRepository peopleRegisterRepository;
    
    @Transactional
    public EducationEntity registerEducation(EduRegisterDto dto) {
        EducationEntity education = new EducationEntity();
        education.setEducationName(dto.getEducation().getEducationName());
        education.setBusinessNumber(dto.getEducation().getBusinessNumber());
        education.setEducationAddress(dto.getEducation().getEducationAddress());
        education.setDescription(dto.getEducation().getDescription());
        education.setEducationDetailAddress(dto.getEducation().getEducationDetailAddress());
        education.setCreatedAt(Instant.now());

        return educationRepository.save(education);
    }

    public String findEducationIdByEducationName(String educationName) {
        EducationEntity education = educationRepository.findByEducationName(educationName);
        if (education != null) {
            return education.getEducationId();
        }
        return null;
    }

    public EducationEntity findEducationByEducationId(String educationId) {
        return educationRepository.findByEducationId(educationId);
    }

    public List<FindEduAndMemberDto> getEducationList() {
        List<EducationEntity> educationList = educationRepository.findAll();
    
        return educationList.stream()
                .map((EducationEntity education) -> {
                    List<MemberEntity> directors =
                            peopleRegisterRepository.findByEducationIdAndMemberRole(
                                    education.getEducationId(), "ROLE_DIRECTOR"
                            );
                    MemberEntity director = directors.isEmpty() ? null : directors.get(0);
    
                    LocalDateTime createdAt = education.getCreatedAt() != null
                            ? LocalDateTime.ofInstant(education.getCreatedAt(), ZoneId.systemDefault())
                            : null;
    
                    LocalDateTime updatedAt = education.getUpdatedAt() != null
                            ? LocalDateTime.ofInstant(education.getUpdatedAt(), ZoneId.systemDefault())
                            : null;
    
                    return FindEduAndMemberDto.builder()
                            .educationId(education.getEducationId())
                            .educationName(education.getEducationName())
                            .memberEmail(director != null ? director.getMemberEmail() : "")
                            .memberName(director != null ? director.getMemberName() : "")
                            .businessNumber(education.getBusinessNumber())
                            .description(education.getDescription())
                            .memberAddress(director != null ? director.getMemberAddress() : "")
                            .educationAddress(education.getEducationAddress())
                            .educationDetailAddress(education.getEducationDetailAddress())
                            .createdAt(createdAt)   // LocalDateTime 변환
                            .updatedAt(updatedAt)   // LocalDateTime 변환
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void updateEducation(String educationId, FindEduAndMemberDto dto) {
        // 1. 학원 정보 수정
        EducationEntity education = educationRepository.findByEducationId(educationId);
        if (education != null) {
            education.setEducationName(dto.getEducationName());
            education.setBusinessNumber(dto.getBusinessNumber());
            education.setDescription(dto.getDescription());
            education.setEducationAddress(dto.getEducationAddress());
            education.setEducationDetailAddress(dto.getEducationDetailAddress());
            educationRepository.save(education);
        }
        
        // 2. 학원장 정보 수정
        List<MemberEntity> directors = peopleRegisterRepository.findByEducationIdAndMemberRole(educationId, "ROLE_DIRECTOR");
        if (!directors.isEmpty()) {
            MemberEntity director = directors.get(0); // 첫 번째 학원장 수정
            director.setMemberEmail(dto.getMemberEmail());
            director.setMemberName(dto.getMemberName());
            director.setMemberAddress(dto.getMemberAddress());
            peopleRegisterRepository.save(director);
        }
    }
}
