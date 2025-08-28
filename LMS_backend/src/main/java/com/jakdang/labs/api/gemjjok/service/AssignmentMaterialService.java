package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.api.file.dto.FileEnum;
import com.jakdang.labs.api.gemjjok.entity.AssignmentFile;
import com.jakdang.labs.api.gemjjok.repository.AssignmentFileRepository;
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
public class AssignmentMaterialService {

    private final AssignmentFileRepository assignmentFileRepository;

    /**
     * 과제 자료 업로드 처리 (메타데이터만)
     * 1. assignment_files 테이블에 저장
     */
    @Transactional
    public Map<String, Object> uploadAssignmentMaterial(String assignmentId, String fileId, String fileName, 
                                                       String fileKey, Long fileSize, FileEnum fileType, String uploadedBy, String title) {
        log.info("과제 자료 메타데이터 업로드 시작 - assignmentId: {}, fileName: {}, fileType: {}, uploadedBy: {}",
                assignmentId, fileName, fileType, uploadedBy);

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

            // assignment_files 테이블에 저장
            AssignmentFile assignmentFile = AssignmentFile.builder()
                .assignmentId(assignmentId)
                .materialId(fileId)
                .fileKey(fileKey != null ? fileKey : fileId)
                .fileName(fileName)
                .title(title != null ? title : fileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .thumbnailUrl(generateThumbnailUrl(fileKey, fileType)) // 썸네일 URL 생성
                .uploadedBy(uploadedBy)
                .isActive(0) // 활성 상태
                .build();
            
            AssignmentFile savedAssignmentFile = assignmentFileRepository.save(assignmentFile);
            log.info("assignment_files 테이블 저장 성공 - id: {}", savedAssignmentFile.getId());

            // 응답 데이터 구성
            Map<String, Object> materialInfo = new HashMap<>();
            materialInfo.put("materialId", savedAssignmentFile.getMaterialId());
            materialInfo.put("fileId", savedAssignmentFile.getMaterialId());
            materialInfo.put("fileName", savedAssignmentFile.getFileName());
            materialInfo.put("title", savedAssignmentFile.getTitle());
            materialInfo.put("fileKey", savedAssignmentFile.getFileKey());
            materialInfo.put("fileSize", savedAssignmentFile.getFileSize());
            materialInfo.put("fileType", savedAssignmentFile.getFileType());
            materialInfo.put("uploadedBy", savedAssignmentFile.getUploadedBy());
            materialInfo.put("createdAt", savedAssignmentFile.getCreatedAt());
            
            result.put("success", true);
            result.put("message", "과제 자료 업로드가 완료되었습니다.");
            result.put("data", materialInfo);
            
        } catch (Exception e) {
            log.error("과제 자료 업로드 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "과제 자료 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 과제 자료 업로드 처리 (파일 직접 업로드)
     * 1. 파일을 S3 또는 로컬에 저장
     * 2. 과제 자료 메타데이터 저장
     * 3. assignment_files 테이블에 연결 정보 저장
     */
    @Transactional
    public Map<String, Object> uploadAssignmentMaterialWithFile(String assignmentId, MultipartFile file, 
                                                               String fileId, String fileKey, String title,
                                                               String fileName, Long fileSize, FileEnum fileType, 
                                                               String memberId, String memberType) {
        log.info("과제 자료 파일 업로드 시작 - assignmentId: {}, fileName: {}, memberId: {}",
                assignmentId, fileName, memberId);
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

            // assignment_files 테이블에 저장
            AssignmentFile assignmentFile = AssignmentFile.builder()
                .assignmentId(assignmentId)
                .materialId(finalFileId)
                .fileKey(finalFileKey)
                .fileName(fileName)
                .title(title != null ? title : fileName)
                .fileSize(file.getSize())
                .fileType(finalFileType)
                .thumbnailUrl(generateThumbnailUrl(finalFileKey, finalFileType)) // 썸네일 URL 생성
                .uploadedBy(memberId)
                .isActive(0) // 활성 상태
                .build();
            
            log.info("assignment_files 엔티티 생성 완료: {}", assignmentFile);
            
            AssignmentFile savedAssignmentFile = assignmentFileRepository.save(assignmentFile);
            log.info("assignment_files 테이블 저장 성공 - id: {}", savedAssignmentFile.getId());

            // 3. 응답 데이터 구성
            Map<String, Object> materialInfo = new HashMap<>();
            materialInfo.put("materialId", savedAssignmentFile.getMaterialId());
            materialInfo.put("fileId", savedAssignmentFile.getMaterialId());
            materialInfo.put("fileName", savedAssignmentFile.getFileName());
            materialInfo.put("fileKey", savedAssignmentFile.getFileKey());
            materialInfo.put("fileSize", savedAssignmentFile.getFileSize());
            materialInfo.put("fileType", savedAssignmentFile.getFileType());
            materialInfo.put("uploadedBy", savedAssignmentFile.getUploadedBy());
            materialInfo.put("createdAt", savedAssignmentFile.getCreatedAt());
            materialInfo.put("title", title);
            
            log.info("응답 데이터 구성 완료: {}", materialInfo);
            
            result.put("success", true);
            result.put("message", "과제 자료 업로드가 완료되었습니다.");
            result.put("data", materialInfo);
            
        } catch (Exception e) {
            log.error("과제 자료 업로드 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "과제 자료 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 과제 자료 목록 조회
     */
    public List<AssignmentFile> getAssignmentMaterials(String assignmentId) {
        log.info("과제 자료 목록 조회 - assignmentId: {}", assignmentId);
        return assignmentFileRepository.findByAssignmentIdAndIsActive(assignmentId, 0);
    }

    /**
     * 과제 자료 삭제 (논리적 삭제)
     */
    @Transactional
    public boolean deleteAssignmentMaterial(Long materialId) {
        log.info("과제 자료 삭제 - materialId: {}", materialId);
        
        try {
            AssignmentFile material = assignmentFileRepository.findById(materialId)
                    .orElseThrow(() -> new RuntimeException("과제 자료를 찾을 수 없습니다. materialId: " + materialId));
            
            material.setIsActive(1); // 비활성 상태로 변경
            assignmentFileRepository.save(material);
            
            log.info("과제 자료 삭제 완료 - materialId: {}", materialId);
            return true;
            
        } catch (Exception e) {
            log.error("과제 자료 삭제 중 오류 발생: {}", e.getMessage(), e);
            return false;
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

        // 파일 타입에 따른 썸네일 URL 생성
        switch (fileType) {
            case image:
                // 이미지 파일은 원본을 썸네일로 사용
                return fileKey;
            case video:
                // 비디오 파일은 썸네일 폴더에서 찾기
                String thumbnailKey = fileKey.replace(extension, "_thumb" + extension);
                return "thumbnails/" + thumbnailKey;
            default:
                // 문서나 기타 파일은 기본 아이콘 사용
                return "";
        }
    }
} 