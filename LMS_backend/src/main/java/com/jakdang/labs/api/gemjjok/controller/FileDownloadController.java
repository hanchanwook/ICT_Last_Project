package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.file.dto.RequestFileDTO;
import com.jakdang.labs.api.file.FileServiceClient;
import com.jakdang.labs.api.gemjjok.entity.LectureFile;
import com.jakdang.labs.api.gemjjok.repository.LectureFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Slf4j
@Component
public class FileDownloadController {

    private final LectureFileRepository lectureFileRepository;
    private final FileServiceClient fileServiceClient;

    /**
     * 파일 다운로드 API
     * GET /api/file/download/{fileId}
     */
    // @GetMapping("/download/{fileId:.+}")
    // public ResponseEntity<?> downloadFile(@PathVariable("fileId") String fileId) {
    //     log.info("파일 다운로드 요청 - fileId: {}", fileId);

    //     try {
    //         // 1. fileId 유효성 검증
    //         if (fileId == null || fileId.trim().isEmpty()) {
    //             log.error("파일 다운로드 실패: fileId가 비어있습니다.");
    //             return ResponseEntity.badRequest().body(Map.of(
    //                     "resultCode", 400,
    //                     "resultMessage", "파일 ID가 필요합니다."
    //             ));
    //         }

    //         // 2. 파일 정보 조회 (fileKey로 먼저 시도)
    //         LectureFile fileInfo = lectureFileRepository.findByFileKey(fileId);

    //         // 3. fileKey로 찾지 못한 경우, materialId로 시도 (확장자 제거)
    //         if (fileInfo == null) {
    //             String uuid = fileId;
    //             if (fileId.contains(".")) {
    //                 uuid = fileId.substring(0, fileId.lastIndexOf("."));
    //                 log.info("파일 확장자 제거: {} -> {}", fileId, uuid);
    //             }
                
    //             fileInfo = lectureFileRepository.findByMaterialId(uuid);
    //         }

    //         if (fileInfo == null) {
    //             log.error("파일 다운로드 실패: 파일을 찾을 수 없습니다. fileId: {}", fileId);
                
    //             // 디버깅: 전체 파일 목록 조회
    //             List<LectureFile> allFiles = lectureFileRepository.findAll();
    //             log.info("전체 파일 목록 ({}개):", allFiles.size());
    //             for (LectureFile file : allFiles) {
    //                 log.info("  - id: {}, materialId: {}, fileKey: {}, fileName: {}", 
    //                     file.getId(), file.getMaterialId(), file.getFileKey(), file.getFileName());
    //             }
                
    //             return ResponseEntity.notFound().build();
    //         }

    //                                     // 4. 파일 다운로드 URL 생성 (materialId 사용)
    //                         String downloadUrl = "/api/instructor/file/download/" + fileInfo.getMaterialId();

    //         // 5. 다운로드 URL 반환 (리다이렉트 대신 JSON 응답)
    //         return ResponseEntity.ok().body(Map.of(
    //                 "resultCode", 200,
    //                 "resultMessage", "파일 다운로드 URL 생성 성공",
    //                 "data", Map.of(
    //                         "downloadUrl", downloadUrl,
    //                         "fileName", fileInfo.getFileName(),
    //                         "fileSize", fileInfo.getFileSize(),
    //                         "fileType", fileInfo.getFileType()
    //                 )
    //         ));

    //     } catch (Exception e) {
    //         log.error("파일 다운로드 중 예상치 못한 오류 발생: fileId: {}, error: {}", fileId, e.getMessage(), e);
    //         return ResponseEntity.status(500).body(Map.of(
    //                 "resultCode", 500,
    //                 "resultMessage", "서버 오류가 발생했습니다: " + e.getMessage()
    //         ));
    //     }
    // }

    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestBody RequestFileDTO dto) {
        byte[] file = fileServiceClient.downloadFile(dto);
        return ResponseEntity.ok().body(file);
    }

    /**
     * 파일 다운로드 API (fileKey 사용)
     * GET /api/file/download/key/{fileKey}
     */
    @GetMapping("/download/key/{fileKey}")
    public ResponseEntity<?> downloadFileByKey(@PathVariable("fileKey") String fileKey) {
        log.info("파일 다운로드 요청 - fileKey: {}", fileKey);

        try {
            // 1. fileKey 유효성 검증
            if (fileKey == null || fileKey.trim().isEmpty()) {
                log.error("파일 다운로드 실패: fileKey가 비어있습니다.");
                return ResponseEntity.badRequest().body(Map.of(
                        "resultCode", 400,
                        "resultMessage", "파일 키가 필요합니다."
                ));
            }

            // 2. 파일 정보 조회
            LectureFile fileInfo = lectureFileRepository.findByFileKey(fileKey);

            if (fileInfo == null) {
                log.error("파일 다운로드 실패: 파일을 찾을 수 없습니다. fileKey: {}", fileKey);
                return ResponseEntity.notFound().build();
            }

            // 3. 파일 다운로드 URL 생성
                                        String downloadUrl = "/api/instructor/file/download/" + fileInfo.getMaterialId();

            // 4. 다운로드 URL 반환 (리다이렉트 대신 JSON 응답)
            return ResponseEntity.ok().body(Map.of(
                    "resultCode", 200,
                    "resultMessage", "파일 다운로드 URL 생성 성공",
                    "data", Map.of(
                            "downloadUrl", downloadUrl,
                            "fileName", fileInfo.getFileName(),
                            "fileSize", fileInfo.getFileSize(),
                            "fileType", fileInfo.getFileType()
                    )
            ));

        } catch (Exception e) {
            log.error("파일 다운로드 중 예상치 못한 오류 발생: fileKey: {}, error: {}", fileKey, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "resultCode", 500,
                    "resultMessage", "서버 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
} 