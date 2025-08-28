package com.jakdang.labs.api.auth.service;

import com.jakdang.labs.api.auth.dto.RoleType;
import com.jakdang.labs.api.auth.dto.UserDTO;
import com.jakdang.labs.api.auth.dto.UserUpdateDTO;
import com.jakdang.labs.api.auth.entity.UserEntity;
import com.jakdang.labs.api.auth.repository.AuthRepository;
import com.jakdang.labs.api.auth.repository.UserRepository;
import com.jakdang.labs.exceptions.handler.CustomException;
import com.jakdang.labs.api.common.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;

    public ResponseDTO<List<UserDTO>> getAllUsers() {
        List<UserEntity> users = userRepository.findAll();
        return ResponseDTO.createSuccessResponse("Success", users.stream()
                .map(entity -> UserDTO.builder()
                        .id(entity.getId())
                        .activated(entity.getActivated())
                        .email(entity.getEmail())
                        .name(entity.getName())
                        .phone(entity.getPhone())
                        .nickname(entity.getNickname())
                        .role(entity.getRole().getRole())
                        .tCreatedAt(entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()))
                        .build()).toList()
        );
    }

    public ResponseDTO<UserDTO> updateUser(String userId, UserUpdateDTO userUpdateDTO) {
        Optional<UserEntity> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            user.update(userUpdateDTO);
            user.setName(userUpdateDTO.getName());
            user.setActivated(userUpdateDTO.getActivated());
            user.setRole(RoleType.valueOf(userUpdateDTO.getRole()));
		    user.setImage(userUpdateDTO.getImage());
            userRepository.save(user);
            return ResponseDTO.createSuccessResponse("유저 정보 업데이트 성공", 
            UserDTO.builder()
            .id(user.getId())
            .activated(user.getActivated())
            .email(user.getEmail())
            .name(user.getName())
            .phone(user.getPhone())
            .nickname(user.getNickname())
            .build());
        } else {
            throw new CustomException("유저를 찾을 수 없습니다.", -200);
        }
    }

    public ResponseDTO<UserDTO> findUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow( () -> new CustomException("유저를 찾을 수 없습니다.", -200));

        return ResponseDTO.createSuccessResponse("유저 데이터 불러오기 성공", UserDTO.builder()
                .nickname(user.getNickname())
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build());
    }

    public UserDTO getUserDTO(String userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        UserDTO userDTO = null;
        if (user != null) {
            userDTO = UserDTO.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .image(user.getImage())
                    .nickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getName())
                    .build();
        }
        return userDTO;
    }

    public UserEntity findById(String requestUserId) {
        return userRepository.findById(requestUserId).orElse(null);
    }

    public ResponseDTO<UserDTO> getUserProfile(String id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("유저를 찾을 수 없습니다.", -200));

        return ResponseDTO.createSuccessResponse("유저 데이터 불러오기 성공", UserDTO.builder()
                .nickname(user.getNickname())
                .id(user.getId())
                .bio(user.getBio())
                .email(user.getEmail())
                .image(user.getImage())
                .build());
    }
}

