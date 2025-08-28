//package com.jakdang.labs.security.oauth.dto;
//
//import com.jakdang.labs.api.auth.entity.UserEntity;
//import lombok.Getter;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//
//@Getter
//public class PrincipalDetails implements UserDetails, OAuth2User {
//
//    private UserEntity user;
//    private Map<String, Object> attributes;
//
//    // 일반 로그인 생성자
//    public PrincipalDetails(UserEntity user) {
//        this.user = user;
//    }
//
//    // OAuth2 로그인 생성자
//    public PrincipalDetails(UserEntity user, Map<String, Object> attributes) {
//        this.user = user;
//        this.attributes = attributes;
//    }
//
//    // OAuth2User 인터페이스 메소드
//    @Override
//    public Map<String, Object> getAttributes() {
//        return attributes;
//    }
//
//    @Override
//    public String getName() {
//        return user.getName();
//    }
//
//    public String getId() {
//        return user.getId();
//    }
//
//    public String getRole(){
//        return user.getRole().toString();
//    }
//
//    // UserDetails 인터페이스 메소드
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        List<GrantedAuthority> authorities = new ArrayList<>();
//        authorities.add(new SimpleGrantedAuthority(user.getRole().toString()));
//        return authorities;
//    }
//
//    @Override
//    public String getPassword() {
//        return user.getPassword();
//    }
//
//    @Override
//    public String getUsername() {
//        return user.getEmail();
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return true;
//    }
//}