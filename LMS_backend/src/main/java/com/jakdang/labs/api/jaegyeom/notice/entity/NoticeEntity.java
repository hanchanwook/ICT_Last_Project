package com.jakdang.labs.api.jaegyeom.notice.entity;

import jakarta.persistence.*;

import com.jakdang.labs.global.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "notices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String noticeId; // 공지사항 ID (UUID)
    
    @Column(nullable = false)
    private String title; // 제목
    
    @Column(columnDefinition = "TEXT")
    private String content; // 내용
    
    @Column(name = "imageUrl", columnDefinition = "LONGTEXT")
    private String imageUrl; // 이미지 URL
    
    @Column(name = "isActive", columnDefinition = "TINYINT(1)")
    @JsonProperty("isActive")
    private Boolean isActive; // 0: 비활성화, 1: 활성화
    
    @Column(name = "createdBy")
    private String createdBy; // 작성자
    
    @Column(name = "updatedBy")
    private String updatedBy; // 수정자

    @Column(name = "educationId")
    private String educationId; // 교육 ID
}