package com.jakdang.labs.api.jaegyeom.mypage.controller;

import com.jakdang.labs.api.jaegyeom.mypage.dto.MyPageRequestDto;
import com.jakdang.labs.api.jaegyeom.mypage.dto.UserAndEduDto;
import com.jakdang.labs.api.jaegyeom.mypage.service.ShowMyInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

    @Autowired
    private ShowMyInfoService showMyInfoService;

    @PostMapping("/get-my-info")
    public ResponseEntity<?> showMyInfo(@RequestBody MyPageRequestDto request) {
        try {
            // 요청을 받아 사용자 정보를 가져옵니다.
            UserAndEduDto userInfo = showMyInfoService.getUserInfo(request);
            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}