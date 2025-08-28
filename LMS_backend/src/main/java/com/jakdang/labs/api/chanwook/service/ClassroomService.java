package com.jakdang.labs.api.chanwook.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Excel 파일 처리를 위한 Apache POI imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;


import com.jakdang.labs.api.chanwook.DTO.ClassroomDTO;
import com.jakdang.labs.entity.ClassroomEntity;
import com.jakdang.labs.entity.EducationEntity;
import com.jakdang.labs.api.chanwook.repository.ClassroomRepository;
import com.jakdang.labs.api.chanwook.repository.QrCodeRepository;
import com.jakdang.labs.api.jaegyeom.registration.repository.EduRegisterRepository;
// import com.jakdang.labs.api.jaegyeom.registration.repository.EduRegisterRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final QrCodeRepository qrCodeRepository;
    private final EduRegisterRepository eduRegisterRepository;

    // 강의실 ID로 조회
    public ClassroomEntity findByClassId(String classId) {
        log.info("강의실 정보 조회: classId = {}", classId);
        List<ClassroomEntity> classrooms = classroomRepository.findByClassIdAndClassActive(classId, 0);
        return classrooms.isEmpty() ? null : classrooms.get(0);
    }

    // 모든 교실 조회 (활성화/비활성화 상관없이)
    public List<ClassroomDTO> getAllClassrooms(Map<String, String> params) {
        log.info("모든 교실 조회: params={}", params);
        
        // educationId 파라미터 추출
        String educationId = params.get("educationId");
        
        List<ClassroomEntity> entities;
        
        if (educationId != null && !educationId.isEmpty()) {
            // educationId가 있으면 해당 교육기관의 모든 교실 조회 (활성화/비활성화 상관없이)
            log.info("educationId로 모든 교실 조회: {}", educationId);
            entities = classroomRepository.findByEducationId(educationId);
        } else {
            // educationId가 없으면 전체 교실 조회 (활성화/비활성화 상관없이)
            log.info("전체 교실 조회 (educationId 없음)");
            entities = classroomRepository.findAllClassrooms();
        }
        
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

    // 교실 생성
    @Transactional
    public ClassroomDTO createClassroom(ClassroomDTO classroomDTO) {
        log.info("교실 생성: {}", classroomDTO.getClassCode());
        
        // 1인당 면적 자동 계산
        String calculatedPersonArea = calculatePersonArea(classroomDTO.getClassArea(), classroomDTO.getClassCapacity());
        
        ClassroomEntity classroom = ClassroomEntity.builder()
                .classCode(classroomDTO.getClassCode())
                .classNumber(classroomDTO.getClassNumber())
                .classCapacity(classroomDTO.getClassCapacity())
                .classActive(classroomDTO.getClassActive())
                .classRent(classroomDTO.getClassRent())
                .classArea(classroomDTO.getClassArea())
                .classPersonArea(calculatedPersonArea)
                .classMemo(classroomDTO.getClassMemo())
                .educationId(classroomDTO.getEducationId())
                .educationName(classroomDTO.getEducationName())
                .build();
        
        if (calculatedPersonArea != null) {
            log.info("1인당 면적 자동 계산: {}㎡ / {}명 = {}㎡/명", 
                    classroomDTO.getClassArea(), classroomDTO.getClassCapacity(), calculatedPersonArea);
        }
        
        ClassroomEntity saved = classroomRepository.save(classroom);
        return toDTO(saved);
    }
    
    // 1인당 면적 계산 메서드 (String으로 처리)
    private String calculatePersonArea(String classArea, String classCapacity) {
        // 면적이 null이거나 비어있는 경우
        if (classArea == null || classArea.trim().isEmpty()) {
            log.info("면적이 입력되지 않아 1인당 면적 계산 생략");
            return null;
        }
        
        // 수용인원이 null이거나 비어있는 경우
        if (classCapacity == null || classCapacity.trim().isEmpty()) {
            log.info("수용인원이 입력되지 않아 1인당 면적 계산 생략");
            return null;
        }
        
        try {
            // 면적을 실수로 변환
            double area = Double.parseDouble(classArea.trim());
            
            // 수용인원을 정수로 변환
            int capacity = Integer.parseInt(classCapacity.trim());
            
            if (capacity <= 0) {
                log.warn("수용인원이 0 이하이어서 1인당 면적 계산 불가: {}", capacity);
                return null;
            }
            
            // 1인당 면적 계산 (면적 / 수용인원)
            double personArea = area / capacity;
            
            log.debug("1인당 면적 계산: {}㎡ / {}명 = {}㎡/명", area, capacity, personArea);
            return String.valueOf(personArea);
            
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: 면적={}, 수용인원={} (1인당 면적 계산 생략)", classArea, classCapacity);
            return null;
        }
    }

    // 교실 수정
    @Transactional
    public ClassroomDTO updateClassroom(String classId, ClassroomDTO classroomDTO) {
        log.info("교실 수정: {}", classId);
        
        ClassroomEntity classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("교실을 찾을 수 없습니다: " + classId));
        
        // DTO의 값으로 Entity 업데이트
        if (classroomDTO.getClassCode() != null) {
            classroom.setClassCode(classroomDTO.getClassCode());
        }
        if (classroomDTO.getClassNumber() > 0) {
            classroom.setClassNumber(classroomDTO.getClassNumber());
        }
        if (classroomDTO.getClassCapacity() != null) {
            classroom.setClassCapacity(classroomDTO.getClassCapacity());
        }
        if (classroomDTO.getClassRent() > 0) {
            classroom.setClassRent(classroomDTO.getClassRent());
        }
        classroom.setClassActive(classroomDTO.getClassActive());
        classroom.setClassArea(classroomDTO.getClassArea());
        classroom.setClassMemo(classroomDTO.getClassMemo());
        if (classroomDTO.getEducationId() != null) {
            classroom.setEducationId(classroomDTO.getEducationId());
        }
        if (classroomDTO.getEducationName() != null) {
            classroom.setEducationName(classroomDTO.getEducationName());
        }
        classroom.setUpdatedAt(LocalDate.now());
        
        // 1인당 면적 자동 계산
        String calculatedPersonArea = calculatePersonArea(classroomDTO.getClassArea(), classroomDTO.getClassCapacity());
        classroom.setClassPersonArea(calculatedPersonArea);
        
        log.info("1인당 면적 자동 계산: {}㎡ / {}명 = {}㎡/명", 
                classroomDTO.getClassArea(), classroomDTO.getClassCapacity(), calculatedPersonArea);
        
        ClassroomEntity saved = classroomRepository.save(classroom);
        return toDTO(saved);
    }

    // 교실 삭제
    @Transactional
    public void deleteClassroom(String classId) {
        log.info("교실 삭제: {}", classId);
        
        // 연관된 QR 코드 먼저 삭제
        qrCodeRepository.deleteByClassId(classId);
        log.info("연관된 QR 코드 삭제 완료: {}", classId);

        ClassroomEntity classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("교실을 찾을 수 없습니다: " + classId));
        
        // 실제 삭제 처리
        classroomRepository.delete(classroom);
        log.info("교실 삭제 완료: {}", classId);
    }

    // 강의실 상세 정보 조회
    public ClassroomDTO getClassroomById(String classId) {
        log.info("강의실 상세 정보 조회: {}", classId);
        
        ClassroomEntity classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("교실을 찾을 수 없습니다: " + classId));
        
        return toDTO(classroom);
    }

    // 강의실 통계 대시보드 데이터 조회
    public Map<String, Object> getClassroomStats() {
        log.info("강의실 통계 데이터 조회");
        
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 강의실 수
        long totalClassrooms = classroomRepository.count();
        stats.put("totalClassrooms", totalClassrooms);
        
        return stats;
    }

    // 강의실 통계와 목록을 한 번에 조회 (최적화)
    public Map<String, Object> getClassroomDashboardData() {
        log.info("강의실 대시보드 데이터 조회 (통계 + 목록)");
        
        Map<String, Object> dashboardData = new HashMap<>();
        
        // 통계 데이터 조회
        Map<String, Object> stats = getClassroomStats();
        dashboardData.put("stats", stats);
        
        // 강의실 목록 조회
        List<ClassroomDTO> classrooms = getAllClassrooms(null);
        dashboardData.put("classrooms", classrooms);
        
        log.info("대시보드 데이터 조회 완료 - 통계: {}, 강의실: {}개", stats.size(), classrooms.size());
        return dashboardData;
    }

    // CSV/Excel 파일 처리 및 DB 저장
    public Map<String, Object> processClassroomFile(MultipartFile file) {
        log.info("=== CSV/Excel 파일 처리 시작 ===");
        log.info("파일명: {}, 크기: {} bytes", file.getOriginalFilename(), file.getSize());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 파일 확장자에 따른 처리
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                log.info("CSV 파일 처리 시작");
                result = processCSVFile(file);
            } else if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
                log.info("Excel 파일 처리 시작");
                result = processExcelFile(file);
            } else {
                log.error("지원하지 않는 파일 형식: {}", fileName);
                result.put("success", false);
                result.put("message", "지원하지 않는 파일 형식입니다.");
                result.put("successCount", 0);
                result.put("errorCount", 0);
                return result;
            }
            
            log.info("파일 처리 완료 - 성공: {}건, 실패: {}건", 
                    result.get("successCount"), result.get("errorCount"));
            
        } catch (Exception e) {
            log.error("파일 처리 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            result.put("successCount", 0);
            result.put("errorCount", 1);
            result.put("errorMessages", List.of(e.getMessage()));
        }
        
        return result;
    }
    
    // CSV 파일 처리
    @Transactional
    public Map<String, Object> processCSVFile(MultipartFile file) throws IOException {
        return processCSVFile(file, null);
    }
    
    // CSV 파일 처리 (educationId 포함)
    @Transactional
    public Map<String, Object> processCSVFile(MultipartFile file, String educationId) throws IOException {
        log.info("CSV 파일 파싱 시작 - educationId: {}", educationId);
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                log.debug("CSV 라인 {} 처리: {}", lineNumber, line);
                
                try {
                    // 헤더 라인 스킵
                    if (lineNumber == 1) {
                        log.info("CSV 헤더 라인 스킵: {}", line);
                        continue;
                    }
                    
                    // CSV 라인 파싱
                    String[] fields = line.split(",");
                    if (fields.length < 3) {
                        log.warn("CSV 라인 {} 필드 수 부족: {}", lineNumber, fields.length);
                        errorCount++;
                        errorMessages.add("라인 " + lineNumber + ": 필드 수가 부족합니다.");
                        continue;
                    }
                    
                    // ClassroomEntity 생성 및 저장 (educationId 전달)
                    ClassroomEntity classroom = createClassroomFromCSV(fields, lineNumber, educationId);
                    if (classroom != null) {
                        classroomRepository.save(classroom);
                        successCount++;
                        log.info("CSV 라인 {} 강의실 저장 성공: {}", lineNumber, classroom.getClassCode());
                    }
                    
                } catch (Exception e) {
                    log.error("CSV 라인 {} 처리 실패: {}", lineNumber, e.getMessage());
                    errorCount++;
                    errorMessages.add("라인 " + lineNumber + ": " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            log.error("CSV 파일 읽기 실패: {}", e.getMessage());
            throw e;
        }
        
        // uploadId 생성 (현재 시간 기반)
        String uploadId = "csv_" + System.currentTimeMillis();
        
        result.put("success", successCount > 0);
        result.put("uploadId", uploadId);
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errorMessages", errorMessages);
        result.put("message", String.format("처리 완료: 성공 %d건, 실패 %d건", successCount, errorCount));
        
        log.info("CSV 파일 처리 완료 - 성공: {}건, 실패: {}건", successCount, errorCount);
        return result;
    }
    
    // Excel 파일 처리 (Apache POI 사용)
    public Map<String, Object> processExcelFile(MultipartFile file) {
        return processExcelFile(file, null);
    }
    
    // Excel 파일 처리 (educationId 포함)
    public Map<String, Object> processExcelFile(MultipartFile file, String educationId) {
        log.info("=== Excel 파일 처리 시작 ===");
        log.info("파일명: {}, 크기: {} bytes, educationId: {}", 
                file.getOriginalFilename(), file.getSize(), educationId);
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        int maxErrorMessages = 20; // 최대 에러 메시지 수 제한
        
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
            int consecutiveEmptyRows = 0; // 연속 빈 행 카운트
            int maxConsecutiveEmptyRows = 5; // 최대 연속 빈 행 허용 수
            
            for (int rowIndex = startRow; rowIndex < totalRows; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                
                // Row가 null이거나 모든 셀이 비어있는 경우 빈 행으로 처리
                boolean isEmptyRow = false;
                String classCode = null;
                
                if (row == null) {
                    isEmptyRow = true;
                } else {
                    // 첫 번째 셀(classCode) 확인
                    classCode = getCellValueAsString(row.getCell(0));
                    isEmptyRow = (classCode == null || classCode.trim().isEmpty());
                }
                
                if (isEmptyRow) {
                    consecutiveEmptyRows++;
                    if (consecutiveEmptyRows <= 2) {
                        log.debug("빈 행 건너뛰기: 행 {}", rowIndex);
                    } else if (consecutiveEmptyRows == 3) {
                        log.info("연속 빈 행이 많아 로그를 줄입니다...");
                    }
                    
                    // 연속 빈 행이 최대 허용 수를 초과하면 처리 중단
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
                    
                    // 셀 데이터 추출
                    String classNumberStr = getCellValueAsString(row.getCell(1));
                    String classCapacity = getCellValueAsString(row.getCell(2));
                    String classActiveStr = getCellValueAsString(row.getCell(3));
                    String classRentStr = getCellValueAsString(row.getCell(4));
                    String classAreaStr = getCellValueAsString(row.getCell(5));
                    String classPersonAreaStr = getCellValueAsString(row.getCell(6));
                    String classMemo = getCellValueAsString(row.getCell(7));
                    String educationIdStr = getCellValueAsString(row.getCell(8));
                    
                    log.debug("행 {} 셀 데이터 추출 완료: 코드={}, 수용인원={}", rowIndex, classCode, classCapacity);
                    
                    // 중복 검사
                    if (classroomRepository.existsByClassCode(classCode.trim())) {
                        log.warn("행 {} 강의실 코드 중복: {}", rowIndex, classCode);
                        errorCount++;
                        if (errorMessages.size() < maxErrorMessages) {
                            errorMessages.add("행 " + (rowIndex + 1) + ": 강의실 코드 중복 (" + classCode + ")");
                        }
                        continue;
                    }
                    
                    // ClassroomEntity 생성 (educationId 전달)
                    ClassroomEntity classroom = createClassroomFromExcel(
                        classCode.trim(),
                        classNumberStr,
                        classCapacity,
                        classActiveStr,
                        classRentStr,
                        classAreaStr,
                        classPersonAreaStr,
                        classMemo,
                        educationIdStr,
                        rowIndex,
                        educationId
                    );
                    
                    if (classroom != null) {
                        // 직접 DB 저장 (트랜잭션 없음)
                        classroomRepository.save(classroom);
                        successCount++;
                        log.info("행 {} 강의실 저장 성공: {}", rowIndex, classCode);
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
            
            // uploadId 생성 (현재 시간 기반)
            String uploadId = "excel_" + System.currentTimeMillis();
            
            result.put("success", true);
            result.put("message", "Excel 파일 처리가 완료되었습니다.");
            result.put("uploadId", uploadId);
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("errorMessages", errorMessages);
            
            // 에러 메시지가 제한되었음을 안내
            if (errorCount > maxErrorMessages) {
                result.put("message", "Excel 파일 처리가 완료되었습니다. (에러 메시지는 최대 " + maxErrorMessages + "개만 표시됩니다.)");
            }
            
            log.info("=== Excel 파일 처리 완료 ===");
            log.info("성공: {}건, 실패: {}건", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Excel 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            errorCount = 1;
            errorMessages.add("Excel 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            
            result.put("success", false);
            result.put("message", "Excel 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            result.put("successCount", 0);
            result.put("errorCount", errorCount);
            result.put("errorMessages", errorMessages);
        }
        
        return result;
    }
    

    
    // 업로드 결과 검증
    public Map<String, Object> validateUploadResult(String uploadId) {
        log.info("=== 업로드 결과 검증 시작 === uploadId: {}", uploadId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // uploadId가 "undefined"이거나 null인 경우 처리
            if (uploadId == null || "undefined".equals(uploadId) || uploadId.isEmpty()) {
                log.warn("uploadId가 유효하지 않음: {}", uploadId);
                result.put("uploadId", uploadId);
                result.put("status", "warning");
                result.put("validRecords", 0);
                result.put("invalidRecords", 0);
                result.put("message", "업로드 ID가 유효하지 않습니다. 최근 업로드된 데이터를 확인합니다.");
                result.put("validRecords", 0);
                result.put("invalidRecords", 0);
                result.put("message", "업로드 ID가 유효하지 않습니다.");
                
            } else {
                // 정상적인 uploadId 처리 (향후 구현)
                log.info("정상적인 uploadId 처리: {}", uploadId);
                result.put("uploadId", uploadId);
                result.put("status", "validated");
                result.put("validRecords", 0);
                result.put("invalidRecords", 0);
                result.put("message", "업로드 ID 기반 검증 (구현 예정)");
            }
            
            log.info("업로드 결과 검증 완료: {} - 유효: {}건, 무효: {}건", uploadId, 
                result.get("validRecords"), result.get("invalidRecords"));
            
        } catch (Exception e) {
            log.error("업로드 결과 검증 실패: {}", e.getMessage(), e);
            result.put("uploadId", uploadId);
            result.put("status", "error");
            result.put("validRecords", 0);
            result.put("invalidRecords", 0);
            result.put("message", "업로드 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    // Excel 셀 값을 String으로 변환하는 헬퍼 메서드
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 숫자를 정수로 변환하여 반환
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    // Excel 데이터로부터 ClassroomEntity 생성 (educationId 포함)
    private ClassroomEntity createClassroomFromExcel(String classCode, String classNumberStr, 
                                                   String classCapacity, String classActiveStr, 
                                                   String classRentStr, String classAreaStr, 
                                                   String classPersonAreaStr, String classMemo, 
                                                   String educationIdStr, int rowIndex, String providedEducationId) {
        try {
            log.debug("Excel 행 {} 데이터 파싱 시작", rowIndex);
            
            // 숫자 필드 파싱
            int classNumber = parseInteger(classNumberStr, 0);
            int classActive = parseInteger(classActiveStr, 1);
            int classRent = parseInteger(classRentStr, 0);
            String classArea = classAreaStr != null ? classAreaStr.trim() : null;
            String classPersonArea = classPersonAreaStr != null ? classPersonAreaStr.trim() : null;
            
            // educationId 처리 - 기본값 "1" 사용 (education 테이블에 존재하는 값)
            String educationId = educationIdStr != null ? educationIdStr : "1";
            
            // 제공된 educationId가 있으면 우선 사용
            if (providedEducationId != null && !providedEducationId.trim().isEmpty()) {
                educationId = providedEducationId.trim();
                log.info("Excel 행 {} - 제공된 educationId 사용: {}", rowIndex, educationId);
            }
            
            // educationId 검증 (빈 값인 경우에만 기본값 설정)
            if (educationId == null || educationId.trim().isEmpty()) {
                log.warn("Excel 행 {} educationId가 비어있음 -> 기본값 1 사용", rowIndex);
                educationId = "1";
            }
            
            // 필수 필드 검증
            if (classCode == null || classCode.trim().isEmpty()) {
                log.warn("Excel 행 {} 강의실 코드가 비어있음", rowIndex);
                return null;
            }
            
            // 중복 검사
            if (classroomRepository.existsByClassCode(classCode.trim())) {
                log.warn("Excel 행 {} 강의실 코드 중복: {}", rowIndex, classCode);
                return null;
            }
            
            // educationName 조회
            String educationName = null;
            if (educationId != null && !educationId.trim().isEmpty()) {
                try {
                    EducationEntity education = eduRegisterRepository.findByEducationId(educationId);
                    if (education != null) {
                        educationName = education.getEducationName();
                        log.debug("Excel 행 {} educationName 조회 성공: educationId={}, educationName={}", rowIndex, educationId, educationName);
                    } else {
                        log.warn("Excel 행 {} education을 찾을 수 없음: educationId={}", rowIndex, educationId);
                    }
                } catch (Exception e) {
                    log.warn("Excel 행 {} educationName 조회 실패: educationId={}, error={}", rowIndex, educationId, e.getMessage());
                }
            }
            
            ClassroomEntity classroom = ClassroomEntity.builder()
                    .classCode(classCode.trim())
                    .classNumber(classNumber)
                    .classCapacity(classCapacity != null ? classCapacity.trim() : "")
                    .classActive(classActive)
                    .classRent(classRent)
                    .classArea(classArea)
                    .classPersonArea(classPersonArea)
                    .classMemo(classMemo != null ? classMemo.trim() : "")
                    .educationId(educationId)
                    .educationName(educationName)
                    .build();
            
            log.debug("Excel 행 {} ClassroomEntity 생성 완료: {}", rowIndex, classroom.getClassCode());
            return classroom;
            
        } catch (NumberFormatException e) {
            log.error("Excel 행 {} 숫자 필드 파싱 실패: {}", rowIndex, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Excel 행 {} ClassroomEntity 생성 실패: {}", rowIndex, e.getMessage());
            return null;
        }
    }
    
    // String을 int로 파싱하는 헬퍼 메서드 (실수 처리 포함)
    private int parseInteger(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            String trimmedValue = value.trim();
            // 실수가 포함된 경우 소수점 이하를 제거하고 정수로 변환
            if (trimmedValue.contains(".")) {
                double doubleValue = Double.parseDouble(trimmedValue);
                return (int) Math.round(doubleValue);
            }
            return Integer.parseInt(trimmedValue);
        } catch (NumberFormatException e) {
            log.warn("정수 파싱 실패: {} -> 기본값 {} 사용", value, defaultValue);
            return defaultValue;
        }
    }
    
    // String을 Integer로 파싱하는 헬퍼 메서드 (null 허용, 실수 처리 포함)
    private Integer parseIntegerOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmedValue = value.trim();
            // 실수가 포함된 경우 소수점 이하를 제거하고 정수로 변환
            if (trimmedValue.contains(".")) {
                double doubleValue = Double.parseDouble(trimmedValue);
                return (int) Math.round(doubleValue);
            }
            return Integer.parseInt(trimmedValue);
        } catch (NumberFormatException e) {
            log.warn("정수 파싱 실패: {} -> null 반환", value);
            return null;
        }
    }
    
    // String을 Double로 파싱하는 헬퍼 메서드 (null 허용)
    private Double parseDoubleOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("실수 파싱 실패: {} -> null 반환", value);
            return null;
        }
    }
    
    // CSV 필드로부터 ClassroomEntity 생성
    private ClassroomEntity createClassroomFromCSV(String[] fields, int lineNumber, String providedEducationId) {
        try {
            log.debug("CSV 필드 파싱 - 라인 {}: {}", lineNumber, String.join(",", fields));
            
            // 필드 파싱 (예시: 강의실코드,강의실번호,수용인원,활성여부,대여료,면적,인당면적,메모,교육기관ID)
            String classCode = fields.length > 0 ? fields[0].trim() : "";
            int classNumber = fields.length > 1 ? parseInteger(fields[1], 0) : 0;
            String classCapacity = fields.length > 2 ? fields[2].trim() : "";
            int classActive = fields.length > 3 ? parseInteger(fields[3], 1) : 1;
            int classRent = fields.length > 4 ? parseInteger(fields[4], 0) : 0;
            String classArea = fields.length > 5 ? fields[5].trim() : null;
            String classPersonArea = fields.length > 6 ? fields[6].trim() : null;
            String classMemo = fields.length > 7 ? fields[7].trim() : "";
            String educationId = fields.length > 8 ? fields[8].trim() : "1";
            
            // 제공된 educationId가 있으면 우선 사용
            if (providedEducationId != null && !providedEducationId.trim().isEmpty()) {
                educationId = providedEducationId.trim();
                log.info("CSV 라인 {} - 제공된 educationId 사용: {}", lineNumber, educationId);
            }
            
            // educationId 검증 (빈 값인 경우에만 기본값 설정)
            if (educationId == null || educationId.trim().isEmpty()) {
                log.warn("CSV 라인 {} educationId가 비어있음 -> 기본값 1 사용", lineNumber);
                educationId = "1";
            }
            
            // 필수 필드 검증
            if (classCode.isEmpty()) {
                log.warn("CSV 라인 {} 강의실 코드가 비어있음", lineNumber);
                return null;
            }
            
            // 중복 검사
            if (classroomRepository.existsByClassCode(classCode)) {
                log.warn("CSV 라인 {} 강의실 코드 중복: {}", lineNumber, classCode);
                return null;
            }
            
            // educationName 조회
            String educationName = null;
            if (educationId != null && !educationId.trim().isEmpty()) {
                try {
                    EducationEntity education = eduRegisterRepository.findByEducationId(educationId);
                    if (education != null) {
                        educationName = education.getEducationName();
                        log.debug("CSV 라인 {} educationName 조회 성공: educationId={}, educationName={}", lineNumber, educationId, educationName);
                    } else {
                        log.warn("CSV 라인 {} education을 찾을 수 없음: educationId={}", lineNumber, educationId);
                    }
                } catch (Exception e) {
                    log.warn("CSV 라인 {} educationName 조회 실패: educationId={}, error={}", lineNumber, educationId, e.getMessage());
                }
            }
            
            ClassroomEntity classroom = ClassroomEntity.builder()
                    .classCode(classCode)
                    .classNumber(classNumber)
                    .classCapacity(classCapacity)
                    .classActive(classActive)
                    .classRent(classRent)
                    .classArea(classArea)
                    .classPersonArea(classPersonArea)
                    .classMemo(classMemo)
                    .educationId(educationId)
                    .educationName(educationName)
                    .build();
            
            log.debug("CSV 라인 {} ClassroomEntity 생성 완료: {}", lineNumber, classroom.getClassCode());
            return classroom;
            
        } catch (NumberFormatException e) {
            log.error("CSV 라인 {} 숫자 필드 파싱 실패: {}", lineNumber, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("CSV 라인 {} ClassroomEntity 생성 실패: {}", lineNumber, e.getMessage());
            return null;
        }
    }

    // Entity를 DTO로 변환
    private ClassroomDTO toDTO(ClassroomEntity entity) {
        // educationName 처리 - 저장된 값 우선 사용, 없으면 조회
        String educationName = entity.getEducationName();
        if (educationName == null && entity.getEducationId() != null && !entity.getEducationId().isEmpty()) {
            try {
                EducationEntity education = eduRegisterRepository.findByEducationId(entity.getEducationId());
                if (education != null) {
                    educationName = education.getEducationName();
                }
            } catch (Exception e) {
                log.warn("EducationName 조회 실패: educationId={}, error={}", entity.getEducationId(), e.getMessage());
            }
        }
        
        return ClassroomDTO.builder()
                .classId(entity.getClassId())
                .classCode(entity.getClassCode())
                .classNumber(entity.getClassNumber())
                .classCapacity(entity.getClassCapacity())
                .classActive(entity.getClassActive())
                .classRent(entity.getClassRent())
                .classArea(entity.getClassArea())
                .classPersonArea(entity.getClassPersonArea())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .classMemo(entity.getClassMemo())
                .educationId(entity.getEducationId())
                .educationName(educationName)
                .build();
    }
}