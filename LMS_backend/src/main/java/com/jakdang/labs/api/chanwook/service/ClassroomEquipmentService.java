package com.jakdang.labs.api.chanwook.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Excel 파일 처리를 위한 Apache POI imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.jakdang.labs.api.chanwook.DTO.ClassroomEquipmentDTO;
import com.jakdang.labs.entity.ClassroomEquipmentEntity;
import com.jakdang.labs.entity.ClassroomEntity;
import com.jakdang.labs.api.chanwook.repository.ClassroomEquipmentRepository;
import com.jakdang.labs.api.chanwook.repository.ClassroomRepository;

import java.time.LocalDate;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClassroomEquipmentService {

    private final ClassroomEquipmentRepository classroomEquipmentRepository;
    private final ClassroomRepository classroomRepository;

    // 직원 - 4 - 강의실 상세 정보 + 장비 목록 동시 로드
    public List<ClassroomEquipmentDTO> getEquipmentsByClassId(String classId) {
        log.info("교실별 장비 조회: {}", classId);
        List<ClassroomEquipmentEntity> entities = classroomEquipmentRepository.findByClassId(classId);
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

    // 직원 - 7 - 강의실 장비 상태 토글
    public ClassroomEquipmentDTO toggleEquipmentActiveStatus(String equipmentId) {
        log.info("장비 활성화 상태 토글: {}", equipmentId);
        
        ClassroomEquipmentEntity equipment = classroomEquipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다: " + equipmentId));
        
        // 현재 상태를 반대로 토글
        Integer currentStatus = equipment.getEquipActive();
        Integer newStatus = (currentStatus == 0) ? 1 : 0; // 0 → 1, 1 → 0
        
        equipment.setEquipActive(newStatus);
        ClassroomEquipmentEntity saved = classroomEquipmentRepository.save(equipment);
        
        log.info("장비 상태 토글 완료: {} -> {} (0:활성화, 1:비활성화)", currentStatus, newStatus);
        return toDTO(saved);
    }

    // 직원 - 7 - 강의실 장비 상태 변경 (대여 상태 변경)
    public ClassroomEquipmentDTO updateEquipmentStatus(String equipmentId, Integer equipRent) {
        log.info("장비 대여 상태 변경: {} -> {}", equipmentId, equipRent);
        
        ClassroomEquipmentEntity equipment = classroomEquipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다: " + equipmentId));
        
        equipment.setEquipRent(equipRent);
        ClassroomEquipmentEntity saved = classroomEquipmentRepository.save(equipment);
        return toDTO(saved);
    }

    // 직원 - 8 - 강의실 장비 생성
    public ClassroomEquipmentDTO createClassroomEquipment(ClassroomEquipmentDTO equipmentDTO) {
        log.info("장비 생성: {}", equipmentDTO.getEquipName());
        
        // UUID 생성
        String equipmentId = UUID.randomUUID().toString();
        
        ClassroomEquipmentEntity equipment = ClassroomEquipmentEntity.builder()
                .classEquipId(equipmentId)
                .classId(equipmentDTO.getClassId())
                .equipName(equipmentDTO.getEquipName() != null ? equipmentDTO.getEquipName() : "")
                .equipModel(equipmentDTO.getEquipModel() != null ? equipmentDTO.getEquipModel() : "")
                .equipNumber(equipmentDTO.getEquipNumber() != null ? equipmentDTO.getEquipNumber() : 1)
                .equipDescription(equipmentDTO.getEquipDescription())
                .equipRent(equipmentDTO.getEquipRent() != null ? equipmentDTO.getEquipRent() : 0)
                .equipActive(equipmentDTO.getEquipActive() != null ? equipmentDTO.getEquipActive() : 0)
                .createdAt(LocalDate.now())
                .build();
        
        ClassroomEquipmentEntity saved = classroomEquipmentRepository.save(equipment);
        return toDTO(saved);
    }

    // 직원 - 9 - 강의실 장비 수정
    public ClassroomEquipmentDTO updateClassroomEquipment(String equipmentId, ClassroomEquipmentDTO equipmentDTO) {
            log.info("장비 수정: {}", equipmentId);
            
        ClassroomEquipmentEntity equipment = classroomEquipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다: " + equipmentId));
        
        // DTO의 값으로 Entity 업데이트
        if (equipmentDTO.getClassId() != null) {
            equipment.setClassId(equipmentDTO.getClassId());
        }
        if (equipmentDTO.getEquipName() != null) {
            equipment.setEquipName(equipmentDTO.getEquipName());
        }
        if (equipmentDTO.getEquipModel() != null) {
            equipment.setEquipModel(equipmentDTO.getEquipModel());
        }
        if (equipmentDTO.getEquipNumber() != null) {
            equipment.setEquipNumber(equipmentDTO.getEquipNumber());
        }
        if (equipmentDTO.getEquipDescription() != null) {
            equipment.setEquipDescription(equipmentDTO.getEquipDescription());
        }
        if (equipmentDTO.getEquipRent() != null) {
            equipment.setEquipRent(equipmentDTO.getEquipRent());
        }
        equipment.setEquipActive(equipmentDTO.getEquipActive() != null ? equipmentDTO.getEquipActive() : equipment.getEquipActive());
        
        ClassroomEquipmentEntity saved = classroomEquipmentRepository.save(equipment);
        return toDTO(saved);
    }

    // 직원 - 10 - 강의실 장비 비활성화 처리 (데이터 보존)
    public void deactivateClassroomEquipment(String equipmentId) {
        log.info("장비 비활성화: {}", equipmentId);
        
        ClassroomEquipmentEntity equipment = classroomEquipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다: " + equipmentId));
        
        // 실제 삭제 대신 비활성화 처리 (데이터 보존)
        equipment.setEquipActive(1); // 1: 비활성화, 0: 활성화
        classroomEquipmentRepository.save(equipment);
    }
    
    // 직원 - 10 - 강의실 장비 실제 삭제 (DB에서 완전 삭제)
    public void deleteClassroomEquipmentPermanently(String equipmentId) {
        log.info("장비 실제 삭제: {}", equipmentId);
        
        ClassroomEquipmentEntity equipment = classroomEquipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다: " + equipmentId));
        
        // 실제 DB에서 삭제
        classroomEquipmentRepository.delete(equipment);
    }

    // 직원 - 11 - 장비 Excel 파일 처리 (특정 강의실에 장비 추가)
    public Map<String, Object> processExcelFile(MultipartFile file, String classId) {
        log.info("=== 장비 Excel 파일 처리 시작 ===");
        log.info("파일명: {}, 크기: {} bytes", file.getOriginalFilename(), file.getSize());
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        int maxErrorMessages = 20;
        
        try {
            // Excel 파일 확장자 확인
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                throw new IllegalArgumentException("파일명이 null입니다.");
            }
            
            // Excel 파일 읽기
            Workbook workbook = null;
            try {
                if (fileName.toLowerCase().endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(file.getInputStream());
                    log.info("XLSX 파일 형식으로 읽기 시작");
                } else if (fileName.toLowerCase().endsWith(".xls")) {
                    workbook = new HSSFWorkbook(file.getInputStream());
                    log.info("XLS 파일 형식으로 읽기 시작");
                } else {
                    throw new IllegalArgumentException("지원하지 않는 Excel 파일 형식입니다: " + fileName);
                }
            } catch (Exception e) {
                log.error("Excel 파일 읽기 실패: {}", e.getMessage());
                throw new RuntimeException("Excel 파일을 읽을 수 없습니다: " + e.getMessage());
            }
            
            // 첫 번째 시트 가져오기
            Sheet sheet = workbook.getSheetAt(0);
            log.info("시트명: {}, 총 행 수: {}", sheet.getSheetName(), sheet.getPhysicalNumberOfRows());
            
            // 헤더 행 건너뛰기 (첫 번째 행은 제목)
            int startRow = 1;
            int totalRows = sheet.getPhysicalNumberOfRows();
            
            log.info("데이터 처리 시작 - 시작 행: {}, 총 행 수: {}", startRow, totalRows);
            
            // 각 행 처리
            int consecutiveEmptyRows = 0;
            int maxConsecutiveEmptyRows = 10;
            
            for (int rowIndex = startRow; rowIndex < totalRows; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    consecutiveEmptyRows++;
                    if (consecutiveEmptyRows >= maxConsecutiveEmptyRows) {
                        log.info("연속 빈 행이 {}개 이상이어서 처리 중단: 행 {}", maxConsecutiveEmptyRows, rowIndex);
                        break;
                    }
                    continue;
                }
                
                // 빈 행이 아니면 카운트 리셋
                consecutiveEmptyRows = 0;
                
                try {
                    log.debug("행 {} 처리 시작", rowIndex);
                    
                    // 셀 데이터 추출 (장비 정보) - 강의실 ID는 URL 파라미터로 받음
                    String equipName = getCellValueAsString(row.getCell(0));
                    String equipModel = getCellValueAsString(row.getCell(1));
                    String equipNumberStr = getCellValueAsString(row.getCell(2));
                    String equipDescription = getCellValueAsString(row.getCell(3));
                    String equipRentStr = getCellValueAsString(row.getCell(4));
                    String equipActiveStr = getCellValueAsString(row.getCell(5));
                    
                    // URL 파라미터의 classId 사용 (Excel 파일의 classId는 무시)
                    log.debug("행 {} URL 파라미터 classId 사용: {}", rowIndex, classId);
                    
                    log.debug("행 {} 셀 데이터 추출 완료: 장비명={}, 모델={}", rowIndex, equipName, equipModel);
                    
                    // 필수 필드 검증
                    if (equipName == null || equipName.trim().isEmpty()) {
                        consecutiveEmptyRows++;
                        if (consecutiveEmptyRows >= maxConsecutiveEmptyRows) {
                            log.info("연속 빈 행이 {}개 이상이어서 처리 중단: 행 {}", maxConsecutiveEmptyRows, rowIndex);
                            break;
                        }
                        continue; // 빈 행이면 로그 없이 건너뛰기
                    }
                    
                    // 강의실 존재 여부 확인 (파라미터로 받은 classId 사용)
                    if (!classroomRepository.existsById(classId)) {
                        log.warn("존재하지 않는 강의실 ID: {}", classId);
                        errorCount++;
                        if (errorMessages.size() < maxErrorMessages) {
                            errorMessages.add("존재하지 않는 강의실 ID (" + classId + ")");
                        }
                        continue;
                    }
                    
                    // 중복 검사 (같은 강의실에 같은 이름의 장비가 있는지)
                    if (classroomEquipmentRepository.existsByClassIdAndEquipName(classId, equipName.trim())) {
                        log.warn("행 {} 장비명 중복: {} (강의실: {})", rowIndex, equipName, classId);
                        errorCount++;
                        if (errorMessages.size() < maxErrorMessages) {
                            errorMessages.add("행 " + (rowIndex + 1) + ": 장비명 중복 (" + equipName + ")");
                        }
                        continue;
                    }
                    
                    // ClassroomEquipmentEntity 생성
                    ClassroomEquipmentEntity equipment = createEquipmentFromExcel(
                        classId, equipName, equipModel, equipNumberStr, 
                        equipDescription, equipRentStr, equipActiveStr, rowIndex
                    );
                    
                    if (equipment != null) {
                        // DB 저장
                        classroomEquipmentRepository.save(equipment);
                        successCount++;
                        log.info("행 {} 장비 저장 성공: {}", rowIndex, equipName);
                    } else {
                        errorCount++;
                        if (errorMessages.size() < maxErrorMessages) {
                            errorMessages.add("행 " + (rowIndex + 1) + ": 데이터 변환 실패");
                        }
                        log.warn("행 {} 데이터 변환 실패", rowIndex);
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    if (errorMessages.size() < maxErrorMessages) {
                        String errorMsg = "행 " + (rowIndex + 1) + " 처리 중 오류: " + e.getMessage();
                        errorMessages.add(errorMsg);
                    }
                    log.error("행 {} 처리 실패: {}", rowIndex, e.getMessage());
                }
            }
            
            // 워크북 닫기
            workbook.close();
            
            // uploadId 생성
            String uploadId = "equipment_excel_" + System.currentTimeMillis();
            
            result.put("success", true);
            result.put("message", "장비 Excel 파일 처리가 완료되었습니다.");
            result.put("uploadId", uploadId);
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("errorMessages", errorMessages);
            
            log.info("=== 장비 Excel 파일 처리 완료 ===");
            log.info("성공: {}건, 실패: {}건", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("장비 Excel 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            errorCount = 1;
            errorMessages.add("장비 Excel 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            
            result.put("success", false);
            result.put("message", "장비 Excel 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            result.put("successCount", 0);
            result.put("errorCount", errorCount);
            result.put("errorMessages", errorMessages);
        }
        
        return result;
    }

    // 직원 - 12 - 장비 엑셀 템플릿 다운로드
    public byte[] downloadEquipmentTemplate() {
        log.info("장비 엑셀 템플릿 다운로드");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("장비 등록");
            
            // 헤더 스타일 생성
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 헤더 행 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"장비명*", "장비 모델", "장비 수량", "장비 설명", "대여 여부", "활성화 상태"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256); // 컬럼 너비 설정
            }
            
            // 사용 가능한 강의실 목록 가져오기
            List<ClassroomEntity> classrooms = classroomRepository.findAvailableClassrooms();
            
            // 예시 데이터 행 생성 (실제 데이터가 아닌 형식 예시)
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("프로젝터");
            exampleRow.createCell(1).setCellValue("Epson EB-X41");
            exampleRow.createCell(2).setCellValue(1);
            exampleRow.createCell(3).setCellValue("강의용 프로젝터");
            exampleRow.createCell(4).setCellValue(0);
            exampleRow.createCell(5).setCellValue(0);
            
            // 사용 가능한 강의실 목록을 별도 시트에 추가
            Sheet classroomSheet = workbook.createSheet("사용 가능한 강의실 목록");
            Row classroomHeaderRow = classroomSheet.createRow(0);
            classroomHeaderRow.createCell(0).setCellValue("강의실 ID");
            classroomHeaderRow.createCell(1).setCellValue("강의실 코드");
            classroomHeaderRow.createCell(2).setCellValue("강의실 번호");
            classroomHeaderRow.createCell(3).setCellValue("수용 인원");
            
            for (int i = 0; i < classrooms.size(); i++) {
                Row row = classroomSheet.createRow(i + 1);
                ClassroomEntity classroom = classrooms.get(i);
                row.createCell(0).setCellValue(classroom.getClassId());
                row.createCell(1).setCellValue(classroom.getClassCode());
                row.createCell(2).setCellValue(classroom.getClassNumber());
                row.createCell(3).setCellValue(classroom.getClassCapacity());
            }
            
            // 파일로 저장
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("장비 엑셀 템플릿 생성 실패: {}", e.getMessage());
            return "장비 템플릿 생성 실패".getBytes();
        }
    }

    // Entity를 DTO로 변환
    private ClassroomEquipmentDTO toDTO(ClassroomEquipmentEntity entity) {
        return ClassroomEquipmentDTO.builder()
                .classEquipId(entity.getClassEquipId())
                .classId(entity.getClassId())
                .equipName(entity.getEquipName())
                .equipModel(entity.getEquipModel())
                .equipNumber(entity.getEquipNumber())
                .equipDescription(entity.getEquipDescription())
                .equipRent(entity.getEquipRent())
                .equipActive(entity.getEquipActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    // Excel 셀 값을 String으로 변환하는 헬퍼 메서드
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    // Excel 데이터로부터 ClassroomEquipmentEntity 생성
    private ClassroomEquipmentEntity createEquipmentFromExcel(String classId, String equipName, 
                                                             String equipModel, String equipNumberStr, 
                                                             String equipDescription, String equipRentStr, 
                                                             String equipActiveStr, int rowIndex) {
        try {
            log.debug("Excel 행 {} 장비 데이터 파싱 시작", rowIndex);
            
            // 숫자 필드 파싱
            int equipNumber = parseInteger(equipNumberStr, 1);
            int equipRent = parseInteger(equipRentStr, 0);
            int equipActive = parseInteger(equipActiveStr, 0);
            
            // 필수 필드 검증
            if (equipName == null || equipName.trim().isEmpty()) {
                log.warn("Excel 행 {} 장비명이 비어있음", rowIndex);
                return null;
            }
            
            // UUID 생성
            String equipmentId = UUID.randomUUID().toString();
            
            ClassroomEquipmentEntity equipment = ClassroomEquipmentEntity.builder()
                    .classEquipId(equipmentId)
                    .classId(classId.trim())
                    .equipName(equipName.trim())
                    .equipModel(equipModel != null ? equipModel.trim() : "")
                    .equipNumber(equipNumber)
                    .equipDescription(equipDescription != null ? equipDescription.trim() : "")
                    .equipRent(equipRent)
                    .equipActive(equipActive)
                    .createdAt(LocalDate.now())
                    .build();
            
            log.debug("Excel 행 {} ClassroomEquipmentEntity 생성 완료: {}", rowIndex, equipment.getEquipName());
            return equipment;
            
        } catch (NumberFormatException e) {
            log.error("Excel 행 {} 숫자 필드 파싱 실패: {}", rowIndex, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Excel 행 {} ClassroomEquipmentEntity 생성 실패: {}", rowIndex, e.getMessage());
            return null;
        }
    }
    
    // String을 int로 파싱하는 헬퍼 메서드
    private int parseInteger(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("숫자 파싱 실패: {} -> 기본값 {} 사용", value, defaultValue);
            return defaultValue;
        }
    }
}
