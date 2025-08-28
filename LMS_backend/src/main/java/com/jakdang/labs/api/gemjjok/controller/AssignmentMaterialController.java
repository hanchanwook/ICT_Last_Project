package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.file.dto.FileEnum;
import com.jakdang.labs.api.gemjjok.DTO.AssignmentMaterialUploadRequest;
import com.jakdang.labs.api.gemjjok.entity.AssignmentFile;
import com.jakdang.labs.api.gemjjok.repository.AssignmentFileRepository;
import com.jakdang.labs.api.gemjjok.service.AssignmentMaterialService;
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
public class AssignmentMaterialController {

    private final AssignmentFileRepository assignmentFileRepository;
    private final AssignmentMaterialService assignmentMaterialService;
    private final RestTemplate restTemplate;
    
    @Value("${file-service.url}")
    private String fileServiceUrl;

    // 과제 자료 목록 조회 엔드포인트
    @GetMapping("/assignments/{assignmentId}/materials")
    public ResponseEntity<?> getAssignmentMaterials(
            @PathVariable(name = "assignmentId") String assignmentId,
            @RequestParam(value = "userId", required = false) String userId) {
        log.info("과제 자료 목록 조회 요청 - assignmentId: {}, userId: {}", assignmentId, userId);

        try {
            // 1. assignmentId 유효성 검증
            if (assignmentId == null || assignmentId.trim().isEmpty()) {
                log.error("과제 자료 목록 조회 실패: assignmentId가 비어있습니다.");
                return ResponseEntity.badRequest().body(Map.of(
                        "resultCode", 400,
                        "resultMessage", "과제 ID가 필요합니다."
                ));
            }
            
            // 2. userId 유효성 검증 (선택적)
            if (userId != null && userId.trim().isEmpty()) {
                log.warn("과제 자료 목록 조회: userId가 비어있습니다. assignmentId: {}", assignmentId);
            }
            
            // 3. 과제 자료 조회 (assignmentId를 String으로 사용)
            List<AssignmentFile> materials = assignmentFileRepository.findByAssignmentIdAndIsActive(assignmentId, 0); // isActive = 0 (활성 상태)
            log.info("과제 자료 목록 조회 성공 - 개수: {}", materials.size());

            // 5. 썸네일 URL이 비어있는 경우 생성 및 다운로드 URL 설정
            for (AssignmentFile material : materials) {
                if (material.getThumbnailUrl() == null || material.getThumbnailUrl().isEmpty()) {
                    String thumbnailUrl = generateThumbnailUrl(material.getFileKey(), material.getFileType());
                    material.setThumbnailUrl(thumbnailUrl);
                }
                // materialId를 사용한 다운로드 URL 설정 (임시로 썸네일 URL 필드에 저장)
                String downloadUrl = "/api/instructor/assignment/file/download/" + material.getMaterialId();
       
            }

            return ResponseEntity.ok().body(Map.of(
                    "resultCode", 200,
                    "resultMessage", "과제 자료 목록 조회 성공",
                    "data", materials
            ));

        } catch (IllegalArgumentException e) {
            log.error("과제 자료 목록 조회 실패: 잘못된 인자 - assignmentId: {}, error: {}", assignmentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 400,
                    "resultMessage", "과제 자료 목록 조회에 실패했습니다: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("과제 자료 목록 조회 중 예상치 못한 오류 발생: assignmentId: {}, error: {}", assignmentId, e.getMessage(), e);
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
     * 과제 자료 삭제 API
     * DELETE /api/instructor/assignments/{assignmentId}/materials/{materialId}
     */
    @DeleteMapping("/assignments/{assignmentId}/materials/{materialId}")
    public ResponseEntity<?> deleteAssignmentMaterial(
            @PathVariable(name = "assignmentId") String assignmentId,
            @PathVariable(name = "materialId") Long materialId) {
        
        log.info("과제 자료 삭제 요청 - assignmentId: {}, materialId: {}", assignmentId, materialId);

        try {
            // 1. assignment_files 테이블에서 레코드 조회
            AssignmentFile material = assignmentFileRepository.findById(materialId)
                    .orElseThrow(() -> new RuntimeException("과제 자료를 찾을 수 없습니다. materialId: " + materialId));

            // 2. assignmentId 일치 여부 확인 (assignmentId가 0인 경우는 무시)
            if (!"0".equals(assignmentId) && !assignmentId.equals(material.getAssignmentId())) {
                log.error("과제 ID 불일치 - 요청된 assignmentId: {}, 실제 assignmentId: {}", 
                        assignmentId, material.getAssignmentId());
                return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 400,
                    "resultMessage", "과제 ID가 일치하지 않습니다."
                ));
            }

            // 3. 논리적 삭제 (isActive = 1로 설정)
            material.setIsActive(1);
            assignmentFileRepository.save(material);
            log.info("과제 자료 논리적 삭제 완료 - materialId: {}", materialId);

            // 4. 파일 서비스에서 실제 파일 삭제 (선택적)
            try {
                String deleteUrl = fileServiceUrl + "/" + material.getMaterialId();
                log.info("파일 서비스에서 파일 삭제 시도: {}", deleteUrl);
                restTemplate.delete(deleteUrl);
                log.info("파일 서비스에서 파일 삭제 성공");
            } catch (Exception e) {
                log.warn("파일 서비스에서 파일 삭제 실패: {}", e.getMessage());
                // 파일 삭제 실패해도 메인 백엔드 삭제는 성공으로 처리
            }

            return ResponseEntity.ok().body(Map.of(
                "resultCode", 200,
                "resultMessage", "과제 자료가 성공적으로 삭제되었습니다."
            ));
            
        } catch (RuntimeException e) {
            log.error("과제 자료 삭제 실패 (자료를 찾을 수 없음): {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "resultCode", 404,
                "resultMessage", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("과제 자료 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "resultCode", 400,
                "resultMessage", "파일 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 파일 조회 프록시 API (파일 서비스로 요청 전달)
     * GET /api/instructor/assignment/file/{fileId}
     */
    @GetMapping("/assignment/file/{fileId}")
    public ResponseEntity<?> getAssignmentFile(@PathVariable("fileId") String fileId) {
        log.info("과제 파일 조회 프록시 요청 - fileId: {}", fileId);

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
     * POST /api/instructor/assignment/file/upload
     */
    @PostMapping("/assignment/file/upload")
    public ResponseEntity<?> uploadAssignmentFile(@RequestBody Map<String, Object> request) {
        log.info("과제 파일 업로드 프록시 요청 - request: {}", request);

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
     * materialId로 파일 다운로드 URL 생성 API
     * GET /api/instructor/assignment/file/download/url/{materialId}
     */
    @GetMapping("/assignment/file/download/url/{materialId}")
    public ResponseEntity<?> generateAssignmentFileDownloadUrl(@PathVariable("materialId") String materialId) {
        log.info("과제 파일 다운로드 URL 생성 요청 - materialId: {}", materialId);

        try {
            // materialId로 파일 정보 조회
            log.info("DB에서 materialId로 파일 정보 조회 시작 - materialId: {}", materialId);
            AssignmentFile material = assignmentFileRepository.findByMaterialId(materialId).orElse(null);
            
            if (material == null) {
                log.warn("materialId로 파일을 찾을 수 없습니다 - materialId: {}", materialId);
                return ResponseEntity.notFound().build();
            }

            log.info("파일 정보 조회 성공 - id: {}, fileName: {}, fileKey: {}, materialId: {}", 
                material.getId(), material.getFileName(), material.getFileKey(), material.getMaterialId());

            // materialId를 사용하여 파일 서비스 다운로드 URL로 리다이렉트
            String downloadUrl = "/api/instructor/assignment/file/download/" + material.getMaterialId();
            
            return ResponseEntity.ok().body(Map.of(
                "resultCode", 200,
                "resultMessage", "파일 다운로드 URL 생성 성공",
                "data", Map.of(
                    "downloadUrl", downloadUrl,
                    "materialId", material.getMaterialId(),
                    "fileName", material.getFileName(),
                    "fileKey", material.getFileKey(),
                    "fileSize", material.getFileSize(),
                    "fileType", material.getFileType()
                )
            ));
                
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
     * GET /api/instructor/assignment/file/download/material/{materialId}
     */
    @GetMapping("/assignment/file/download/material/{materialId}")
    public ResponseEntity<?> downloadAssignmentFileByMaterialId(
            @PathVariable("materialId") String materialId,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("materialId로 과제 파일 다운로드 요청 - materialId: {}, userId: {}", materialId, userId);

        try {
            // materialId로 파일 정보 조회
            log.info("DB에서 materialId로 파일 정보 조회 시작 - materialId: {}", materialId);
            AssignmentFile material = assignmentFileRepository.findByMaterialId(materialId).orElse(null);
            
            if (material == null) {
                log.warn("materialId로 파일을 찾을 수 없습니다 - materialId: {}", materialId);
                return ResponseEntity.notFound().build();
            }

            log.info("파일 정보 조회 성공 - id: {}, fileName: {}, fileKey: {}, materialId: {}", 
                material.getId(), material.getFileName(), material.getFileKey(), material.getMaterialId());

            // materialId를 사용하여 파일 서비스 다운로드 URL로 리다이렉트
            String downloadUrl = "/api/instructor/assignment/file/download/" + material.getMaterialId();
            
            return ResponseEntity.ok().body(Map.of(
                "resultCode", 200,
                "resultMessage", "파일 다운로드 정보",
                "data", Map.of(
                    "downloadUrl", downloadUrl,
                    "materialId", material.getMaterialId(),
                    "fileName", material.getFileName(),
                    "fileKey", material.getFileKey(),
                    "fileSize", material.getFileSize(),
                    "fileType", material.getFileType()
                )
            ));
                
        } catch (Exception e) {
            log.error("materialId로 파일 다운로드 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "resultCode", 500,
                "resultMessage", "파일 다운로드 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * fileKey로 파일 다운로드 API
     * GET /api/instructor/assignment/file/download/{fileKey}
     */
    // 프론트엔드 호출 패턴에 맞는 엔드포인트 추가
    @GetMapping("/assignments/materials/{materialId}/download")
    public ResponseEntity<?> downloadAssignmentMaterial(
            @PathVariable("materialId") String materialId,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("과제 자료 다운로드 요청 - materialId: {}, userId: {}", materialId, userId);
        
        try {
            // materialId로 AssignmentFile 조회
            AssignmentFile material = assignmentFileRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new RuntimeException("과제 자료를 찾을 수 없습니다. materialId: " + materialId));
            
            // 기존 다운로드 로직 재사용
            return downloadAssignmentFileByMaterialId(materialId, userId);
            
        } catch (Exception e) {
            log.error("과제 자료 다운로드 실패: materialId={}, error={}", materialId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "resultCode", 500,
                    "resultMessage", "과제 자료 다운로드에 실패했습니다: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/assignment/file/download/{fileKey}")
    public ResponseEntity<?> downloadAssignmentFile(
            @PathVariable("fileKey") String fileKey,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("과제 파일 다운로드 요청 - fileKey: {}, userId: {}", fileKey, userId);

        try {
            // fileKey로 파일 정보 조회
            log.info("DB에서 fileKey로 파일 정보 조회 시작 - fileKey: {}", fileKey);
            AssignmentFile material = assignmentFileRepository.findByFileKey(fileKey).orElse(null);
            
            String fileName = fileKey;
            if (material != null) {
                log.info("파일 정보 조회 성공 - id: {}, fileName: {}, fileKey: {}, materialId: {}", 
                    material.getId(), material.getFileName(), material.getFileKey(), material.getMaterialId());
                fileName = material.getFileName() != null ? material.getFileName() : fileKey;
            } else {
                log.warn("DB에서 파일을 찾을 수 없습니다 - fileKey: {}", fileKey);
                
                // 디버깅: 전체 파일 목록 조회
                List<AssignmentFile> allFiles = assignmentFileRepository.findAll();
                log.info("전체 파일 목록 ({}개):", allFiles.size());
                for (AssignmentFile file : allFiles) {
                    log.info("  - id: {}, fileKey: {}, fileName: {}", file.getId(), file.getFileKey(), file.getFileName());
                }
            }

            // 실제 파일 다운로드 처리
            log.info("파일 다운로드 직접 처리: {}", fileKey);
            
            if (material == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 파일 서비스에서 실제 파일 데이터 가져오기
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

    // 과제 자료 저장 엔드포인트
    @PostMapping("/assignments/{assignmentId}/materials")
    public ResponseEntity<?> uploadAssignmentMaterial(
            @PathVariable(name = "assignmentId") String assignmentId,
            @Valid @RequestBody AssignmentMaterialUploadRequest request,
            BindingResult bindingResult) {

        log.info("과제 자료 저장 요청 - assignmentId: {}, request: {}", assignmentId, request);

        // Validation 체크
        if (bindingResult.hasErrors()) {
            log.error("Validation 오류: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 400,
                    "resultMessage", "필수 필드가 누락되었습니다: " + bindingResult.getFieldError().getDefaultMessage()
            ));
        }

        try {
            // assignmentId가 0인 경우 현재 사용자의 최신 과제 ID로 설정
            String finalAssignmentId = assignmentId;
            if ("0".equals(assignmentId)) {
                // TODO: 현재 사용자의 최신 과제 ID 조회 로직 구현
                log.warn("assignmentId가 0입니다. 현재 사용자의 최신 과제 ID를 사용해야 합니다.");
                // 임시로 assignmentId를 그대로 사용 (나중에 실제 로직으로 교체)
            }

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
                    break;
                case video:
                    log.info("비디오 파일 처리: {}", request.getFileName());
                    break;
                case audio:
                    log.info("오디오 파일 처리: {}", request.getFileName());
                    break;
                case file:
                default:
                    log.info("일반 파일 처리: {}", request.getFileName());
                    break;
            }

            // 과제 자료 저장 로직 (AssignmentMaterialService 호출)
            Map<String, Object> uploadResult = assignmentMaterialService.uploadAssignmentMaterial(
                finalAssignmentId,
                request.getFileId(),
                request.getFileName(),
                request.getFileKey(),
                request.getFileSize(),
                request.getFileType(),
                request.getMemberId(),
                request.getTitle()
            );
            
            if ((Boolean) uploadResult.get("success")) {
                return ResponseEntity.ok().body(Map.of(
                        "resultCode", 200,
                        "resultMessage", "과제 자료가 성공적으로 저장되었습니다.",
                        "data", uploadResult.get("data")
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "resultCode", 400,
                        "resultMessage", uploadResult.get("message")
                ));
            }

        } catch (Exception e) {
            log.error("과제 자료 저장 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "resultCode", 500,
                    "resultMessage", "과제 자료 저장 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
} 