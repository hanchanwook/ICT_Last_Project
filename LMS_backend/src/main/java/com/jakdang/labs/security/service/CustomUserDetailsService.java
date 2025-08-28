package com.jakdang.labs.security.service;

import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import com.jakdang.labs.api.auth.repository.AuthRepository;
import com.jakdang.labs.api.common.EducationId;
import com.jakdang.labs.entity.MemberEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthRepository userRepository;
    private final EducationId educationId;
    @Override
    public UserDetails loadUserByUsername(String email) {
        log.info(email);
        return userRepository.findByEmail(email)
                .map(user -> {
                    String educationId = this.educationId.getEducationIdByUserId(user.getId()).orElse("");
                    MemberEntity member = new MemberEntity();
                    member.setEducationId(educationId);
                    return new CustomUserDetails(user, member);
                })
                .orElseThrow( () -> new UsernameNotFoundException("UserDetails is null"));
    }
}
