package com.jakdang.labs.api.gemjjok.DTO;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassInfoResponse {
    private String classId;
    private String classCode;      // "A101" 형식
    private String className;      // "A동 101호" 형식
    private Integer capacity;      // 수용 인원
    private String building;       // 건물명 (예: "A동")
    private String floor;          // 층수 (예: "1층")
    private String roomNumber;     // 호수 (예: "101")
} 