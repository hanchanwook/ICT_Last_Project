package com.jakdang.labs.api.auth.entity;

import com.jakdang.labs.api.auth.dto.RoleType;
import com.jakdang.labs.api.auth.dto.SignUpDTO;
import com.jakdang.labs.api.auth.dto.UserUpdateDTO;
import com.jakdang.labs.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class UserEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "varchar(100)")
    private String id;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 255)
    private String phone;

    // 0 비활성 1 활성
    @Column(name = "activated", columnDefinition = "tinyint(1) default 1")
    private Boolean activated;

    @Column(name = "role", length = 255)
    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "provider", length = 255)
    private String provider;

    @Column(name = "device_token", columnDefinition = "longtext")
    private String deviceToken;

    @Column(name = "token_type", columnDefinition = "int(11)")
    private Integer tokenType;

    @Column(name = "advertise", columnDefinition = "tinyint(1)")
    private Boolean advertise;

    @Column(name = "last_login")
    @Temporal(TemporalType.DATE)
    private Date lastLogin;

    @Column(name = "is_callable", length = 255)
    private String isCallable;

    @Column(name = "refresh_token", columnDefinition = "longtext")
    private String refreshToken;

    @Column(name = "google", length = 255)
    private String google;

    @Column(name = "kakao", length = 255)
    private String kakao;

    @Column(name = "apple", length = 255)
    private String apple;

    @Column(name = "facebook", length = 255)
    private String facebook;

    @Column(name = "naver", length = 255)
    private String naver;

    @Column(name = "referral_code", length = 50)
    private String referralCode;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    @Column(name = "bio", length = 100)
    private String bio;

    public static UserEntity fromDto(SignUpDTO signUpDTO) {
        return UserEntity.builder()
                .email(signUpDTO.getEmail())
                .password(signUpDTO.getPassword())
                .name(signUpDTO.getName())
                .phone(signUpDTO.getPhone())
                .provider(signUpDTO.getProvider())
                .build();
    }

    public void update(UserUpdateDTO dto) {
        setName(dto.getName());
        //setEmail(dto.getEmail()); // 위험
        setNickname(dto.getNickname());
        setImage(dto.getImage());
        setBio(dto.getBio());
    }

}