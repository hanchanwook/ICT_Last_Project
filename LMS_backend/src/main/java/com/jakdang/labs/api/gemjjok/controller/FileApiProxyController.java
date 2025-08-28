package com.jakdang.labs.api.gemjjok.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Slf4j
public class FileApiProxyController {
    
    /**
     * 강사 쪽 파일 다운로드 프록시
     * GET /api/file/download/{fileKey} -> GET /api/instructor/file/download/{fileKey}
     */
    @GetMapping("/download/{fileKey}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String fileKey,
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.info("강사 파일 다운로드 프록시 요청 - fileKey: {}, userId: {}", fileKey, userId);
        
        try {
            // /api/instructor/file/download/{fileKey}로 리다이렉트
            String redirectUrl = "/api/instructor/file/download/" + fileKey;
            if (userId != null) {
                redirectUrl += "?userId=" + userId;
            }
            
            log.info("강사 파일 다운로드 URL로 리다이렉트: {}", redirectUrl);
            
            return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
                
        } catch (Exception e) {
            log.error("강사 파일 다운로드 프록시 오류: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
} 