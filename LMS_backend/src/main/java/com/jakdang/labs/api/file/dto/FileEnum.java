package com.jakdang.labs.api.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum FileEnum {
    image("image"),
    file("file"),
    video("video"),
    audio("audio");

    private final String value;

    public static FileEnum fromString(String type) {
        if (type == null) {
            return file;
        }
        return Arrays.stream(values())
                .filter(fileEnum -> fileEnum.getValue().equals(type))
                .findFirst()
                .orElse(file);
    }

    public static boolean isValidType(String type) {
        if (type == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(fileEnum -> fileEnum.getValue().equals(type));
    }

    // File_Service와의 호환성을 위한 MIME 타입 변환
    public String toMimeType() {
        switch (this) {
            case image:
                return "image/jpeg"; // 기본 이미지 MIME 타입
            case video:
                return "video/mp4";  // 기본 비디오 MIME 타입
            case audio:
                return "audio/mpeg"; // 기본 오디오 MIME 타입
            case file:
            default:
                return "application/octet-stream"; // 기본 파일 MIME 타입
        }
    }

    // 파일 확장자로부터 MIME 타입 추정
    public String toMimeTypeByExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return toMimeType();
        }

        String lowerFileName = fileName.toLowerCase();
        
        switch (this) {
            case image:
                if (lowerFileName.endsWith(".png")) return "image/png";
                if (lowerFileName.endsWith(".gif")) return "image/gif";
                if (lowerFileName.endsWith(".bmp")) return "image/bmp";
                if (lowerFileName.endsWith(".webp")) return "image/webp";
                return "image/jpeg"; // 기본값
            case video:
                if (lowerFileName.endsWith(".avi")) return "video/x-msvideo";
                if (lowerFileName.endsWith(".mov")) return "video/quicktime";
                if (lowerFileName.endsWith(".wmv")) return "video/x-ms-wmv";
                if (lowerFileName.endsWith(".flv")) return "video/x-flv";
                if (lowerFileName.endsWith(".webm")) return "video/webm";
                if (lowerFileName.endsWith(".mkv")) return "video/x-matroska";
                return "video/mp4"; // 기본값
            case audio:
                if (lowerFileName.endsWith(".wav")) return "audio/wav";
                if (lowerFileName.endsWith(".aac")) return "audio/aac";
                if (lowerFileName.endsWith(".ogg")) return "audio/ogg";
                if (lowerFileName.endsWith(".flac")) return "audio/flac";
                if (lowerFileName.endsWith(".m4a")) return "audio/mp4";
                return "audio/mpeg"; // 기본값
            case file:
            default:
                if (lowerFileName.endsWith(".pdf")) return "application/pdf";
                if (lowerFileName.endsWith(".doc")) return "application/msword";
                if (lowerFileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                if (lowerFileName.endsWith(".xls")) return "application/vnd.ms-excel";
                if (lowerFileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                if (lowerFileName.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
                if (lowerFileName.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                if (lowerFileName.endsWith(".txt")) return "text/plain";
                if (lowerFileName.endsWith(".zip")) return "application/zip";
                if (lowerFileName.endsWith(".rar")) return "application/x-rar-compressed";
                return "application/octet-stream"; // 기본값
        }
    }

    // 기존 MIME 타입 호환성을 위한 메서드 (하위 호환성 유지)
    public static FileEnum fromContentType(String contentType) {
        if (contentType == null) {
            return file;
        }
        if (contentType.startsWith("image/")) {
            return image;
        } else if (contentType.startsWith("video/")) {
            return video;
        } else if (contentType.startsWith("audio/")) {
            return audio;
        } else {
            return file;
        }
    }
}
