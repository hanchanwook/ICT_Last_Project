package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.ResponseEntity;
import com.jakdang.labs.api.chanwook.service.ClassroomEquipmentService;
import com.jakdang.labs.api.chanwook.DTO.ClassroomEquipmentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// 직원용 강의실 장비 관련 컨트롤러
@RestController
@RequestMapping("/api/classroom-equipment")
@RequiredArgsConstructor
@Slf4j
public class ClassroomEquipmentController { 
    
    private final ClassroomEquipmentService classroomEquipmentService;
    
   // 직원 - 4 -  강의실 상세 정보 + 장비 목록 동시 로드
    @GetMapping("/classroom/{classId}")
    public ResponseEntity<List<ClassroomEquipmentDTO>> getEquipmentByClassroom(@PathVariable String classId) {
        log.info("=== 강의실별 장비 목록 조회 요청 === classId: {}", classId);
        try {
            List<ClassroomEquipmentDTO> equipments = classroomEquipmentService.getEquipmentsByClassId(classId);
            log.info("조회된 장비 수: {}", equipments.size());
            return ResponseEntity.ok(equipments);
        } catch (Exception e) {
            log.error("강의실별 장비 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 직원 - 7 - 강의실 장비 상태 토글
    @PutMapping("/{equipmentId}/toggle")
    public ResponseEntity<ClassroomEquipmentDTO> toggleEquipmentStatus(
            @PathVariable String equipmentId) {
        log.info("=== 장비 상태 토글 요청 === equipmentId: {}", equipmentId);
        try {
            ClassroomEquipmentDTO equipment = classroomEquipmentService.toggleEquipmentActiveStatus(equipmentId);
            log.info("장비 상태 토글 성공: {} -> equipActive: {}", equipmentId, equipment.getEquipActive());
            return ResponseEntity.ok(equipment);
        } catch (RuntimeException e) {
            log.error("장비 상태 토글 실패 (NotFound): {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("장비 상태 토글 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 직원 - 8 - 강의실 장비 생성
    @PostMapping
    public ResponseEntity<ClassroomEquipmentDTO> createEquipment(@RequestBody ClassroomEquipmentDTO equipmentDTO) {
        log.info("=== 장비 생성 요청 ===");
        log.info("장비 데이터: {}", equipmentDTO);
        
        try {
            ClassroomEquipmentDTO createdEquipment = classroomEquipmentService.createClassroomEquipment(equipmentDTO);
            log.info("장비 생성 성공: {}", createdEquipment.getClassEquipId());
            return ResponseEntity.ok(createdEquipment);
        } catch (Exception e) {
            log.error("장비 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 직원 - 9 - 강의실 장비 수정
    @PutMapping("/{equipmentId}")
    public ResponseEntity<ClassroomEquipmentDTO> updateEquipment(
            @PathVariable String equipmentId,
            @RequestBody ClassroomEquipmentDTO equipmentDTO) {
        log.info("=== 장비 수정 요청 === equipmentId: {}", equipmentId);
        log.info("수정 데이터: {}", equipmentDTO);
        
        try {
            equipmentDTO.setClassEquipId(equipmentId);
            ClassroomEquipmentDTO updatedEquipment = classroomEquipmentService.updateClassroomEquipment(equipmentId, equipmentDTO);
            log.info("장비 수정 성공: {}", updatedEquipment.getClassEquipId());
            return ResponseEntity.ok(updatedEquipment);
        } catch (RuntimeException e) {
            log.error("장비 수정 실패 (NotFound): {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("장비 수정 실패 (BadRequest): {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 직원 - 10 - 강의실 장비 삭제 (실제 DB 삭제)
    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<String> deleteEquipment(@PathVariable String equipmentId) {
        log.info("=== 장비 삭제 요청 === equipmentId: {}", equipmentId);
        try {
            classroomEquipmentService.deleteClassroomEquipmentPermanently(equipmentId);
            log.info("장비 삭제 성공: {}", equipmentId);
            return ResponseEntity.ok("장비가 성공적으로 삭제되었습니다: " + equipmentId);
        } catch (RuntimeException e) {
            log.error("장비 삭제 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    // 직원 - 11  - 장비 엑셀 파일 업로드
    @PostMapping("/excel-upload")
    public ResponseEntity<Map<String, Object>> uploadEquipmentExcel(
            @RequestPart(value = "excelFile", required = false) MultipartFile excelFile,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "classId", required = false) String classId) {
        log.info("=== 장비 엑셀 파일 업로드 요청 ===");
        
        // 상세 디버깅 로그 추가
        log.info("=== 파일 필드 디버깅 ===");
        log.info("excelFile: {}", excelFile != null ? "존재함" : "null");
        log.info("file: {}", file != null ? "존재함" : "null");
        log.info("classId: {}", classId);
        
        if (excelFile != null) {
            log.info("excelFile 상세: 이름={}, 크기={}, 타입={}", 
                    excelFile.getOriginalFilename(), excelFile.getSize(), excelFile.getContentType());
        }
        if (file != null) {
            log.info("file 상세: 이름={}, 크기={}, 타입={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        }
        log.info("=== 디버깅 완료 ===");
        
        // classId 검증
        if (classId == null || classId.trim().isEmpty()) {
            log.error("classId가 제공되지 않았습니다.");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "classId가 제공되지 않았습니다.");
            errorResponse.put("successCount", 0);
            errorResponse.put("errorCount", 1);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // 파일 필드 확인
        MultipartFile uploadFile = excelFile != null ? excelFile : file;
        if (uploadFile == null) {
            log.error("Excel 파일이 제공되지 않았습니다.");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Excel 파일이 제공되지 않았습니다.");
            errorResponse.put("successCount", 0);
            errorResponse.put("errorCount", 1);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        log.info("파일명: {}, 크기: {} bytes, classId: {}", 
                uploadFile.getOriginalFilename(), uploadFile.getSize(), classId);
        
        try {
            log.info("장비 Excel 파일 파싱 시작...");
            
            // Excel 파일 확장자 확인
            String fileName = uploadFile.getOriginalFilename();
            if (fileName == null) {
                log.error("파일명이 null입니다.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "파일명이 null입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String fileExtension = fileName.toLowerCase().substring(fileName.lastIndexOf(".") + 1);
            if (!fileExtension.equals("xlsx") && !fileExtension.equals("xls") && !fileExtension.equals("csv")) {
                log.error("지원하지 않는 파일 형식입니다: {}", fileExtension);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "지원하지 않는 파일 형식입니다. Excel(.xlsx, .xls) 또는 CSV(.csv) 파일을 업로드해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("파일 형식 검증 완료: {} ({})", fileName, fileExtension);
            
            // Excel 파일 처리 및 DB 저장
            log.info("서비스 메서드 호출 시작: processExcelFile, 강의실 ID: {}", classId);
            Map<String, Object> uploadResult = classroomEquipmentService.processExcelFile(uploadFile, classId);
            log.info("서비스 메서드 호출 완료");
            
            log.info("장비 Excel 파일 처리 완료 - 성공: {}건, 실패: {}건", 
                    uploadResult.get("successCount"), uploadResult.get("errorCount"));
            
            if ((Integer) uploadResult.get("successCount") > 0) {
                log.info("DB 저장 성공: {}건의 장비 데이터가 저장되었습니다.", uploadResult.get("successCount"));
            }
            
            if ((Integer) uploadResult.get("errorCount") > 0) {
                log.warn("DB 저장 실패: {}건의 데이터 처리 중 오류가 발생했습니다.", uploadResult.get("errorCount"));
            }
            
            return ResponseEntity.ok(uploadResult);
            
        } catch (Exception e) {
            log.error("장비 Excel 파일 업로드 처리 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "장비 Excel 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("successCount", 0);
            errorResponse.put("errorCount", 0);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // 직원 - 12 - 장비 엑셀 템플릿 다운로드
    @GetMapping("/template/download")
    public ResponseEntity<byte[]> downloadEquipmentTemplate() {
        log.info("=== 장비 엑셀 템플릿 다운로드 요청 ===");
        try {
            byte[] templateData = classroomEquipmentService.downloadEquipmentTemplate();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=classroom_equipment_template.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(templateData);
        } catch (Exception e) {
            log.error("장비 엑셀 템플릿 다운로드 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
