package com.jakdang.labs.api.youngjae.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.jakdang.labs.entity.MemberEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSearchResponse {
    private boolean success;
    private String error;
    private List<UserDto> data;
    private int totalCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String name;
        private String type;
        private String department;
        private Integer grade;
        private String studentNumber;
        private String email;
        
        public static UserDto from(MemberEntity member) {
            return UserDto.builder()
                    .id(member.getId())
                    .name(member.getMemberName())
                    .type(member.getMemberRole().toLowerCase()) // 소문자로 변환
                    .department("컴퓨터공학과") // 임시값, 실제로는 member에서 가져와야 함
                    .grade(2) // 임시값, 실제로는 member에서 가져와야 함
                    .studentNumber(member.getId()) // 학번은 id와 동일
                    .email(member.getMemberEmail())
                    .build();
        }
    }
} 