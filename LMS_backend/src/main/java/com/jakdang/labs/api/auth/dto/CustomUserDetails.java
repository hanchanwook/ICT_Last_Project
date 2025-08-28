package com.jakdang.labs.api.auth.dto;

import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.entity.MemberEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@RequiredArgsConstructor
@ToString
public class CustomUserDetails implements UserDetails {

    private final UserEntity userEntity;
    private final MemberEntity memberEntity;

    public Collection<? extends GrantedAuthority> getAuthorities() {

        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        authorities.add((GrantedAuthority) () -> userEntity.getRole().getRole());

        return authorities;
    }

    public String getUserId() {
        return userEntity.getId();
    }

    @Override
    public String getPassword() {
        return userEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return userEntity.getName();
    }

    public RoleType getRole() {
        return userEntity.getRole();
    }

    public String getEmail() {
        return userEntity.getEmail();
    }

    public String getEducationId() {
        return memberEntity.getEducationId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

//    @Override
//    public boolean isEnabled() {
//        return UserDetails.super.isEnabled();
//    }

    @Override
    public boolean isEnabled() {
        return userEntity.getActivated();
    }
}
