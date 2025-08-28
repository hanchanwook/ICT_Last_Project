package com.jakdang.labs.api.jaegyeom.notice.service;

import com.jakdang.labs.api.jaegyeom.notice.dto.CreateNoticeDto;
import com.jakdang.labs.api.jaegyeom.notice.dto.NoticeDto;
import com.jakdang.labs.api.jaegyeom.notice.dto.UpdateNoticeDto;
import com.jakdang.labs.api.jaegyeom.notice.entity.NoticeEntity;
import com.jakdang.labs.api.jaegyeom.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 공지사항 목록 조회
    @Transactional(readOnly = true)
    public List<NoticeDto> getAllNotices(String educationId) {
        List<NoticeEntity> notices = noticeRepository.findByEducationIdOrderByCreatedAtDesc(educationId);
        return notices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 공지사항 생성
    @Transactional
    public NoticeDto createNotice(CreateNoticeDto createNoticeDto, String createdBy) {
        NoticeEntity notice = NoticeEntity.builder()
                .title(createNoticeDto.getTitle())
                .content(createNoticeDto.getContent())
                .imageUrl(createNoticeDto.getImageUrl())
                .isActive(createNoticeDto.isActive()) // boolean -> TINYINT(1)로 자동 변환
                .createdBy(createdBy)
                .educationId(createNoticeDto.getEducationId())
                .build();

        NoticeEntity savedNotice = noticeRepository.save(notice);
        return convertToDto(savedNotice);
    }

    // 공지사항 수정
    @Transactional
    public NoticeDto updateNotice(String id, UpdateNoticeDto updateNoticeDto, String updatedBy) {
        NoticeEntity notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다. ID: " + id));

        notice.setTitle(updateNoticeDto.getTitle());
        notice.setContent(updateNoticeDto.getContent());
        notice.setImageUrl(updateNoticeDto.getImageUrl());
        notice.setIsActive(updateNoticeDto.isActive()); // boolean -> TINYINT(1)로 자동 변환
        notice.setUpdatedBy(updatedBy);
        notice.setEducationId(updateNoticeDto.getEducationId()); // educationId 설정

        NoticeEntity updatedNotice = noticeRepository.save(notice);
        return convertToDto(updatedNotice);
    }

    // 공지사항 삭제
    @Transactional
    public void deleteNotice(String id) {
        if (!noticeRepository.existsById(id)) {
            throw new RuntimeException("공지사항을 찾을 수 없습니다. ID: " + id);
        }
        noticeRepository.deleteById(id);
    }

    // 공지사항 활성화/비활성화 토글
    @Transactional
    public NoticeDto toggleNoticeActive(String id, Boolean desiredState) {
        NoticeEntity notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다. ID: " + id));

        if (desiredState != null) {
            notice.setIsActive(desiredState);
        } else {
            notice.setIsActive(!Boolean.TRUE.equals(notice.getIsActive()));
        }

        NoticeEntity updatedNotice = noticeRepository.save(notice);
        return convertToDto(updatedNotice);
    }



    // Entity를 DTO로 변환
    private NoticeDto convertToDto(NoticeEntity entity) {
        NoticeDto dto = NoticeDto.builder()
                .id(entity.getNoticeId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()) ? 1 : 0) // TINYINT(1) -> boolean으로 안전하게 변환
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .educationId(entity.getEducationId())
                .build();
        
        return dto;
    }
}