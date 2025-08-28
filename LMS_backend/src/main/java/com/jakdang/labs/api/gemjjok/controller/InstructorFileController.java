package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.file.dto.FileEnum;
import com.jakdang.labs.api.file.dto.RequestFileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
@Slf4j
public class InstructorFileController {

    @Value("${file-service.url}")
    private String fileServiceUrl;

    private final RestTemplate restTemplate;


    // 1. Presigned URL 요청 프록시
    @GetMapping("/file/upload/{fileName}")
    public ResponseEntity<?> getPresignedUrl(
            @PathVariable("fileName") String fileName,
            @RequestParam("ownerId") String ownerId,
            @RequestParam("memberType") String memberType,
            @RequestParam("userId") String userId) {

        log.info("Presigned URL 요청 - fileName: {}, ownerId: {}, memberType: {}, userId: {}",
                fileName, ownerId, memberType, userId);

        try {
            String url = fileServiceUrl + "/v2/file/upload/" + fileName +
                        "?ownerId=" + ownerId +
                        "&memberType=" + memberType +
                        "&userId=" + userId;

            log.info("File_Service 호출 URL: {}", url);
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            log.info("File_Service 응답: {}", response.getBody());

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Presigned URL 요청 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(503)
                .body(ResponseDTO.createErrorResponse(503, "File_Service 연결 실패: " + e.getMessage()));
        }
    }

    // 2. 파일 업로드 완료 프록시
    @PostMapping("/file/upload/complete")
    public ResponseEntity<?> completeFileUpload(@RequestBody RequestFileDTO requestFileDTO) {

        log.info("=== 파일 업로드 완료 요청 시작 ===");
        log.info("RequestFileDTO: {}", requestFileDTO);
        log.info("파일명: {}", requestFileDTO.getName());
        log.info("파일 타입: {}", requestFileDTO.getType());
        log.info("파일 크기: {}", requestFileDTO.getSize());

        try {
            // File_Service 호환성을 위해 FileEnum을 MIME 타입으로 변환
            RequestFileDTO convertedRequest = convertRequestFileDTOForFileService(requestFileDTO);
            
            log.info("=== File_Service 호출 정보 ===");
            log.info("변환된 RequestFileDTO: {}", convertedRequest);
            log.info("변환된 파일 타입: {}", convertedRequest.getType());

            String url = fileServiceUrl + "/v2/upload/complete";
            log.info("File_Service 호출 URL: {}", url);

            ResponseEntity<Object> response = restTemplate.postForEntity(url, convertedRequest, Object.class);
            log.info("File_Service 응답 상태: {}", response.getStatusCode());
            log.info("File_Service 응답 데이터: {}", response.getBody());

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("=== 파일 업로드 완료 요청 실패 ===");
            log.error("예외 타입: {}", e.getClass().getSimpleName());
            log.error("예외 메시지: {}", e.getMessage());
            log.error("예외 상세: ", e);
            
            // HttpServerErrorException인 경우 응답 본문도 로깅
            if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                org.springframework.web.client.HttpServerErrorException httpError = 
                    (org.springframework.web.client.HttpServerErrorException) e;
                log.error("HTTP 에러 상태: {}", httpError.getStatusCode());
                log.error("HTTP 에러 응답 본문: {}", httpError.getResponseBodyAsString());
            }
            
            return ResponseEntity.status(503)
                .body(ResponseDTO.createErrorResponse(503, "File_Service 연결 실패: " + e.getMessage()));
        }
    }

    /**
     * File_Service 호환성을 위해 RequestFileDTO의 FileEnum을 MIME 타입으로 변환
     */
    private RequestFileDTO convertRequestFileDTOForFileService(RequestFileDTO originalRequest) {
        log.info("=== RequestFileDTO 변환 시작 ===");
        log.info("원본 RequestFileDTO: {}", originalRequest);
        
        // 임시 해결: File Service 호환성을 위해 모든 파일 타입을 'file'로 강제 변환
        log.info("File Service 호환성을 위해 파일 타입을 'file'로 강제 변환");
        
        // 새로운 RequestFileDTO 생성 (type 필드를 file로 강제 변환)
        return RequestFileDTO.builder()
                .id(originalRequest.getId())
                .name(originalRequest.getName())
                .type(FileEnum.file) // 모든 파일을 'file' 타입으로 처리
                .key(originalRequest.getKey())
                .address(originalRequest.getAddress())
                .thumbnailKey(originalRequest.getThumbnailKey())
                .isActive(originalRequest.isActive())
                .thumbnailAddress(originalRequest.getThumbnailAddress())
                .width(originalRequest.getWidth())
                .height(originalRequest.getHeight())
                .index(originalRequest.getIndex())
                .duration(originalRequest.getDuration())
                .size(originalRequest.getSize())
                .build();
    }
} 