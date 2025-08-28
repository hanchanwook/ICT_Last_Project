package com.jakdang.labs.api.jaegyeom.registration.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jakdang.labs.api.jaegyeom.registration.repository.GetCourseRepo;
import com.jakdang.labs.entity.CourseEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCourseService {

    private final GetCourseRepo getCourseRepo;

    public List<CourseEntity> getCourseList(String educationId) {
        List<CourseEntity> courseList = getCourseRepo.getCourseList(educationId);
        return courseList;
    }
}
