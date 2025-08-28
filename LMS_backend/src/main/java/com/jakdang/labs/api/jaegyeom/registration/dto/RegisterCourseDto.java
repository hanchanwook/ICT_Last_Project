package com.jakdang.labs.api.jaegyeom.registration.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCourseDto {
    private String memberEmail;
    private String courseId;
    private String courseName;
}
