package com.jakdang.labs.api.jaegyeom.registration.service;

import com.jakdang.labs.api.jaegyeom.registration.dto.RegisterCourseDto;
import com.jakdang.labs.api.jaegyeom.registration.repository.RegisterCourseRepo;
import com.jakdang.labs.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterCourseService {

    private final RegisterCourseRepo registerCourseRepo;

    /**
     * 학생 이메일 기준으로 기본 정보를 가져와 새로운 courseId를 가진 학생 등록
     * @param registerCourseDtos 여러 과정 신청 요청
     */
    @Transactional
    public void registerCourses(List<RegisterCourseDto> registerCourseDtos) {
        for (RegisterCourseDto dto : registerCourseDtos) {
            // 이메일 기반 기존 회원 조회
            List<MemberEntity> members = registerCourseRepo.findByMemberEmail(dto.getMemberEmail());

            if (members.isEmpty()) {
                throw new RuntimeException("회원 정보를 찾을 수 없습니다: " + dto.getMemberEmail());
            }

            // 첫 번째 회원 정보 기준 복사
            MemberEntity baseMember = members.get(0);

            // 새로운 과정으로 새로운 회원 Row 생성
            MemberEntity newMember = MemberEntity.builder()
                    .id(baseMember.getId())
                    .memberName(baseMember.getMemberName())
                    .memberEmail(baseMember.getMemberEmail())
                    .memberPhone(baseMember.getMemberPhone())
                    .memberBirthday(baseMember.getMemberBirthday())
                    .memberAddress(baseMember.getMemberAddress())
                    .memberRole(baseMember.getMemberRole())
                    .profileFileId(baseMember.getProfileFileId())
                    .educationId(baseMember.getEducationId())
                    .courseId(dto.getCourseId()) // **신규 과정**
                    .memberExpired(null)
                    .build();

            registerCourseRepo.save(newMember);  // **항상 INSERT**
        }
    }
}
