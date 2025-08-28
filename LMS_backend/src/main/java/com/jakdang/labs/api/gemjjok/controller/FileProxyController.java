package com.jakdang.labs.api.gemjjok.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/instructor/file")
@RequiredArgsConstructor
@Slf4j
public class FileProxyController {
    
    private final RestTemplate restTemplate;
    
    @Value("${file-service.url}")
    private String fileServiceUrl;
    
    /**
     * 파일 다운로드 프록시 API (fileKey 경로)
     * GET /api/instructor/file/download/filekey/{fileKey}
     */
    @GetMapping("/download/filekey/{fileKey}")
    public ResponseEntity<byte[]> downloadFileByKey(
            @PathVariable String fileKey,
            @RequestParam(required = false) String userId) {
        
        log.info("파일 다운로드 프록시 요청 - fileKey: {}, userId: {}", fileKey, userId);
        
        // 파일 백엔드로 요청 전달
        String downloadUrl = fileServiceUrl + "/download/filekey/" + fileKey;
        
        try {
            log.info("파일 서비스 호출: {}", downloadUrl);
            ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadUrl, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("파일 다운로드 성공 - 파일 크기: {} bytes", response.getBody().length);
                
                // Content-Type 헤더 설정
                String contentType = getContentType(fileKey);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileKey + "\"")
                    .body(response.getBody());
            } else {
                log.warn("파일 서비스에서 파일을 찾을 수 없습니다: {}", fileKey);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("파일 다운로드 프록시 오류: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 파일명에서 Content-Type을 추출하는 메서드
     */
    private String getContentType(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "application/octet-stream";
        }

        String extension = "";
        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }

        switch (extension) {
            case ".pdf": return "application/pdf";
            case ".png": return "image/png";
            case ".jpg": case ".jpeg": return "image/jpeg";
            case ".gif": return "image/gif";
            case ".bmp": return "image/bmp";
            case ".webp": return "image/webp";
            case ".mp4": return "video/mp4";
            case ".avi": return "video/x-msvideo";
            case ".mov": return "video/quicktime";
            case ".mp3": return "audio/mpeg";
            case ".wav": return "audio/wav";
            case ".doc": return "application/msword";
            case ".docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls": return "application/vnd.ms-excel";
            case ".xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt": return "application/vnd.ms-powerpoint";
            case ".pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".txt": return "text/plain";
            case ".zip": return "application/zip";
            case ".rar": return "application/x-rar-compressed";
            default: return "application/octet-stream";
        }
    }
} 