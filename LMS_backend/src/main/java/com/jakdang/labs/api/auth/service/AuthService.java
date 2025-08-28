package com.jakdang.labs.api.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.jakdang.labs.api.auth.dto.*;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.api.auth.repository.AuthRepository;
import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.security.jwt.service.TokenService;
import com.jakdang.labs.security.jwt.utils.TokenUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder argon2PasswordEncoder;
    private final TokenService tokenService;
//    private final FirebaseAuth firebaseAuth;
    private final TokenUtils tokenUtils;

    @Transactional
    public ResponseDTO<?> signUpUser(SignUpDTO signUpDTO) {

        if (checkRequiredFields(signUpDTO)){
            return ResponseDTO.createErrorResponse(400, "요청값이 비어있습니다.");
        }

        if (existsUser(signUpDTO)) {
            return ResponseDTO.createErrorResponse(400, "이미 존재하는 이메일입니다.");
        }

        UserEntity user = UserEntity.fromDto(signUpDTO);
        user.setPassword(argon2PasswordEncoder.encode(signUpDTO.getPassword()));
        user.setActivated(true);
        user.setRole(RoleType.valueOf(signUpDTO.getRole()));
        user.setId(signUpDTO.getId());

        authRepository.save(user);

        return ResponseDTO.createSuccessResponse("회원가입이 완료되었습니다.", null);
    }

//    public ResponseDTO<TokenDTO> joinSns(Map<String, Object> header, SnsSignInRequest dto, HttpServletResponse res) throws FirebaseAuthException {
//        String token = dto.getAccessToken();
//        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
//        String provider = dto.getType();
//        String email = decodedToken.getEmail();
//        String name = decodedToken.getName();
//
//        log.info("uid: {}, email: {}, name: {}", provider, email, name);
//
//        UserEntity user = findOrCreateUser(provider, email, name);
//
//        TokenDTO tokenDTO = tokenService.createTokenPair(user.getName(), user.getRole().toString(), email, user.getId());
//        tokenUtils.addRefreshTokenCookie(res, tokenDTO.getRefreshToken());
//
//        return ResponseDTO.<TokenDTO>builder()
//                .resultCode(200)
//                .resultMessage("소셜 가입/로그인 성공")
//                .data(tokenDTO)
//                .build();
//
//    }

    private UserEntity findOrCreateUser(String provider, String email, String name) {
        Optional<UserEntity> optionalUser = authRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            UserEntity user = optionalUser.get();

            if (user.getProvider() == null || !provider.equals("google")) {
                user.setProvider("google");
            }

            if (user.getProvider() == null || !provider.equals("apple")) {
                user.setProvider("apple");
            }

            return user;
        } else {
            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .name(name)
                    .role(RoleType.ROLE_STUDENT)
                    .provider(provider)
                    .build();

            return authRepository.save(newUser);
        }
    }

    @Transactional
    public ResponseDTO<?> joinApple(Map<String, Object> header, UserDTO dto){
        UserEntity user = authRepository.findByEmail(dto.getEmail()).orElse(null);
        UserEntity savedUser;
        String provider = header.get("user-agent").toString();
        if (user == null){
            UserEntity newUser = UserEntity.builder()
                    .email(dto.getEmail())
                    .name(dto.getName())
                    .role(RoleType.ROLE_STUDENT)
                    .provider(provider)
                    .password(argon2PasswordEncoder.encode(dto.getPassword()))
                    .activated(true)
                    .build();
            savedUser = authRepository.save(newUser);
        } else {
            user.setProvider(provider);
            user.setEmail(dto.getEmail());
            savedUser = user;
        }

        // Use the new social token creation method
        TokenDTO tokenDTO = tokenService.createTokenPairForSocial(savedUser.getId(), savedUser.getRole().toString(), savedUser.getEmail(), savedUser.getId());

        return ResponseDTO.<TokenDTO>builder()
                .resultCode(200)
                .resultMessage("소셜 가입/로그인 성공")
                .data(tokenDTO)
                .build();
    }

    private boolean checkRequiredFields(SignUpDTO user){
        return user.getEmail() == null || user.getPassword() == null || user.getName() == null;
    }

    private boolean existsUser(SignUpDTO user){
        return authRepository.existsByEmail(user.getEmail());
    }

    public ResponseDTO<UserDTO> getUserInfo(String id) {
        UserEntity user = authRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException());

        UserDTO userDTO = UserDTO.builder()
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .image(user.getImage())
                .createdAt(user.getCreatedAt().toString())
                .nickname(user.getNickname())
                .bio(user.getBio())
                .build();

        return ResponseDTO.<UserDTO>builder()
                .resultCode(200)
                .resultMessage("유저 정보 조회 성공")
                .data(userDTO)
                .build();
    }
    @Transactional
    public ResponseDTO<?> updateUserInfo(UserUpdateDTO dto, String id) {
        UserEntity user = authRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException());
        user.update(dto);

        return ResponseDTO.createSuccessResponse("유저 정보 수정 성공" , null);
    }

    public List<UserDTO> getMemberInfoList(List<String> likedUserIds) {
        return authRepository.findByIdIn(likedUserIds).stream()
                .map( user -> UserDTO.builder()
                        .nickname(user.getNickname())
                        .image(user.getImage())
                        .email(user.getEmail())
                        .build()).toList();
    }
}