package com.jakdang.labs.api.jaegyeom.registration.controller;

import com.jakdang.labs.api.jaegyeom.registration.dto.RegisterCourseDto;
import com.jakdang.labs.api.jaegyeom.registration.service.RegisterCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registerCourse")
@RequiredArgsConstructor
public class RegisterCourseController {

    private final RegisterCourseService registerCourseService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody List<RegisterCourseDto> registerCourseDtos) {
        registerCourseService.registerCourses(registerCourseDtos);
        return ResponseEntity.ok("신청되었습니다");
    }
}