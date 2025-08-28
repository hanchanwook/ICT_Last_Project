package com.jakdang.labs.api.gemjjok.util;

import com.jakdang.labs.api.file.dto.FileEnum;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileTypeValidator {

    /**
     * 파일 타입 문자열을 검증하고 FileEnum으로 변환
     * @param type 파일 타입 문자열
     * @return FileEnum
     * @throws IllegalArgumentException 유효하지 않은 파일 타입인 경우
     */
    public static FileEnum validateAndConvert(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 타입은 필수입니다");
        }

        String normalizedType = type.trim().toLowerCase();
        
        if (!FileEnum.isValidType(normalizedType)) {
            // log.error("지원하지 않는 파일 타입: {}", type);
            throw new IllegalArgumentException("지원하지 않는 파일 타입입니다: " + type + 
                    ". 지원되는 타입: image, file, audio, video");
        }

        return FileEnum.fromString(normalizedType);
    }

    /**
     * 파일 타입별 추가 검증 로직
     * @param fileType FileEnum
     * @param fileName 파일명
     * @param fileSize 파일 크기
     */
    public static void validateFileTypeSpecific(FileEnum fileType, String fileName, Long fileSize) {
        if (fileType == null) {
            throw new IllegalArgumentException("파일 타입은 필수입니다");
        }

        switch (fileType) {
            case image:
                validateImageFile(fileName, fileSize);
                break;
            case video:
                validateVideoFile(fileName, fileSize);
                break;
            case audio:
                validateAudioFile(fileName, fileSize);
                break;
            case file:
                validateGeneralFile(fileName, fileSize);
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 파일 타입입니다: " + fileType);
        }
    }

    private static void validateImageFile(String fileName, Long fileSize) {
        // log.info("이미지 파일 검증: {}", fileName);
        
        // 파일 크기 제한 (예: 10MB)
        if (fileSize != null && fileSize > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 파일 크기는 10MB를 초과할 수 없습니다");
        }

        // 이미지 파일 확장자 검증
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (!lowerFileName.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                // log.warn("이미지 파일이 아닌 확장자: {}", fileName);
            }
        }
    }

    private static void validateVideoFile(String fileName, Long fileSize) {
        // log.info("비디오 파일 검증: {}", fileName);
        
        // 파일 크기 제한 (예: 100MB)
        if (fileSize != null && fileSize > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("비디오 파일 크기는 100MB를 초과할 수 없습니다");
        }

        // 비디오 파일 확장자 검증
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (!lowerFileName.matches(".*\\.(mp4|avi|mov|wmv|flv|webm|mkv)$")) {
                // log.warn("비디오 파일이 아닌 확장자: {}", fileName);
            }
        }
    }

    private static void validateAudioFile(String fileName, Long fileSize) {
        // log.info("오디오 파일 검증: {}", fileName);
        
        // 파일 크기 제한 (예: 50MB)
        if (fileSize != null && fileSize > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("오디오 파일 크기는 50MB를 초과할 수 없습니다");
        }

        // 오디오 파일 확장자 검증
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (!lowerFileName.matches(".*\\.(mp3|wav|aac|ogg|flac|m4a)$")) {
                // log.warn("오디오 파일이 아닌 확장자: {}", fileName);
            }
        }
    }

    private static void validateGeneralFile(String fileName, Long fileSize) {
        // log.info("일반 파일 검증: {}", fileName);
        
        // 파일 크기 제한 (예: 20MB)
        if (fileSize != null && fileSize > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("일반 파일 크기는 20MB를 초과할 수 없습니다");
        }

        // 위험한 파일 확장자 차단
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.matches(".*\\.(exe|bat|cmd|com|pif|scr|vbs|js|jar|war|ear)$")) {
                throw new IllegalArgumentException("실행 파일은 업로드할 수 없습니다: " + fileName);
            }
        }
    }
} 