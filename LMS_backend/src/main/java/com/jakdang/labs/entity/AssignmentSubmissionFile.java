package com.jakdang.labs.entity;

import com.jakdang.labs.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assignment_submission_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionFile extends BaseEntity {

    @Id
    @Column(name = "id", length = 100)
    private String id; // 외부 파일 서비스의 파일 ID (UUID)

    @Column(name = "submission_id", length = 100, nullable = true, columnDefinition = "VARCHAR(100) NULL")
    private String submissionId; // assignmentsubmission.submissionId (과제 제출 시에만 설정)

    @Column(name = "assignment_id", length = 100, nullable = false)
    private String assignmentId; // assignment.assignmentId

    @Column(name = "course_id", length = 100, nullable = false)
    private String courseId; // course.courseId

    @Column(name = "student_id", length = 100, nullable = false)
    private String studentId; // users.id (학생)

    @Column(name = "file_key", length = 255)
    private String fileKey; // 외부 파일 서비스의 파일 키 (UUID + 확장자)

    @Column(name = "file_name", length = 255)
    private String fileName; // 원본 파일명

    @Column(name = "file_size")
    private Long fileSize; // 파일 크기

    @Column(name = "file_type", length = 50)
    private String fileType; // 파일 타입 (MIME type)

    @Column(name = "is_active")
    private Boolean isActive = true; // 활성화 여부
} 