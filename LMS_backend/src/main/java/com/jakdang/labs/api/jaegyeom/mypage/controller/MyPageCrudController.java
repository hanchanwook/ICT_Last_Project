package com.jakdang.labs.api.jaegyeom.mypage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.jakdang.labs.api.jaegyeom.mypage.dto.MyPageCrudRequestDto;
import com.jakdang.labs.api.jaegyeom.mypage.dto.MyPageCrudResponseDto;
import com.jakdang.labs.api.jaegyeom.mypage.service.MyPageCrudService;

@RestController
@RequestMapping("/api/mypage")
public class MyPageCrudController {

    @Autowired
    private MyPageCrudService myPageCrudService;

    @PostMapping("/update-my-info")
    public ResponseEntity<MyPageCrudResponseDto> updateMyInfo(@RequestBody MyPageCrudRequestDto request) {
        return ResponseEntity.ok(myPageCrudService.updateUserInfo(request));
    }

    @PostMapping("/delete-my-info")
    public ResponseEntity<MyPageCrudResponseDto> deleteMyInfo(@RequestBody MyPageCrudRequestDto request) {
        return ResponseEntity.ok(myPageCrudService.deleteUserInfo(request));
    }
}