package com.jakdang.labs.api.auth.controller;

import com.jakdang.labs.aop.RunningTime;
import com.jakdang.labs.api.auth.dto.UserDTO;
import com.jakdang.labs.api.auth.dto.UserUpdateDTO;
import com.jakdang.labs.api.auth.service.UserService;
import com.jakdang.labs.api.common.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping(value = "/find/{email}")
    @RunningTime
    public ResponseEntity<ResponseDTO<UserDTO>> findUserByEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok().body(userService.findUserByEmail(email));
    }

    @GetMapping(value = "/all")
    @RunningTime
    public ResponseEntity<ResponseDTO<List<UserDTO>>> getAllUsers() {
        return ResponseEntity.ok().body(userService.getAllUsers());
    }

    @PutMapping(value = "/update/{id}")
    @RunningTime
    public ResponseEntity<ResponseDTO<UserDTO>> updateUser(@PathVariable("id") String id, @RequestBody UserUpdateDTO userUpdateDTO) {
        return ResponseEntity.ok().body(userService.updateUser(id, userUpdateDTO));
    }

    @GetMapping(value = "/profile/{id}")
    @RunningTime
    public ResponseEntity<ResponseDTO<UserDTO>> getUserProfile(@PathVariable("id") String id) {
        return ResponseEntity.ok().body(userService.getUserProfile(id));
    }
 }