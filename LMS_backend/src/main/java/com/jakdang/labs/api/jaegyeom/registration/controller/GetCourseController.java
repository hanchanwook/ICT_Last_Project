package com.jakdang.labs.api.jaegyeom.registration.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jakdang.labs.api.jaegyeom.registration.service.GetCourseService;
import com.jakdang.labs.entity.CourseEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/getCourse")
public class GetCourseController {

    private final GetCourseService getCourseService;

    @GetMapping("/list")
    public ResponseEntity<List<CourseEntity>> getCourseList(@RequestParam String educationId) {
        return ResponseEntity.ok(getCourseService.getCourseList(educationId));
    }
}
