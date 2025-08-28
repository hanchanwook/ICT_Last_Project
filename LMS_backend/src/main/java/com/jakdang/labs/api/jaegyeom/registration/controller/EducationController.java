package com.jakdang.labs.api.jaegyeom.registration.controller;

import com.jakdang.labs.api.jaegyeom.registration.dto.EduRegisterDto;
import com.jakdang.labs.api.jaegyeom.registration.dto.FindEduAndMemberDto;
import com.jakdang.labs.api.jaegyeom.registration.dto.InviteMailDto;
import com.jakdang.labs.api.jaegyeom.registration.service.EducationRegisterService;
import lombok.RequiredArgsConstructor;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jakdang.labs.api.jaegyeom.registration.dto.PeopleRegisterDto;
import com.jakdang.labs.api.jaegyeom.registration.service.RegistrationService;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.auth.dto.SignUpDTO;
import com.jakdang.labs.api.auth.dto.UserUpdateDTO;
import com.jakdang.labs.api.auth.service.AuthService;
import com.jakdang.labs.entity.EducationEntity;
import java.util.List;

@RestController
@RequestMapping("/api/education")
@RequiredArgsConstructor
public class EducationController {

    private final EducationRegisterService educationService;
    private final RegistrationService registrationService;
    private final AuthService authService;
    private final JavaMailSender mailSender;

    @PostMapping("/register")
    public ResponseEntity<String> registerEducation(@RequestBody EduRegisterDto dto) {
        // 교육기관 등록
        EducationEntity education = educationService.registerEducation(dto);
        String educationId = education.getEducationId();

        PeopleRegisterDto peopleRegisterDto = PeopleRegisterDto.builder()
            .id(registrationService.generateUniqueId())
            .password(dto.getMember().getPassword())
            .memberName(dto.getMember().getMemberName())
            .memberEmail(dto.getMember().getMemberEmail())
            .memberPhone(dto.getMember().getMemberPhone())
            .memberBirthday(dto.getMember().getMemberBirthday())
            .memberRole("ROLE_DIRECTOR")
            .courseId(dto.getMember().getCourseId())
            .educationId(educationId)
            .memberAddress(dto.getMember().getMemberAddress())
            .profileFileId(dto.getMember().getProfileFileId())
            .build();

        // 학원장 등록
        registrationService.registerEach(peopleRegisterDto);

        MemberEntity entity = registrationService.showData(peopleRegisterDto.getMemberEmail(), peopleRegisterDto.getMemberName());

            // 회원 개별 등록(User 테이블에 저장됨)
            SignUpDTO signUpDTO = SignUpDTO.builder()
                    .id(entity.getId())
                    .email(peopleRegisterDto.getMemberEmail())
                    .password(peopleRegisterDto.getPassword())
                    .name(peopleRegisterDto.getMemberName())
                    .phone(peopleRegisterDto.getMemberPhone())
                    .birth(peopleRegisterDto.getMemberBirthday())
                    .role(peopleRegisterDto.getMemberRole())
                    .provider("local")
                    .build();   
            
            authService.signUpUser(signUpDTO);

        return ResponseEntity.ok("교육기관 등록이 완료되었습니다.");
    }

    @PostMapping("/invite")
    public ResponseEntity<String> inviteMember(@RequestBody InviteMailDto dto) {
    String email = dto.getEmail();

    try {
        // 이메일 본문
        String content = "<h1>학원장 가입 초대</h1>"
                + "<p>아래 버튼을 클릭하여 가입을 완료해주세요.</p>"
                + "<a href=\"http://localhost:5173/invite-register\">가입하러 가기</a>";

        String subject = "학원장 가입 초대";

        MimeMessagePreparator message = mimeMessage -> {
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            mimeMessage.setFrom("hussar12@naver.com");
            mimeMessage.setSubject(subject);
            mimeMessage.setContent(content, "text/html; charset=UTF-8");
        };

        mailSender.send(message);
        return ResponseEntity.ok("초대 이메일을 " + email + " 로 발송했습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("초대 이메일 발송 실패");
        }
    }


    @GetMapping("/list")
    public ResponseEntity<List<FindEduAndMemberDto>> getEducationList() {
        // 교육기관 목록 조회
        return ResponseEntity.ok(educationService.getEducationList());
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateEducation(@PathVariable String id, @RequestBody FindEduAndMemberDto dto) {
        try {
            // 1. educationId로 학원 정보 수정
            educationService.updateEducation(id, dto);
            
            // 2. educationId로 학원장(ROLE_DIRECTOR) 찾아서 User 테이블 정보 수정
            List<MemberEntity> directors = registrationService.findDirectorsByEducationId(id);
            if (!directors.isEmpty()) {
                MemberEntity director = directors.get(0);
                
                // UserUpdateDTO로 변환하여 기존 사용자 정보 업데이트
                UserUpdateDTO userUpdateDTO = UserUpdateDTO.builder()
                        .name(dto.getMemberName())
                        .email(dto.getMemberEmail())
                        .role(director.getMemberRole())
                        .build();
                
                authService.updateUserInfo(userUpdateDTO, director.getId());
            }
            
            return ResponseEntity.ok("교육기관 정보 수정이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("교육기관 정보 수정에 실패했습니다: " + e.getMessage());
        }
    }
}
