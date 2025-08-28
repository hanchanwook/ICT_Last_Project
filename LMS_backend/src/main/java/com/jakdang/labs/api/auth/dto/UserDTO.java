package com.jakdang.labs.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private String id;
    private String password;
    private String email;
    private String phone;
    private Boolean activated;
    private String name;
    private String provider;
    private String createdAt;
    private String updatedAt;
    private String deviceToken;
    private String tokenType;
    private Boolean advertise;
    private ZonedDateTime lastLogin;
    private String isCallable;
    private String google;
    private String facebook;
    private String naver;
    private String kakao;
    private String apple;
    private String nickname;
    private String image;
    private String bio;
    private int count;
    private String grade;
    private String userAgent;
    private String uName, sName, cName, memberNickname;
    private String role;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("tCreatedAt")
    private ZonedDateTime tCreatedAt;
    private String mName, mId;
}
