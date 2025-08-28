package com.jakdang.labs.entity;

import com.jakdang.labs.global.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scorestudent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreStudentEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID 생성
    @Column(name = "scoreStudentId", columnDefinition = "VARCHAR(100)")
    private String scoreStudentId; // 학생 점수 UUID

    @Column(name = "score", columnDefinition = "int")
    private int score; // 학생 총점

    @Column(name = "isChecked", columnDefinition = "int default 1")
    private int isChecked; // 학생 점수 확인 여부  (0: 학생 확인 완료, 1: 채점 완료/학생 미확인)

    @Column(name = "totalComment", columnDefinition = "VARCHAR(100)")
    private String totalComment; // 선생의 총 코멘트

    @Column(name = "memberId", columnDefinition = "VARCHAR(100)")
    private String memberId; // 학생 UUID
    
    @Column(name = "templateId", columnDefinition = "VARCHAR(100)")
    private String templateId; // 시험 UUID

    @Column(name = "graded", columnDefinition = "int default 0")
    private Integer graded; // 채점 완료 여부 (0: 채점 완료, 1: 미채점)

}
