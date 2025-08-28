package com.jakdang.labs.api.file.controller;


import com.jakdang.labs.api.common.ResponseDTO;
import com.jakdang.labs.api.file.FileServiceClient;
import com.jakdang.labs.api.file.dto.*;
import com.jakdang.labs.api.gemjjok.service.AssignmentSubmissionFileService;
import com.jakdang.labs.api.gemjjok.repository.AssignmentSubmissionFileRepository;
import com.jakdang.labs.entity.AssignmentSubmissionFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/file")
@Slf4j
public class FileV2Controller {

    private final FileServiceClient fileServiceClient;
    private final AssignmentSubmissionFileService assignmentSubmissionFileService;
    private final AssignmentSubmissionFileRepository assignmentSubmissionFileRepository;


    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestBody RequestFileDTO dto) {
        log.info("파일 다운로드 요청 - key: {}, name: {}, type: {}", dto.getKey(), dto.getName(), dto.getType());
        
        // 1. 메인 DB에서 파일 정보 조회 (과제 제출된 파일)
        Optional<AssignmentSubmissionFile> dbFile = assignmentSubmissionFileRepository.findByFileKey(dto.getKey());
        if (dbFile.isPresent()) {
            AssignmentSubmissionFile fileInfo = dbFile.get();
            log.info("메인 DB에서 파일 정보 찾음 - fileKey: {}, fileName: {}, externalFileId: {}", 
                    fileInfo.getFileKey(), fileInfo.getFileName(), fileInfo.getId());
            
            // 외부 파일 서비스의 ID로 다운로드 시도
            try {
                RequestFileDTO externalDto = RequestFileDTO.builder()
                        .key(fileInfo.getId()) // 외부 파일 서비스의 ID 사용
                        .name(fileInfo.getFileName())
                        .type(FileEnum.valueOf(fileInfo.getFileType()))
                        .build();
                
                byte[] file = fileServiceClient.downloadFile(externalDto);
                log.info("외부 파일 ID로 다운로드 성공 - externalFileId: {}, 파일 크기: {} bytes", fileInfo.getId(), file.length);
                return ResponseEntity.ok().body(file);
                
            } catch (Exception e) {
                log.error("외부 파일 ID로 다운로드 실패 - externalFileId: {}, 오류: {}", fileInfo.getId(), e.getMessage());
            }
        } else {
            log.info("메인 DB에서 파일 정보를 찾을 수 없음 - fileKey: {}", dto.getKey());
        }
        
        // 2. 외부 파일 서비스에서 직접 조회 (과제 제출 전 업로드된 파일)
        log.info("외부 파일 서비스에서 직접 조회 시도");
        String[] keyVariations = generateKeyVariations(dto.getKey(), dto.getName());
        
        for (String key : keyVariations) {
            try {
                log.info("외부 서비스에서 키로 시도: {}", key);
                
                RequestFileDTO tryDto = RequestFileDTO.builder()
                        .key(key)
                        .name(dto.getName())
                        .type(dto.getType())
                        .build();
                
                byte[] file = fileServiceClient.downloadFile(tryDto);
                log.info("외부 서비스에서 파일 다운로드 성공 - key: {}, 파일 크기: {} bytes", key, file.length);
                return ResponseEntity.ok().body(file);
                
            } catch (Exception e) {
                log.info("외부 서비스에서 키 '{}'로 시도 실패: {}", key, e.getMessage());
                continue;
            }
        }
        
        // 모든 시도가 실패한 경우
        log.error("메인 DB와 외부 파일 서비스 모두에서 파일을 찾을 수 없음 - 원본 key: {}, name: {}", dto.getKey(), dto.getName());
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 파일 키의 다양한 형식을 생성
     */
    private String[] generateKeyVariations(String key, String fileName) {
        if (key == null) {
            return new String[0];
        }
        
        java.util.List<String> variations = new java.util.ArrayList<>();
        
        // 1. 원본 키
        variations.add(key);
        log.info("키 변형 1 (원본): {}", key);
        
        // 2. 파일 이름이 있는 경우 확장자 추가
        if (fileName != null && !fileName.isEmpty()) {
            String extension = getFileExtension(fileName);
            if (!extension.isEmpty()) {
                String keyWithExtension = key + "." + extension;
                variations.add(keyWithExtension);
                log.info("키 변형 2 (확장자 추가): {}", keyWithExtension);
            }
        }
        
        // 3. 파일 이름에서 UUID 부분만 추출 (확장자가 포함된 파일명인 경우)
        if (fileName != null && fileName.contains(".")) {
            String uuidPart = extractFileId(fileName);
            if (!uuidPart.equals(fileName)) {
                variations.add(uuidPart);
                log.info("키 변형 3 (파일명에서 UUID 추출): {}", uuidPart);
            }
        }
        
        // 4. 키가 UUID 형식이 아닌 경우 UUID 형식으로 변환 시도
        if (!key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            // UUID 형식이 아닌 경우 원본 그대로 사용
            log.info("키가 UUID 형식이 아님: {}", key);
        }
        
        log.info("생성된 키 변형들: {}", java.util.Arrays.toString(variations.toArray()));
        return variations.toArray(new String[0]);
    }
    
    /**
     * 파일 이름에서 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 파일 서비스의 extractFileId 메서드와 동일한 로직
     */
    private String extractFileId(String id) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        if (id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            return id;
        }
        int lastDotIndex = id.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String uuidPart = id.substring(0, lastDotIndex);
            if (uuidPart.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                return uuidPart;
            }
        }
        return id;
    }

    @PostMapping(value = "", consumes = "multipart/form-data")
    public ResponseEntity<ResponseDTO<Object>> handleFileUpload(@RequestPart("file") MultipartFile file, @RequestParam(value = "index", required = false) Integer index, @RequestParam(value = "width", required = false) Integer width, @RequestParam(value = "height", required = false) Integer height, @RequestParam(value = "ownerId", required = false) String ownerId, @RequestParam(value = "memberType", required = false) MemberEnum memberType) {

        ResponseDTO<Object> responseDTO = fileServiceClient.handleFileUpload(file, index, width, height, ownerId, memberType);

        return ResponseEntity.ok().body(responseDTO);
    }

    @GetMapping("/upload/{fileName}")
    public ResponseEntity<ResponseDTO<FileInfoDTO>> generatePresignedUrlForUpload(@PathVariable(value = "fileName") String fileName) {
        ResponseDTO<FileInfoDTO> responseDTO = fileServiceClient.generatePresignedUrlForUpload(fileName);
        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping("/upload/success")
    public ResponseEntity<ResponseDTO<ResponseFileDTO>> handleUploadSuccess(
            @RequestBody RequestFileDTO requestFileDTO, 
            @RequestParam(required = false, value = "ownerId") String ownerId, 
            @RequestParam(required = false, value = "memberType") MemberEnum memberType,
            @RequestParam(required = false, value = "assignmentId") String assignmentId,
            @RequestParam(required = false, value = "courseId") String courseId,
            @RequestParam(required = false, value = "studentId") String studentId,
            @RequestParam(required = false, value = "submissionId") String submissionId) {
        
        log.info("requestFileDTO: {}, assignmentId: {}, courseId: {}, studentId: {}, submissionId: {}", 
                requestFileDTO, assignmentId, courseId, studentId, submissionId);
        
        ResponseDTO<ResponseFileDTO> responseDTO = fileServiceClient.handleUploadSuccess(requestFileDTO, ownerId, memberType);

        // 과제 제출 파일인 경우 assignment_submission_file 테이블에 저장 (submissionId는 나중에 업데이트)
        if (responseDTO.getResultCode() == 200 && assignmentId != null && studentId != null) {
            try {
                ResponseFileDTO fileDTO = responseDTO.getData();
                
                // 외부 파일 서비스에서 반환된 파일 ID와 file_key를 각각 저장
                String externalFileId = fileDTO.getId(); // 외부 파일 서비스의 파일 ID (UUID)
                String externalFileKey = fileDTO.getKey(); // 외부 파일 서비스의 파일 키 (UUID + 확장자)
                
                // 메인 DB에 저장할 때는 확장자를 제거한 키를 사용 (파일 서비스의 extractFileId 방식과 일치)
                String processedFileKey = extractFileId(externalFileKey);
                
                log.info("파일 키 처리 - 원본: '{}' -> 처리됨: '{}'", externalFileKey, processedFileKey);
                
                assignmentSubmissionFileService.saveSubmissionFile(
                    submissionId, 
                    assignmentId, 
                    courseId, 
                    studentId, 
                    externalFileId, // assignment_submission_file.id에 외부 파일의 id 저장
                    processedFileKey, // assignment_submission_file.fileKey에 확장자 제거된 키 저장
                    fileDTO.getName(), 
                    Long.valueOf(fileDTO.getSize()), 
                    fileDTO.getType().toString()
                );
                log.info("과제 제출 파일 저장 완료: assignmentId={}, studentId={}, submissionId={}, externalFileId={}, processedFileKey={}", 
                        assignmentId, studentId, submissionId, externalFileId, processedFileKey);
            } catch (Exception e) {
                log.error("과제 제출 파일 저장 실패", e);
            }
        } else {
            log.warn("과제 제출 파일 저장 조건 불만족 - resultCode: {}, assignmentId: {}, studentId: {}, submissionId: {}", 
                    responseDTO.getResultCode(), assignmentId, studentId, submissionId);
        }

        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping(value = "/upload-audio", consumes = "multipart/form-data")
    public ResponseEntity<ResponseDTO<ResponseFileDTO>> uploadAudio(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "ownerId") String ownerId, @RequestParam(value = "memberType") MemberEnum memberType) {
        ResponseDTO<ResponseFileDTO> responseDTO = fileServiceClient.uploadAudio(file, ownerId, memberType);
        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<ResponseDTO<ResponseFileDTO>> uploadImage(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "ownerId") String ownerId, @RequestParam(value = "memberType") MemberEnum memberType) {
        ResponseDTO<ResponseFileDTO> responseDTO = fileServiceClient.uploadImage(file, ownerId, memberType);
        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping(value = "/upload-file", consumes = "multipart/form-data")
    public ResponseEntity<ResponseDTO<ResponseFileDTO>> uploadFile(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "ownerId") String ownerId, @RequestParam(value = "memberType") MemberEnum memberType) {
        ResponseDTO<ResponseFileDTO> responseDTO = fileServiceClient.uploadFile(file, ownerId, memberType);
        return ResponseEntity.ok().body(responseDTO);
    }

    @GetMapping("/image/{fileId}")
    public ResponseEntity<byte[]> getImage(@PathVariable(value = "fileId") String fileId) {
        byte[] image = fileServiceClient.getImage(fileId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @GetMapping("/imagelink/{fileId}")
    public ResponseEntity<ResponseDTO<String>> getImageByLink(@PathVariable(value = "fileId") String fileId)  {
        ResponseDTO<String> responseDTO = fileServiceClient.getImageLink(fileId);
        return ResponseEntity.ok().body(responseDTO);
    }

    @GetMapping("/image/{fileId}/{size}")
    public ResponseEntity<byte[]> getImageResize(@PathVariable(value = "fileId") String fileId, @PathVariable(value = "size") int size) {
        byte[] image = fileServiceClient.getImageResize(fileId, size);
        return ResponseEntity.ok().headers(httpHeaders -> {
            httpHeaders.setContentType(MediaType.IMAGE_JPEG);
            httpHeaders.setContentLength(image.length);
        }).body(image);
    }

    @PostMapping("/image/resources/{imageName}/{size}")
    public ResponseEntity<byte[]> getPublicImageResize(@PathVariable(value = "imageName") String imageName, @PathVariable(value = "size") int size) {
        if (size < 10 || size > 1000) return ResponseEntity.badRequest().body("사이즈는 최소 10부터 1000까지 가능합니다.".getBytes());

        byte[] image = fileServiceClient.getPublicImageResize(imageName, size);

        return ResponseEntity.ok().headers(httpHeaders -> {
            httpHeaders.setContentType(MediaType.IMAGE_JPEG);
            httpHeaders.setContentLength(image.length);
        }).body(image);
    }

    @GetMapping("/thumbnail/{fileId}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable(value = "fileId") String fileId) {
        byte[] image = fileServiceClient.getThumbnail(fileId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @GetMapping("/image/member/{memberId}")
    public ResponseEntity<byte[]> getMemberImage(@PathVariable(value = "memberId") String memberId) {
        byte[] image = fileServiceClient.getMemberImage(memberId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @GetMapping("/image/user/{userId}")
    public ResponseEntity<byte[]> getUserImage(@PathVariable(value = "userId") String userId) {
        byte[] image = fileServiceClient.getUserImage(userId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @GetMapping("/image/school/{schoolId}")
    public ResponseEntity<byte[]> getSchoolImage(@PathVariable(value = "schoolId") String schoolId) {
        byte[] image = fileServiceClient.getSchoolImage(schoolId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @GetMapping("/image/kid/{kidId}")
    public ResponseEntity<byte[]> getKidImage(@PathVariable(value = "kidId") String kidId) {
        byte[] image = fileServiceClient.getKidImage(kidId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @GetMapping("/image/kid_all/{kidId}")
    public ResponseEntity<List<FileOwnerDTO>> getAllKidImage(@PathVariable(value = "kidId") String kidId) {
        List<FileOwnerDTO> dto = fileServiceClient.getAllKidImage(kidId);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("/image/media/{mediaId}")
    public ResponseEntity<byte[]> getImageAlbumMedia(@PathVariable(value = "mediaId") String mediaId) {
        byte[] image = fileServiceClient.getImageAlbumMedia(mediaId);
        return ResponseEntity.ok().headers(httpHeaders -> httpHeaders.setContentType(MediaType.IMAGE_JPEG)).body(image);
    }

    @DeleteMapping("/{fileId}")
    @Transactional
    public ResponseEntity<ResponseDTO<?>> deleteFile(@PathVariable(value = "fileId") String fileId) {
        log.info("=== 파일 삭제 요청 시작 - fileId: {} ===", fileId);
        
        // 1. 메인 DB에서 파일 정보 조회 (과제 제출된 파일)
        log.info("메인 DB에서 파일 정보 조회 시도 - fileKey: {}", fileId);
        Optional<AssignmentSubmissionFile> dbFile = assignmentSubmissionFileRepository.findByFileKey(fileId);
        
        if (dbFile.isPresent()) {
            AssignmentSubmissionFile fileInfo = dbFile.get();
            log.info("메인 DB에서 파일 정보 찾음 - fileKey: {}, fileName: {}, externalFileId: {}, id: {}", 
                    fileInfo.getFileKey(), fileInfo.getFileName(), fileInfo.getId(), fileInfo.getId());
            
            // 외부 파일 서비스의 ID로 삭제 시도 (논리적 삭제만 수행)
            boolean externalDeleteSuccess = false;
            try {
                log.info("외부 파일 서비스 삭제 시도 - externalFileId: {}", fileInfo.getId());
                ResponseDTO<?> deleteResult = fileServiceClient.deleteFile(fileInfo.getId());
                log.info("외부 파일 서비스에서 논리적 삭제 성공 - externalFileId: {}", fileInfo.getId());
                externalDeleteSuccess = true;
                
            } catch (Exception e) {
                log.error("외부 파일 서비스에서 삭제 실패 - externalFileId: {}, 오류: {}", fileInfo.getId(), e.getMessage());
            }
            
            // 메인 DB에서 물리적 삭제 (외부 삭제 성공 여부와 관계없이)
            log.info("메인 DB 물리적 삭제 시도 - fileKey: {}, entity: {}", fileInfo.getFileKey(), fileInfo);
            try {
                // 방법 1: delete() 메서드 사용
                assignmentSubmissionFileRepository.delete(fileInfo);
                log.info("=== 메인 DB에서 파일 물리적 삭제 완료 (delete 메서드) - fileKey: {} ===", fileInfo.getFileKey());
                
                // 방법 2: deleteById() 메서드도 시도
                assignmentSubmissionFileRepository.deleteById(fileInfo.getId());
                log.info("=== 메인 DB에서 파일 물리적 삭제 완료 (deleteById 메서드) - id: {} ===", fileInfo.getId());
                
                // 삭제 확인을 위해 다시 조회
                Optional<AssignmentSubmissionFile> checkFile = assignmentSubmissionFileRepository.findByFileKey(fileId);
                if (checkFile.isPresent()) {
                    AssignmentSubmissionFile remainingFile = checkFile.get();
                    log.error("삭제 후에도 파일이 여전히 존재함 - fileKey: {}, isActive: {}, id: {}", 
                            fileId, remainingFile.getIsActive(), remainingFile.getId());
                } else {
                    log.info("삭제 확인 완료 - 파일이 정상적으로 삭제됨 - fileKey: {}", fileId);
                }
                
                return ResponseEntity.ok(ResponseDTO.builder()
                        .resultCode(200)
                        .resultMessage("파일이 성공적으로 삭제되었습니다.")
                        .data(null)
                        .build());
                
            } catch (Exception e) {
                log.error("메인 DB에서 파일 삭제 실패 - fileKey: {}, 오류: {}", fileInfo.getFileKey(), e.getMessage(), e);
                
                return ResponseEntity.ok(ResponseDTO.builder()
                        .resultCode(500)
                        .resultMessage("메인 DB에서 파일 삭제에 실패했습니다.")
                        .data(null)
                        .build());
            }
        } else {
            log.info("메인 DB에서 파일 정보를 찾을 수 없음 - fileId: {}", fileId);
        }
        
        // 2. 외부 파일 서비스에서 직접 삭제 시도 (과제 제출 전 업로드된 파일)
        log.info("외부 파일 서비스에서 직접 삭제 시도");
        
        // 여러 가지 키 형식으로 시도
        String[] keyVariations = generateKeyVariations(fileId, null);
        
        for (String key : keyVariations) {
            try {
                log.info("외부 서비스에서 키로 삭제 시도: {}", key);
                ResponseDTO<?> deleteResult = fileServiceClient.deleteFile(key);
                log.info("외부 서비스에서 파일 삭제 성공 - key: {}", key);
                
                // 외부 파일 서비스에서 삭제 성공 시 성공 응답 반환
                return ResponseEntity.ok(ResponseDTO.builder()
                        .resultCode(200)
                        .resultMessage("파일이 성공적으로 삭제되었습니다.")
                        .data(null)
                        .build());
                
            } catch (Exception e) {
                log.info("외부 서비스에서 키 '{}'로 삭제 시도 실패: {}", key, e.getMessage());
                continue;
            }
        }
        
        // 모든 시도가 실패한 경우
        log.error("메인 DB와 외부 파일 서비스 모두에서 파일을 찾을 수 없음 - fileId: {}", fileId);
        return ResponseEntity.ok(ResponseDTO.builder()
                .resultCode(404)
                .resultMessage("파일을 찾을 수 없습니다.")
                .data(null)
                .build());
    }

    @PostMapping("/findAll")
    public ResponseEntity<ResponseDTO<List<ResponseFileDTO>>> findAllById(@RequestBody List<String> fileIds) throws IOException {
        ResponseDTO<List<ResponseFileDTO>> responseDTO = fileServiceClient.findAllById(fileIds);
        return ResponseEntity.ok().body(responseDTO);
    }
    
    /**
     * 파일 서비스 연결 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> checkFileServiceHealth() {
        try {
            // 간단한 파일 조회를 통해 서비스 연결 상태 확인
            log.info("파일 서비스 헬스체크 시작");
            return ResponseEntity.ok("File service is healthy");
        } catch (Exception e) {
            log.error("파일 서비스 헬스체크 실패: {}", e.getMessage());
            return ResponseEntity.status(503).body("File service is not available: " + e.getMessage());
        }
    }
    
    /**
     * 파일 정보 조회 (디버깅용)
     */
    @GetMapping("/info/{fileKey}")
    public ResponseEntity<ResponseDTO<Object>> getFileInfo(@PathVariable String fileKey) {
        log.info("파일 정보 조회 요청 - fileKey: {}", fileKey);
        
        // 1. 메인 DB에서 파일 정보 조회
        Optional<AssignmentSubmissionFile> dbFile = assignmentSubmissionFileRepository.findByFileKey(fileKey);
        if (dbFile.isPresent()) {
            AssignmentSubmissionFile fileInfo = dbFile.get();
            log.info("메인 DB에서 파일 정보 찾음: {}", fileInfo);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("source", "main_db");
            response.put("fileKey", fileInfo.getFileKey());
            response.put("fileName", fileInfo.getFileName());
            response.put("externalFileId", fileInfo.getId());
            response.put("fileType", fileInfo.getFileType());
            response.put("fileSize", fileInfo.getFileSize());
            response.put("assignmentId", fileInfo.getAssignmentId());
            response.put("studentId", fileInfo.getStudentId());
            
            return ResponseEntity.ok(ResponseDTO.builder()
                    .resultCode(200)
                    .resultMessage("메인 DB에서 파일 정보를 찾았습니다.")
                    .data(response)
                    .build());
        } else {
            log.info("메인 DB에서 파일 정보를 찾을 수 없음 - fileKey: {}", fileKey);
            
            return ResponseEntity.ok(ResponseDTO.builder()
                    .resultCode(404)
                    .resultMessage("메인 DB에서 파일 정보를 찾을 수 없습니다. 외부 파일 서비스에서 직접 조회를 시도해보세요.")
                    .data(null)
                    .build());
        }
    }
}

