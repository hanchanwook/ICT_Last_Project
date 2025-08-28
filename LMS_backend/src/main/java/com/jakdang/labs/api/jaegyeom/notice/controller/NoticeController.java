package com.jakdang.labs.api.jaegyeom.notice.controller;

import com.jakdang.labs.api.jaegyeom.notice.dto.CreateNoticeDto;
import com.jakdang.labs.api.jaegyeom.notice.dto.NoticeDto;
import com.jakdang.labs.api.jaegyeom.notice.dto.UpdateNoticeDto;
import com.jakdang.labs.api.jaegyeom.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {
    
    private final NoticeService noticeService;
    
    // 공지사항 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<NoticeDto>> getNoticeList(@RequestParam String educationId) {
        try {
            List<NoticeDto> notices = noticeService.getAllNotices(educationId);
            return ResponseEntity.ok(notices);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 공지사항 생성
    @PostMapping("/create")
    public ResponseEntity<NoticeDto> createNotice(@RequestBody CreateNoticeDto createNoticeDto) {
        try {
            // DTO에서 작성자 정보를 우선 사용하고, 없으면 현재 사용자명 사용
            String createdBy = createNoticeDto.getCreatedBy() != null ? 
                createNoticeDto.getCreatedBy() : getCurrentUsername();
            NoticeDto createdNotice = noticeService.createNotice(createNoticeDto, createdBy);
            return ResponseEntity.ok(createdNotice);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 공지사항 수정
    @PutMapping("/{id}")
    public ResponseEntity<NoticeDto> updateNotice(@PathVariable String id, @RequestBody UpdateNoticeDto updateNoticeDto) {
        try {
            // DTO에서 수정자 정보를 우선 사용하고, 없으면 현재 사용자명 사용
            String updatedBy = updateNoticeDto.getUpdatedBy() != null ? 
                updateNoticeDto.getUpdatedBy() : getCurrentUsername();
            NoticeDto updatedNotice = noticeService.updateNotice(id, updateNoticeDto, updatedBy);
            return ResponseEntity.ok(updatedNotice);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 공지사항 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable String id) {
        try {
            noticeService.deleteNotice(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 공지사항 활성화/비활성화 토글
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NoticeDto> toggleNoticeActive(@PathVariable String id, @RequestBody(required = false) Boolean isActive) {
        try {
            NoticeDto toggledNotice = noticeService.toggleNoticeActive(id, isActive);
            return ResponseEntity.ok(toggledNotice);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    // 공지사항 공개/비공개 토글
    @PatchMapping("/{id}/toggle-visibility")
    public ResponseEntity<NoticeDto> toggleVisibility(@PathVariable String id, @RequestBody(required = false) Boolean isActive) {
        try {
            NoticeDto toggledNotice = noticeService.toggleNoticeActive(id, isActive);
            return ResponseEntity.ok(toggledNotice);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 이미지 업로드 (임시 URL 생성)
    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestBody String base64Image) {
        try {
            // 실제로는 이미지를 서버에 저장하고 URL을 반환해야 함
            // 여기서는 임시로 base64 데이터를 그대로 반환
            return ResponseEntity.ok(base64Image);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 현재 사용자명 가져오기
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "unknown";
    }
    
    // 현재 사용자 이메일 가져오기 (토큰에서 추출)
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            // CustomUserDetails에서 이메일 추출 시도
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // UserDetails에서 이메일 정보가 있다면 추출
                return authentication.getName(); // 기본적으로 username 반환
            }
        }
        return "unknown@example.com";
    }
}
