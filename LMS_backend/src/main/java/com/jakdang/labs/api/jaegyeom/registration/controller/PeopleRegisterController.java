package com.jakdang.labs.api.jaegyeom.registration.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;


import com.jakdang.labs.api.jaegyeom.registration.dto.PeopleRegisterDto;
import com.jakdang.labs.api.jaegyeom.registration.service.RegistrationService;
import com.jakdang.labs.api.auth.dto.SignUpDTO;
import com.jakdang.labs.api.auth.service.AuthService;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.jaegyeom.registration.dto.CheckEmailAndPhoneDto;
import com.jakdang.labs.api.auth.entity.UserEntity;



@RestController
@RequestMapping("/api/registration")
public class PeopleRegisterController {

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private AuthService authService;

    @PostMapping("/registerAll")
    public ResponseEntity<String> registerAll(@RequestBody List<PeopleRegisterDto> peopleRegisterDtoList) {
        try {
            for (PeopleRegisterDto peopleRegisterDto : peopleRegisterDtoList) {
                // courseName → courseId 변환
                if (peopleRegisterDto.getCourseName() != null) {
                    String courseId = registrationService.findCourseIdByCourseName(peopleRegisterDto.getCourseName());
                    peopleRegisterDto.setCourseId(courseId);
                } else {
                }

                registrationService.registerEach(peopleRegisterDto);
            }

            for (PeopleRegisterDto peopleRegisterDto : peopleRegisterDtoList) {
                MemberEntity entity = registrationService.showData(peopleRegisterDto.getMemberEmail(), peopleRegisterDto.getMemberName());

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
            }

            return ResponseEntity.ok("회원 일괄 등록 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 과정명이 포함되어 있습니다: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원 일괄 등록 실패");
        }
    }

    @PostMapping("/registerEach")
    public ResponseEntity<String> registerEach(@RequestBody PeopleRegisterDto peopleRegisterDto) {
        try {
            String phone = peopleRegisterDto.getMemberPhone();
            UserEntity user = registrationService.getPhone(phone);
            
            // user가 null이 아닌 경우에만 전화번호 비교
            if (user != null) {
                if (user.getPhone() != null && user.getPhone().trim().equals(phone.trim())) {
                    return ResponseEntity.ok("등록된 전화번호입니다. 다시 입력해주세요.");
                }
            }


            // 회원 개별 등록(Member 테이블에 저장됨)
            registrationService.registerEach(peopleRegisterDto);

            // UUID가 된 ID를 추출하기 위한 메서드
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

            return ResponseEntity.ok("회원 개별 등록 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원 개별 등록 실패");
        }
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody Map<String, String> requestBody) {
        String memberEmail = requestBody.get("memberEmail");

        CheckEmailAndPhoneDto email = registrationService.getUserEmail(memberEmail);

        Map<String, Object> result = new HashMap<>();
        result.put("isDuplicate", email.getMemberEmail() != null);
        result.put("message", email.getMemberEmail() == null ? "사용 가능한 이메일입니다." : "이미 존재하는 이메일입니다.");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/check-phone")
    public ResponseEntity<Map<String, Object>> checkPhone(@RequestBody Map<String, String> requestBody) {
        String memberPhone = requestBody.get("memberPhone");

        CheckEmailAndPhoneDto phone = registrationService.getUserPhone(memberPhone);

        Map<String, Object> result = new HashMap<>();
        result.put("isDuplicate", phone.getMemberPhone() != null);
        result.put("message", phone.getMemberPhone() == null ? "사용 가능한 전화번호입니다." : "이미 존재하는 전화번호입니다.");
        return ResponseEntity.ok(result);
    }

}