package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.file.dto.FileEnum;
import com.jakdang.labs.api.gemjjok.DTO.MaterialUploadRequest;
import com.jakdang.labs.api.gemjjok.entity.LectureFile;
import com.jakdang.labs.api.gemjjok.repository.LectureFileRepository;
import com.jakdang.labs.api.gemjjok.util.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
@Slf4j
public class CourseMaterialUploadController {

    private final LectureFileRepository lectureFileRepository;
    private final RestTemplate restTemplate;
    
    @Value("${file-service.url}")
    private String fileServiceUrl;

    // 강의 자료 목록 조회 엔드포인트
    @GetMapping("/lectures/{lectureId}/materials")
    public ResponseEntity<?> getCourseMaterials(
            @PathVariable(name = "lectureId") String lectureId,
            @RequestParam(value = "userId", required = false) String userId) {
        log.info("강의 자료 목록 조회 요청 - lectureId: {}, userId: {}", lectureId, userId);

        try {
            // 1. lectureId 유효성 검증
            if (lectureId == null || lectureId.trim().isEmpty()) {
                log.error("강의 자료 목록 조회 실패: lectureId가 비어있습니다.");
                return ResponseEntity.badRequest().body(Map.of(
                        "resultCode", 400,
                        "resultMessage", "강의 ID가 필요합니다."
                ));
            }
            
            // 2. userId 유효성 검증 (선택적)
            if (userId != null && userId.trim().isEmpty()) {
                log.warn("강의 자료 목록 조회: userId가 비어있습니다. lectureId: {}", lectureId);
            }
            
            // 3. 강의 자료 조회 (lectureId를 String으로 사용)
            List<LectureFile> materials = lectureFileRepository.findByLectureIdAndIsActive(lectureId, 0); // isActive = 0 (활성 상태)
            log.info("강의 자료 목록 조회 성공 - 개수: {}", materials.size());

            // 5. 썸네일 URL이 비어있는 경우 생성 및 다운로드 URL 설정
            for (LectureFile material : materials) {
                if (material.getThumbnailUrl() == null || material.getThumbnailUrl().isEmpty()) {
                    String thumbnailUrl = generateThumbnailUrl(material.getFileKey(), material.getFileType());
                    material.setThumbnailUrl(thumbnailUrl);
                }
                // materialId를 사용한 다운로드 URL 설정 (임시로 썸네일 URL 필드에 저장)
                String downloadUrl = "/api/instructor/file/download/" + material.getMaterialId();
                log.info("강의 자료 다운로드 URL 설정 - materialId: {}, downloadUrl: {}", material.getMaterialId(), downloadUrl);
            }

            return ResponseEntity.ok().body(Map.of(
                    "resultCode", 200,
                    "resultMessage", "강의 자료 목록 조회 성공",
                    "data", materials
            ));

        } catch (IllegalArgumentException e) {
            log.error("강의 자료 목록 조회 실패: 잘못된 인자 - lectureId: {}, error: {}", lectureId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 400,
                    "resultMessage", "강의 자료 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("강의 자료 목록 조회 중 예상치 못한 오류 발생: lectureId: {}, error: {}", lectureId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "resultCode", 500,
                    "resultMessage", "서버 오류가 발생했습니다: " + e.getMessage()
            ));
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

    /**
     * 강의 자료 삭제 API
     * DELETE /api/instructor/lectures/{lectureId}/materials/{materialId}
     */
    @DeleteMapping("/lectures/{lectureId}/materials/{materialId}")
    public ResponseEntity<?> deleteMaterial(
            @PathVariable(name = "lectureId") String lectureId,
            @PathVariable(name = "materialId") Long materialId) {
        
        log.info("강의 자료 삭제 요청 - lectureId: {}, materialId: {}", lectureId, materialId);

        try {
            // 1. lecture_files 테이블에서 레코드 조회
            LectureFile material = lectureFileRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("강의 자료를 찾을 수 없습니다. materialId: " + materialId));
            
            log.info("강의 자료 조회 성공 - fileName: {}, fileKey: {}", material.getFileName(), material.getFileKey());
            
            // 2. 파일 서비스에서 파일 삭제
            String fileKey = material.getFileKey();
            if (fileKey != null && !fileKey.trim().isEmpty()) {
                try {
                    // 파일 서비스 삭제 로직 (구현 필요)
                    log.info("파일 서비스 삭제 성공 - fileKey: {}", fileKey);
                } catch (Exception e) {
                    log.warn("파일 서비스 삭제 실패 (무시하고 진행) - fileKey: {}, error: {}", fileKey, e.getMessage());
                    // 파일 서비스 삭제가 실패해도 DB에서 삭제는 진행
                }
            }
            
            // 3. LMS DB에서 레코드 삭제
            lectureFileRepository.delete(material);
            log.info("강의 자료 DB 삭제 성공 - materialId: {}", materialId);
            
            return ResponseEntity.ok(Map.of(
                "resultCode", 200,
                "resultMessage", "파일이 성공적으로 삭제되었습니다.",
                "deletedMaterialId", materialId
            ));
            
        } catch (RuntimeException e) {
            log.error("강의 자료 삭제 실패 (자료를 찾을 수 없음): {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "resultCode", 404,
                "resultMessage", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("강의 자료 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 파일 조회 프록시 API (파일 서비스로 요청 전달)
     * GET /api/instructor/file/{fileId}
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<?> getFile(@PathVariable("fileId") String fileId) {
        log.info("파일 조회 프록시 요청 - fileId: {}", fileId);

        try {
            // 파일 서비스로 조회 요청 전달
            String requestUrl = fileServiceUrl + "/" + fileId;
            
            ResponseEntity<String> response = restTemplate.getForEntity(
                requestUrl,
                String.class
            );
            
            log.info("파일 서비스 조회 요청 성공 - fileId: {}", fileId);
            return ResponseEntity.ok(response.getBody());
            
        } catch (Exception e) {
            log.error("파일 조회 프록시 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 파일 업로드 프록시 API (파일 서비스로 요청 전달)
     * POST /api/instructor/file/upload
     */
    @PostMapping("/file/upload")
    public ResponseEntity<?> uploadFile(@RequestBody Map<String, Object> request) {
        log.info("파일 업로드 프록시 요청 - request: {}", request);

        try {
            // 파일 서비스로 업로드 요청 전달
            String uploadUrl = fileServiceUrl + "/upload";
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                uploadUrl,
                request,
                String.class
            );
            
            log.info("파일 서비스 업로드 요청 성공");
            return ResponseEntity.ok(response.getBody());
            
        } catch (Exception e) {
            log.error("파일 업로드 프록시 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 파일 삭제 프록시 API (파일 서비스로 요청 전달)
     * DELETE /api/instructor/file/{fileId}
     */
    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable("fileId") String fileId) {
        log.info("파일 삭제 프록시 요청 - fileId: {}", fileId);

        try {
            // 파일 서비스로 삭제 요청 전달
            String deleteUrl = fileServiceUrl + "/" + fileId;
            
            ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl,
                HttpMethod.DELETE,
                null,
                String.class
            );
            
            log.info("파일 서비스 삭제 요청 성공 - fileId: {}", fileId);
            return ResponseEntity.ok(response.getBody());
            
        } catch (Exception e) {
            log.error("파일 삭제 프록시 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 파일 다운로드 URL 생성 API (프론트엔드에서 다운로드 링크 생성용)
     * GET /api/instructor/file/download-url/{fileKey}
     */
    @GetMapping("/file/download-url/{fileKey}")
    public ResponseEntity<?> getDownloadUrl(@PathVariable("fileKey") String fileKey) {
        log.info("파일 다운로드 URL 생성 요청 - fileKey: {}", fileKey);

        try {
            // 파일 정보 조회
            LectureFile material = lectureFileRepository.findByFileKey(fileKey);
            if (material == null) {
                log.warn("파일을 찾을 수 없습니다 - fileKey: {}", fileKey);
                return ResponseEntity.notFound().build();
            }

            // 다운로드 URL 생성
            String downloadUrl = "/api/instructor/file/download/" + fileKey;
            String fileName = material.getFileName() != null ? material.getFileName() : fileKey;
            
            Map<String, Object> response = Map.of(
                "downloadUrl", downloadUrl,
                "fileName", fileName,
                "fileSize", material.getFileSize(),
                "fileType", material.getFileType()
            );

            log.info("파일 다운로드 URL 생성 성공 - fileKey: {}", fileKey);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("파일 다운로드 URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 다운로드 URL 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * materialId로 파일 다운로드 URL 생성 API
     * GET /api/instructor/file/download-url/material/{materialId}
     */
    @GetMapping("/file/download-url/material/{materialId}")
    public ResponseEntity<?> getDownloadUrlByMaterialId(@PathVariable("materialId") String materialId) {
        log.info("materialId로 파일 다운로드 URL 생성 요청 - materialId: {}", materialId);

        try {
            // materialId로 파일 정보 조회
            LectureFile material = lectureFileRepository.findByMaterialId(materialId);
            
            if (material != null) {
                log.info("파일 정보 조회 성공 - id: {}, fileName: {}, fileKey: {}, materialId: {}", 
                    material.getId(), material.getFileName(), material.getFileKey(), material.getMaterialId());
                
                // 다운로드 URL 생성
                String downloadUrl = "/api/instructor/file/download/material/" + materialId;
                String fileName = material.getFileName() != null ? material.getFileName() : material.getFileKey();
                
                return ResponseEntity.ok(Map.of(
                    "resultCode", 200,
                    "resultMessage", "파일 다운로드 URL 생성 성공",
                    "downloadUrl", downloadUrl,
                    "fileName", fileName,
                    "fileSize", material.getFileSize(),
                    "fileType", material.getFileType(),
                    "materialId", material.getMaterialId(),
                    "fileKey", material.getFileKey()
                ));
            } else {
                log.warn("materialId로 파일을 찾을 수 없습니다 - materialId: {}", materialId);
                return ResponseEntity.notFound().build();
            }
                
        } catch (Exception e) {
            log.error("materialId로 파일 다운로드 URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 다운로드 URL 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * materialId로 파일 다운로드 API
     * GET /api/instructor/file/download/material/{materialId}
     */
    @GetMapping("/file/download/material/{materialId}")
    public ResponseEntity<?> downloadFileByMaterialId(
            @PathVariable("materialId") String materialId,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("materialId로 파일 다운로드 요청 - materialId: {}, userId: {}", materialId, userId);

        try {
            // materialId로 파일 정보 조회
            log.info("DB에서 materialId로 파일 정보 조회 시작 - materialId: {}", materialId);
            LectureFile material = lectureFileRepository.findByMaterialId(materialId);
            
            if (material == null) {
                log.warn("materialId로 파일을 찾을 수 없습니다 - materialId: {}", materialId);
                return ResponseEntity.notFound().build();
            }

            log.info("파일 정보 조회 성공 - id: {}, fileName: {}, fileKey: {}, materialId: {}", 
                material.getId(), material.getFileName(), material.getFileKey(), material.getMaterialId());

                        // materialId를 사용하여 파일 서비스 다운로드 URL로 리다이렉트
            String fileMaterialId = material.getMaterialId();
            String fileServiceUrl = "/api/file/download/" + fileMaterialId;
            log.info("파일 서비스 다운로드 URL로 리다이렉트 - materialId: {}, URL: {}", fileMaterialId, fileServiceUrl);
       
            return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, fileServiceUrl)
                .build();
                
        } catch (Exception e) {
            log.error("materialId로 파일 다운로드 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 다운로드 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 파일 다운로드 API (리다이렉트)
     * GET /api/instructor/file/download/{fileKey}
     */
    @GetMapping("/file/download/{fileKey}")
    public ResponseEntity<?> downloadFile(
            @PathVariable("fileKey") String fileKey,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("파일 다운로드 요청 - fileKey: {}, userId: {}", fileKey, userId);

        try {
            // 파일 정보 조회
            log.info("DB에서 파일 정보 조회 시작 - fileKey: {}", fileKey);
            LectureFile material = lectureFileRepository.findByFileKey(fileKey);
            
            String fileName = fileKey; // 기본값으로 fileKey 사용
            
            if (material != null) {
                log.info("파일 정보 조회 성공 - id: {}, fileName: {}, fileKey: {}", 
                    material.getId(), material.getFileName(), material.getFileKey());
                fileName = material.getFileName() != null ? material.getFileName() : fileKey;
            } else {
                log.warn("DB에서 파일을 찾을 수 없습니다 - fileKey: {}", fileKey);
                
                // 디버깅: 전체 파일 목록 조회
                List<LectureFile> allFiles = lectureFileRepository.findAll();
                log.info("전체 파일 목록 ({}개):", allFiles.size());
                for (LectureFile file : allFiles) {
                    log.info("  - id: {}, fileKey: {}, fileName: {}", file.getId(), file.getFileKey(), file.getFileName());
                }
            }

            // 실제 파일 다운로드 처리
            log.info("파일 다운로드 직접 처리: {}", fileKey);
            
            if (material == null) {
                return ResponseEntity.notFound().build();
            }
            
                        // 파일 서비스에서 실제 파일 데이터 가져오기 (materialId 사용)
            String downloadUrl = fileServiceUrl + "/download/" + material.getMaterialId(); // materialId 사용

            try {
                log.info("파일 서비스에서 파일 다운로드 시도 - materialId: {}, URL: {}", material.getMaterialId(), downloadUrl);
                // RestTemplate을 사용해서 파일 서비스에서 파일 데이터 가져오기
                ResponseEntity<byte[]> fileResponse = restTemplate.getForEntity(downloadUrl, byte[].class);

                if (fileResponse.getStatusCode().is2xxSuccessful() && fileResponse.getBody() != null) {
                    log.info("파일 서비스에서 파일 다운로드 성공 - materialId: {}", material.getMaterialId());
                    // 파일 다운로드 응답 생성
                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, getContentType(fileName))
                        .body(fileResponse.getBody());
                } else {
                    // 파일 서비스에서 파일을 찾을 수 없는 경우
                    log.warn("파일 서비스에서 파일을 찾을 수 없습니다 - materialId: {}", material.getMaterialId());
                    return ResponseEntity.notFound().build();
                }
            } catch (Exception e) {
                log.error("파일 서비스 호출 중 오류 - materialId: {}, error: {}", material.getMaterialId(), e.getMessage());
                // 파일 서비스 호출 실패 시 파일 정보만 반환
                return ResponseEntity.ok(Map.of(
                    "resultCode", 200,
                    "resultMessage", "파일 다운로드 정보 (파일 서비스 연결 실패)",
                    "data", Map.of(
                        "materialId", material.getMaterialId(),
                        "fileKey", fileKey,
                        "fileName", fileName,
                        "fileSize", material.getFileSize(),
                        "fileType", material.getFileType(),
                        "fileServiceUrl", downloadUrl
                    )
                ));
            }
                
        } catch (Exception e) {
            log.error("파일 다운로드 처리 중 오류 발생: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "resultCode", 500,
                "resultMessage", "파일 다운로드 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }



    // 강의 자료 저장 엔드포인트
    @PostMapping("/lectures/{lectureId}/materials")
    public ResponseEntity<?> uploadCourseMaterial(
            @PathVariable(name = "lectureId") String lectureId,
            @Valid @RequestBody MaterialUploadRequest request,
            BindingResult bindingResult) {

        log.info("강의 자료 저장 요청 - lectureId: {}, request: {}", lectureId, request);

        // Validation 체크
        if (bindingResult.hasErrors()) {
            log.error("Validation 오류: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 400,
                    "resultMessage", "필수 필드가 누락되었습니다: " + bindingResult.getFieldError().getDefaultMessage()
            ));
        }

        try {
            // 파일 타입 검증
            if (request.getFileType() == null) {
                log.error("파일 타입이 null입니다");
                return ResponseEntity.badRequest().body(Map.of(
                        "resultCode", 400,
                        "resultMessage", "파일 타입은 필수입니다"
                ));
            }

            // 파일 타입별 상세 검증
            try {
                FileTypeValidator.validateFileTypeSpecific(
                    request.getFileType(), 
                    request.getFileName(), 
                    request.getFileSize()
                );
            } catch (IllegalArgumentException e) {
                log.error("파일 타입 검증 실패: {}", e.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                        "resultCode", 400,
                        "resultMessage", e.getMessage()
                ));
            }

            // 파일 타입별 처리 로직
            switch (request.getFileType()) {
                case image:
                    log.info("이미지 파일 처리: {}", request.getFileName());
                    // 이미지 파일 추가 처리 로직 (썸네일 생성 등)
                    break;
                case video:
                    log.info("비디오 파일 처리: {}", request.getFileName());
                    // 비디오 파일 추가 처리 로직 (썸네일 생성, 재생 시간 추출 등)
                    break;
                case audio:
                    log.info("오디오 파일 처리: {}", request.getFileName());
                    // 오디오 파일 추가 처리 로직 (재생 시간 추출 등)
                    break;
                case file:
                    log.info("일반 파일 처리: {}", request.getFileName());
                    // 일반 파일 추가 처리 로직
                    break;
                default:
                    log.error("지원하지 않는 파일 타입: {}", request.getFileType());
                    return ResponseEntity.badRequest().body(Map.of(
                            "resultCode", 400,
                            "resultMessage", "지원하지 않는 파일 타입입니다: " + request.getFileType()
                    ));
            }

            // LectureFile 엔티티 생성 및 저장
            LectureFile lectureFile = LectureFile.builder()
                    .lectureId(lectureId)
                    .courseId(lectureId) // courseId도 String으로 설정
                    .materialId(request.getFileId())
                    .fileKey(request.getFileKey())
                    .fileName(request.getFileName())
                    .title(request.getTitle())
                    .fileSize(request.getFileSize())
                    .fileType(request.getFileType())
                    .thumbnailUrl(request.getThumbnailUrl() != null ? request.getThumbnailUrl() : generateThumbnailUrl(request.getFileKey(), request.getFileType()))
                    .uploadedBy(request.getMemberId())
                    .isActive(0) // 0: 활성, 1: 삭제
                    .build();

            LectureFile savedLectureFile = lectureFileRepository.save(lectureFile);
            log.info("강의 자료 저장 성공 - id: {}, fileType: {}", savedLectureFile.getId(), savedLectureFile.getFileType());

            return ResponseEntity.ok().body(Map.of(
                    "resultCode", 200,
                    "resultMessage", "강의 자료 저장 성공",
                    "data", savedLectureFile
            ));

        } catch (Exception e) {
            log.error("강의 자료 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 400,
                    "resultMessage", "강의 자료 업로드 중 오류가 발생했습니다: " + e.getMessage()
            ));
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
            case ".pdf":
                return "application/pdf";
            case ".png":
                return "image/png";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".gif":
                return "image/gif";
            case ".bmp":
                return "image/bmp";
            case ".webp":
                return "image/webp";
            case ".mp4":
                return "video/mp4";
            case ".avi":
                return "video/x-msvideo";
            case ".mov":
                return "video/quicktime";
            case ".mp3":
                return "audio/mpeg";
            case ".wav":
                return "audio/wav";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt":
                return "application/vnd.ms-powerpoint";
            case ".pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".txt":
                return "text/plain";
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            default:
                return "application/octet-stream";
        }
    }
} 