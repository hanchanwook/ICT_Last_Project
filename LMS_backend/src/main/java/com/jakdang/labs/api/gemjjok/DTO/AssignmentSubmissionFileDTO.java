package com.jakdang.labs.api.gemjjok.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionFileDTO {

    private String id; // 외부 파일 서비스의 파일 ID
    private String submissionId; // 제출 ID
    private String assignmentId; // 과제 ID
    private String courseId; // 과정 ID
    private String studentId; // 학생 ID
    private String fileKey; // 파일 키
    private String fileName; // 파일명
    private Long fileSize; // 파일 크기
    private String fileType; // 파일 타입
    private Boolean isActive; // 활성화 여부
    private LocalDateTime createdAt; // 생성일
    private LocalDateTime updatedAt; // 수정일
} 