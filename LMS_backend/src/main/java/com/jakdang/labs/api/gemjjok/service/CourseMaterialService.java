package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.api.file.dto.FileEnum;
import com.jakdang.labs.api.gemjjok.DTO.CourseMaterialRequest;
import com.jakdang.labs.api.gemjjok.entity.LectureFile;
import com.jakdang.labs.api.gemjjok.repository.LectureFileRepository;
import com.jakdang.labs.api.gemjjok.util.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseMaterialService {

    private final LectureFileRepository lectureFileRepository;

    /**
     * 강의 자료 업로드 처리 (메타데이터만)
     * 1. lecture_files 테이블에 저장
     */
    @Transactional
    public Map<String, Object> uploadCourseMaterial(String courseId, String fileId, String fileName, 
                                                   String fileKey, Long fileSize, FileEnum fileType, String uploadedBy, String title) {
        log.info("강의 자료 메타데이터 업로드 시작 - courseId: {}, fileName: {}, fileType: {}, uploadedBy: {}",
                courseId, fileName, fileType, uploadedBy);

        Map<String, Object> result = new HashMap<>();

        try {
            // 파일 타입 검증
            if (fileType == null) {
                log.error("파일 타입이 null입니다");
                result.put("success", false);
                result.put("message", "파일 타입은 필수입니다");
                return result;
            }

            // 파일 타입별 상세 검증
            try {
                FileTypeValidator.validateFileTypeSpecific(fileType, fileName, fileSize);
            } catch (IllegalArgumentException e) {
                log.error("파일 타입 검증 실패: {}", e.getMessage());
                result.put("success", false);
                result.put("message", e.getMessage());
                return result;
            }

            // lecture_files 테이블에 저장
            LectureFile lectureFile = LectureFile.builder()
                .lectureId(courseId)  // courseId를 lectureId로 변환 (String)
                .courseId(courseId)
                .materialId(fileId)
                .fileKey(fileKey != null ? fileKey : fileId)
                .fileName(fileName)
                .title(title != null ? title : fileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .thumbnailUrl(generateThumbnailUrl(fileKey, fileType)) // 썸네일 URL 생성
                .uploadedBy(uploadedBy)
                .isActive(1)
                .build();
            
            LectureFile savedLectureFile = lectureFileRepository.save(lectureFile);
            log.info("lecture_files 테이블 저장 성공 - id: {}", savedLectureFile.getId());

            // 응답 데이터 구성
            Map<String, Object> materialInfo = new HashMap<>();
            materialInfo.put("materialId", savedLectureFile.getMaterialId());
            materialInfo.put("fileId", savedLectureFile.getMaterialId());
            materialInfo.put("fileName", savedLectureFile.getFileName());
            materialInfo.put("title", savedLectureFile.getTitle());
            materialInfo.put("fileKey", savedLectureFile.getFileKey());
            materialInfo.put("fileSize", savedLectureFile.getFileSize());
            materialInfo.put("fileType", savedLectureFile.getFileType());
            materialInfo.put("uploadedBy", savedLectureFile.getUploadedBy());
            materialInfo.put("createdAt", savedLectureFile.getCreatedAt());
            
            result.put("success", true);
            result.put("message", "강의 자료 업로드가 완료되었습니다.");
            result.put("data", materialInfo);
            
        } catch (Exception e) {
            log.error("강의 자료 업로드 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "강의 자료 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 강의 자료 업로드 처리 (파일 직접 업로드)
     * 1. 파일을 S3 또는 로컬에 저장
     * 2. 강의 자료 메타데이터 저장
     * 3. lecture_files 테이블에 연결 정보 저장
     */
    @Transactional
    public Map<String, Object> uploadCourseMaterialWithFile(String courseId, MultipartFile file, 
                                                           String fileId, String fileKey, String title,
                                                           String fileName, Long fileSize, FileEnum fileType, 
                                                           String memberId, String memberType) {
        log.info("강의 자료 파일 업로드 시작 - courseId: {}, fileName: {}, memberId: {}",
                courseId, fileName, memberId);
        log.info("파일 정보 - originalFilename: {}, size: {}, contentType: {}, isEmpty: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType(), file.isEmpty());
        log.info("파라미터 정보 - fileId: {}, fileKey: {}, title: {}, fileType: {}, memberType: {}", 
                fileId, fileKey, title, fileType, memberType);

        Map<String, Object> result = new HashMap<>();

        try {
            // 파일 검증
            if (file == null) {
                log.error("파일이 null입니다.");
                result.put("success", false);
                result.put("message", "업로드할 파일이 없습니다.");
                return result;
            }

            if (file.isEmpty()) {
                log.error("파일이 비어있습니다. - originalFilename: {}, size: {}", 
                        file.getOriginalFilename(), file.getSize());
                result.put("success", false);
                result.put("message", "업로드할 파일이 비어있습니다.");
                return result;
            }

            // 파일 크기 검증 (100MB 제한)
            if (file.getSize() > 100 * 1024 * 1024) {
                log.error("파일 크기가 너무 큽니다: {} bytes", file.getSize());
                result.put("success", false);
                result.put("message", "파일 크기는 100MB를 초과할 수 없습니다.");
                return result;
            }

            // 파일명 검증
            if (fileName == null || fileName.trim().isEmpty()) {
                log.error("파일명이 null이거나 비어있습니다.");
                result.put("success", false);
                result.put("message", "유효하지 않은 파일명입니다.");
                return result;
            }

            log.info("파일 검증 완료 - fileName: {}, fileSize: {}, contentType: {}",
                    fileName, file.getSize(), file.getContentType());

            // 파일 타입 결정 (파라미터로 받은 fileType이 null이면 파일 확장자로 추정)
            FileEnum finalFileType = fileType;
            if (finalFileType == null) {
                finalFileType = FileEnum.fromContentType(file.getContentType());
                log.info("파일 타입을 ContentType으로 추정: {} -> {}", file.getContentType(), finalFileType);
            }

            // 파일 타입별 상세 검증
            try {
                FileTypeValidator.validateFileTypeSpecific(finalFileType, fileName, file.getSize());
            } catch (IllegalArgumentException e) {
                log.error("파일 타입 검증 실패: {}", e.getMessage());
                result.put("success", false);
                result.put("message", e.getMessage());
                return result;
            }

            // 파일 키 결정
            String finalFileKey = fileKey;
            if (finalFileKey == null || finalFileKey.trim().isEmpty()) {
                finalFileKey = fileId;
                log.info("파일 키를 fileId로 설정: {}", finalFileKey);
            }

            // 파일 ID 결정
            String finalFileId = fileId;
            if (finalFileId == null || finalFileId.trim().isEmpty()) {
                finalFileId = java.util.UUID.randomUUID().toString();
                log.info("파일 ID를 UUID로 생성: {}", finalFileId);
            }

            // lecture_files 테이블에 저장
            LectureFile lectureFile = LectureFile.builder()
                .lectureId(courseId)  // courseId를 lectureId로 변환 (String)
                .courseId(courseId)
                .materialId(finalFileId)
                .fileKey(finalFileKey)
                .fileName(fileName)
                .title(title != null ? title : fileName)
                .fileSize(file.getSize())
                .fileType(finalFileType)
                .thumbnailUrl(generateThumbnailUrl(finalFileKey, finalFileType)) // 썸네일 URL 생성
                .uploadedBy(memberId)
                .isActive(1)
                .build();
            
            log.info("lecture_files 엔티티 생성 완료: {}", lectureFile);
            
            LectureFile savedLectureFile = lectureFileRepository.save(lectureFile);
            log.info("lecture_files 테이블 저장 성공 - id: {}", savedLectureFile.getId());

            // 3. 응답 데이터 구성
            Map<String, Object> materialInfo = new HashMap<>();
            materialInfo.put("materialId", savedLectureFile.getMaterialId());
            materialInfo.put("fileId", savedLectureFile.getMaterialId());
            materialInfo.put("fileName", savedLectureFile.getFileName());
            materialInfo.put("fileKey", savedLectureFile.getFileKey());
            materialInfo.put("fileSize", savedLectureFile.getFileSize());
            materialInfo.put("fileType", savedLectureFile.getFileType());
            materialInfo.put("uploadedBy", savedLectureFile.getUploadedBy());
            materialInfo.put("createdAt", savedLectureFile.getCreatedAt());
            materialInfo.put("title", title);
            
            log.info("응답 데이터 구성 완료: {}", materialInfo);
            
            result.put("success", true);
            result.put("message", "강의 자료 업로드가 완료되었습니다.");
            result.put("data", materialInfo);
            
        } catch (Exception e) {
            log.error("강의 자료 업로드 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "강의 자료 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 강의별 자료 목록 조회
     */
    public List<LectureFile> getCourseMaterials(String courseId) {
        log.info("강의 자료 목록 조회 - courseId: {}", courseId);
        
        // lecture_files 테이블에서 조회
        List<LectureFile> materials = lectureFileRepository.findByLectureId(courseId);
        
        log.info("강의 자료 조회 결과 - lecture_files: {}개", 
                materials.size());
        
        return materials;
    }
    
    /**
     * 강의 자료 삭제 (논리적 삭제)
     */
    @Transactional
    public boolean deleteCourseMaterial(String courseId, String materialId) {
        log.info("강의 자료 삭제 - courseId: {}, materialId: {}", courseId, materialId);
        
        try {
            LectureFile material = lectureFileRepository.findByMaterialId(materialId);
            
            if (material == null) {
                log.warn("강의 자료를 찾을 수 없습니다 - materialId: {}", materialId);
                return false;
            }
            
            if (!material.getLectureId().equals(courseId)) {
                log.warn("강의 ID가 일치하지 않습니다 - expected: {}, actual: {}", courseId, material.getLectureId());
                return false;
            }
            
            // 논리적 삭제 (isActive = 0)
            material.setIsActive(0);
            lectureFileRepository.save(material);
            
            log.info("강의 자료 삭제 완료 - materialId: {}", materialId);
            return true;
            
        } catch (Exception e) {
            log.error("강의 자료 삭제 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 파일 확장자로부터 FileEnum 추정 (하위 호환성을 위한 메서드)
     */
    private FileEnum getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return FileEnum.file;
        }
        
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            return FileEnum.image;
        } else if (lowerFileName.matches(".*\\.(mp4|avi|mov|wmv|flv|webm|mkv)$")) {
            return FileEnum.video;
        } else if (lowerFileName.matches(".*\\.(mp3|wav|aac|ogg|flac|m4a)$")) {
            return FileEnum.audio;
        } else {
            return FileEnum.file;
        }
    }

    /**
     * 파일 키와 파일 타입을 받아 썸네일 URL을 생성하는 메서드
     */
    private String generateThumbnailUrl(String fileKey, FileEnum fileType) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return "";
        }

        // 파일 확장자 추출
        String extension = "";
        if (fileKey.contains(".")) {
            extension = fileKey.substring(fileKey.lastIndexOf(".")).toLowerCase();
        }

        // 이미지 파일인 경우 원본 파일을 썸네일로 사용
        if (fileType == FileEnum.image) {
            // 이미지 파일은 원본을 썸네일로 사용
            return "https://lmsync-file-bucket.s3.ap-northeast-2.amazonaws.com/" + fileKey;
        } else if (fileType == FileEnum.video) {
            // 비디오 파일은 썸네일 폴더에서 찾기
            String thumbnailKey = fileKey.replace(extension, "_thumb" + extension);
            return "https://lmsync-file-bucket.s3.ap-northeast-2.amazonaws.com/thumbnails/" + thumbnailKey;
        } else {
            // 문서나 기타 파일은 기본 아이콘 사용
            return "";
        }
    }
} 