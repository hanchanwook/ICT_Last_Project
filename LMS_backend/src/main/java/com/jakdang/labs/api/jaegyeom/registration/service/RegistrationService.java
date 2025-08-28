package com.jakdang.labs.api.jaegyeom.registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.jaegyeom.registration.dto.PeopleRegisterDto;
import com.jakdang.labs.api.jaegyeom.registration.repository.CourseNameChangeIdRepository;
import com.jakdang.labs.api.jaegyeom.registration.repository.PeopleRegisterRepository;
import com.jakdang.labs.api.jaegyeom.registration.repository.CheckEmailAndPhoneRepo;
import com.jakdang.labs.api.jaegyeom.registration.dto.CheckEmailAndPhoneDto;
import com.jakdang.labs.api.auth.entity.UserEntity;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.commons.lang3.RandomStringUtils;
import java.util.UUID;

@Service
public class RegistrationService {

    @Autowired
    private PeopleRegisterRepository peopleRegisterRepository;
    @Autowired
    private PasswordEncoder argon2PasswordEncoder;
    @Autowired
    private CourseNameChangeIdRepository courseRepository;
    @Autowired
    private CheckEmailAndPhoneRepo checkEmailAndPhoneRepository;



    public void registerAll(List<PeopleRegisterDto> peopleRegisterDtoList) {
        for (PeopleRegisterDto dto : peopleRegisterDtoList) {
            MemberEntity entity = MemberEntity.builder()
                .id(UUID.randomUUID().toString()) // 임의의 아이디 생성
                .password(argon2PasswordEncoder.encode(dto.getPassword()))
                .memberName(dto.getMemberName())
                .memberEmail(dto.getMemberEmail())
                .memberPhone(dto.getMemberPhone())
                .memberBirthday(dto.getMemberBirthday())
                .memberRole(dto.getMemberRole())
                .courseId(dto.getCourseId())
                .educationId(dto.getEducationId())
                .memberAddress(dto.getMemberAddress())
                .profileFileId(dto.getProfileFileId())
                .build();
            peopleRegisterRepository.save(entity);
        }
    }

    public void registerEach(PeopleRegisterDto dto) {
        MemberEntity entity = MemberEntity.builder()
            .id(UUID.randomUUID().toString())
            .password(argon2PasswordEncoder.encode(dto.getPassword()))
            .memberName(dto.getMemberName())
            .memberEmail(dto.getMemberEmail())
            .memberPhone(dto.getMemberPhone())
            .memberBirthday(dto.getMemberBirthday())
            .memberRole(dto.getMemberRole())
            .courseId(dto.getCourseId())
            .educationId(dto.getEducationId())
            .memberAddress(dto.getMemberAddress())
            .profileFileId(dto.getProfileFileId())
            .build();
        peopleRegisterRepository.save(entity);
    }

    // **중복 처리 변경**
    public MemberEntity showData(String memberEmail, String memberName) {
        List<MemberEntity> results = peopleRegisterRepository.findByMemberEmailAndMemberName(memberEmail, memberName);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
        }
        return results.get(0); // 첫 번째 결과 반환
    }

    public String generateUniqueId() {
        String id;
        do {
            id = RandomStringUtils.randomAlphanumeric(10);
        } while (peopleRegisterRepository.existsById(id));
        return id;
    }

    public String findCourseIdByCourseName(String courseName) {
        CourseEntity course = courseRepository.findByCourseName(courseName);
        return course.getCourseId();
    }

    public CheckEmailAndPhoneDto getUserEmail(String memberEmail) {
        // users 테이블에서 중복 체크
        UserEntity user = checkEmailAndPhoneRepository.findByEmail(memberEmail.trim());
        return CheckEmailAndPhoneDto.builder()
            .memberEmail(user != null ? user.getEmail() : null)
            .build();
    }

    public CheckEmailAndPhoneDto getUserPhone(String memberPhone) {
        UserEntity user = checkEmailAndPhoneRepository.findByPhone(memberPhone.trim());
        return CheckEmailAndPhoneDto.builder()
            .memberPhone(user != null ? user.getPhone() : null)
            .build();
    }

    public UserEntity getPhone(String phone) {
        try {
            UserEntity user = checkEmailAndPhoneRepository.findByPhone(phone.trim());
            return user;
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            // 여러 결과가 있는 경우 첫 번째만 반환
            List<UserEntity> users = checkEmailAndPhoneRepository.findAllByPhone(phone.trim());
            return users.isEmpty() ? null : users.get(0);
        }
    }
    
    public List<MemberEntity> findDirectorsByEducationId(String educationId) {
        return peopleRegisterRepository.findByEducationIdAndMemberRole(educationId, "ROLE_DIRECTOR");
    }
}
