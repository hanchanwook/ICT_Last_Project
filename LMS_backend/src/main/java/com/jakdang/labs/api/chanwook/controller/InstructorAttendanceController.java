package com.jakdang.labs.api.chanwook.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jakdang.labs.api.chanwook.service.AttendanceManagementService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.jakdang.labs.api.auth.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// 강사용 본인 학생 출석 관리 컨트롤러

@RestController
@RequestMapping("/api/instructor/attendance")
@Slf4j
@RequiredArgsConstructor
public class InstructorAttendanceController {

    private final AttendanceManagementService attendanceManagementService;

    // 강사 담당 과정별 출석 현황 조회 (List)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAttendanceStatus(@RequestParam Map<String, String> params) {
        log.info("=== 강사 담당 과정별 출석 현황 조회 요청 === params: {}", params);
        try {
            // 사용자 ID 파라미터 추출 (JWT에서도 확인)
            String userId = params.get("userId");
            
            // 중복 파라미터 처리: userId가 배열로 전달될 경우 첫 번째 값 사용
            if (userId != null && userId.contains(",")) {
                userId = userId.split(",")[0].trim();
                log.info("중복 userId 파라미터 감지, 첫 번째 값 사용: {}", userId);
            }
            
            // 파라미터에 userId가 없으면 JWT에서 추출
            if (userId == null || userId.trim().isEmpty()) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                    userId = userDetails.getUserEntity().getId();
                    log.info("JWT에서 userId 추출: {}", userId);
                } else {
                    log.error("userId가 제공되지 않았고 JWT에서도 추출할 수 없습니다.");
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "userId가 제공되지 않았습니다.");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            // Service 호출하여 실제 DB 데이터 조회
            Map<String, Object> response = attendanceManagementService.getAttendanceStatus(userId, params);
            
            log.info("강사 담당 과정별 출석 현황 조회 성공: {} 건", response.get("totalCount"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("강사 담당 과정별 출석 현황 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "출석 현황 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 강사 - 특정 과정의 출석 디테일 기록 조회 (Detail)
    @GetMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getLectureAttendance(
            @PathVariable String courseId, 
            @RequestParam Map<String, String> params) {
        log.info("=== 특정 강의 출석 기록 조회 요청 === courseId: {}, params: {}", courseId, params);
        try {
            // Service 호출하여 실제 DB 데이터 조회
            Map<String, Object> response = attendanceManagementService.getLectureAttendance(courseId, params);
            
            log.info("특정 강의 출석 기록 조회 성공: {} 건", response.get("totalCount"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("특정 강의 출석 기록 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "출석 기록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 출석 데이터 엑셀 내보내기 (스타일 적용)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAttendanceData(@RequestParam Map<String, String> params) {
        log.info("=== 출석 데이터 엑셀 내보내기 요청 === params: {}", params);
        try {
            // Apache POI를 사용한 Excel 파일 생성
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("출석 데이터");
            
            // 스타일 생성
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            // 헤더 외곽 테두리 (굵게)
            headerStyle.setBorderTop(BorderStyle.MEDIUM);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            // 데이터 셀 테두리 (일반) - 테두리 없음
            dataStyle.setBorderTop(BorderStyle.NONE);
            dataStyle.setBorderBottom(BorderStyle.NONE);
            dataStyle.setBorderLeft(BorderStyle.NONE);
            dataStyle.setBorderRight(BorderStyle.NONE);
            
            // 맨 윗줄 스타일 (첫 번째 행)
            CellStyle topRowStyle = workbook.createCellStyle();
            topRowStyle.setAlignment(HorizontalAlignment.CENTER);
            topRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            topRowStyle.setBorderTop(BorderStyle.THIN);  // 위쪽만 굵게
            topRowStyle.setBorderBottom(BorderStyle.NONE);
            topRowStyle.setBorderLeft(BorderStyle.NONE);
            topRowStyle.setBorderRight(BorderStyle.NONE);
            
            // 맨 왼쪽 줄 스타일 (첫 번째 열)
            CellStyle leftColumnStyle = workbook.createCellStyle();
            leftColumnStyle.setAlignment(HorizontalAlignment.CENTER);
            leftColumnStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            leftColumnStyle.setBorderTop(BorderStyle.NONE);
            leftColumnStyle.setBorderBottom(BorderStyle.NONE);
            leftColumnStyle.setBorderLeft(BorderStyle.THIN);  // 왼쪽만 굵게
            leftColumnStyle.setBorderRight(BorderStyle.NONE);
            
            // 맨 오른쪽 줄 스타일 (마지막 열)
            CellStyle rightColumnStyle = workbook.createCellStyle();
            rightColumnStyle.setAlignment(HorizontalAlignment.CENTER);
            rightColumnStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            rightColumnStyle.setBorderTop(BorderStyle.NONE);
            rightColumnStyle.setBorderBottom(BorderStyle.NONE);
            rightColumnStyle.setBorderLeft(BorderStyle.NONE);
            rightColumnStyle.setBorderRight(BorderStyle.THIN);  // 오른쪽만 굵게
            
            // 맨 아랫줄 스타일 (마지막 행)
            CellStyle bottomRowStyle = workbook.createCellStyle();
            bottomRowStyle.setAlignment(HorizontalAlignment.CENTER);
            bottomRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bottomRowStyle.setBorderTop(BorderStyle.NONE);
            bottomRowStyle.setBorderBottom(BorderStyle.MEDIUM);  // 아래쪽만 굵게
            bottomRowStyle.setBorderLeft(BorderStyle.NONE);
            bottomRowStyle.setBorderRight(BorderStyle.NONE);
            
            // 모서리 셀 스타일 (4개 모서리) 왼쪽 위 모서리
            CellStyle topLeftCornerStyle = workbook.createCellStyle();
            topLeftCornerStyle.setAlignment(HorizontalAlignment.CENTER);
            topLeftCornerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            topLeftCornerStyle.setBorderTop(BorderStyle.THIN);
            topLeftCornerStyle.setBorderBottom(BorderStyle.NONE);
            topLeftCornerStyle.setBorderLeft(BorderStyle.THIN);
            topLeftCornerStyle.setBorderRight(BorderStyle.NONE);
            
            // 모서리 셀 스타일 (4개 모서리) 오른쪽 위 모서리
            CellStyle topRightCornerStyle = workbook.createCellStyle();
            topRightCornerStyle.setAlignment(HorizontalAlignment.CENTER);
            topRightCornerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            topRightCornerStyle.setBorderTop(BorderStyle.THIN);
            topRightCornerStyle.setBorderBottom(BorderStyle.NONE);
            topRightCornerStyle.setBorderLeft(BorderStyle.NONE);
            topRightCornerStyle.setBorderRight(BorderStyle.THIN);
            
            // 모서리 셀 스타일 (4개 모서리) 왼쪽 아래 모서리
            CellStyle bottomLeftCornerStyle = workbook.createCellStyle();
            bottomLeftCornerStyle.setAlignment(HorizontalAlignment.CENTER);
            bottomLeftCornerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bottomLeftCornerStyle.setBorderTop(BorderStyle.NONE);
            bottomLeftCornerStyle.setBorderBottom(BorderStyle.MEDIUM);
            bottomLeftCornerStyle.setBorderLeft(BorderStyle.THIN);
            bottomLeftCornerStyle.setBorderRight(BorderStyle.NONE);
            
            // 모서리 셀 스타일 (4개 모서리) 오른쪽 아래 모서리
            CellStyle bottomRightCornerStyle = workbook.createCellStyle();
            bottomRightCornerStyle.setAlignment(HorizontalAlignment.CENTER);
            bottomRightCornerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bottomRightCornerStyle.setBorderTop(BorderStyle.NONE);
            bottomRightCornerStyle.setBorderBottom(BorderStyle.MEDIUM);
            bottomRightCornerStyle.setBorderLeft(BorderStyle.NONE);
            bottomRightCornerStyle.setBorderRight(BorderStyle.THIN);
            
            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"학생ID", "학생명", "과정명", "출석일", "상태", "입실시간", "퇴실시간"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Service에서 실제 DB 데이터를 가져와서 Excel에 채우기
            List<Map<String, Object>> attendanceData = attendanceManagementService.getAttendanceDataForExport(params);
            
            int rowIndex = 1;
            for (Map<String, Object> record : attendanceData) {
                Row dataRow = sheet.createRow(rowIndex);
                
                String studentId = String.valueOf(record.get("userId"));
                String studentName = String.valueOf(record.get("memberName"));
                String courseName = String.valueOf(record.get("courseName"));
                String date = String.valueOf(record.get("date"));
                String status = String.valueOf(record.get("status"));
                String checkInTime = record.get("checkInTime") != null ? String.valueOf(record.get("checkInTime")) : "";
                String checkOutTime = record.get("checkOutTime") != null ? String.valueOf(record.get("checkOutTime")) : "";
                
                String[] data = {studentId, studentName, courseName, date, status, checkInTime, checkOutTime};
                
                for (int i = 0; i < data.length; i++) {
                    Cell cell = dataRow.createCell(i);
                    cell.setCellValue(data[i]);
                    
                    // 첫 번째 행의 스타일 적용 (맨 윗줄)
                    if (rowIndex == 1) {
                        if (i == 0) {
                            cell.setCellStyle(topLeftCornerStyle);  // 왼쪽 위 모서리
                        } else if (i == data.length - 1) {
                            cell.setCellStyle(topRightCornerStyle);  // 오른쪽 위 모서리
                        } else {
                            cell.setCellStyle(topRowStyle);  // 맨 윗줄 중간 셀들
                        }
                    }
                    // 마지막 행의 스타일 적용 (맨 아랫줄)
                    else if (rowIndex == attendanceData.size()) {
                        if (i == 0) {
                            cell.setCellStyle(bottomLeftCornerStyle);  // 왼쪽 아래 모서리
                        } else if (i == data.length - 1) {
                            cell.setCellStyle(bottomRightCornerStyle);  // 오른쪽 아래 모서리
                        } else {
                            cell.setCellStyle(bottomRowStyle);  // 맨 아랫줄 중간 셀들
                        }
                    }
                    // 중간 행들의 스타일 적용
                    else {
                        if (i == 0) {
                            cell.setCellStyle(leftColumnStyle);  // 맨 왼쪽 열
                        } else if (i == data.length - 1) {
                            cell.setCellStyle(rightColumnStyle);  // 맨 오른쪽 열
                        } else {
                            cell.setCellStyle(dataStyle);  // 중간 열들
                        }
                    }
                }
                rowIndex++;
            }
            
            // 데이터가 없는 경우 헤더만 표시 (더미 데이터 제거)
            if (attendanceData.isEmpty()) {
                log.info("출석 데이터가 없습니다. 헤더만 포함된 엑셀 파일을 생성합니다.");
            }
            
            // 열 너비 설정 (더 크게)
            sheet.setColumnWidth(0, 15 * 256);  // 학생ID
            sheet.setColumnWidth(1, 20 * 256);  // 학생명
            sheet.setColumnWidth(2, 25 * 256);  // 과정명
            sheet.setColumnWidth(3, 15 * 256);  // 출석일
            sheet.setColumnWidth(4, 12 * 256);  // 상태
            sheet.setColumnWidth(5, 15 * 256);  // 입실시간
            sheet.setColumnWidth(6, 15 * 256);  // 퇴실시간
            
            // 파일 생성
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            byte[] excelBytes = outputStream.toByteArray();
            
            log.info("출석 데이터 엑셀 내보내기 성공");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=attendance_data.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("출석 데이터 엑셀 내보내기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 학생 출석 이력 조회
    @GetMapping("/students/{userId}/attendance")
    public ResponseEntity<Map<String, Object>> getStudentAttendance(
            @PathVariable String userId, 
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) String instructorId) {
        log.info("=== 학생 출석 이력 조회 요청 === userId: {}, instructorId: {}, params: {}", userId, instructorId, params);
        try {
            // Service 호출하여 실제 DB 데이터 조회 (강사 권한 확인 포함)
            Map<String, Object> response = attendanceManagementService.getStudentAttendance(userId, instructorId, params);
            
            log.info("학생 출석 이력 조회 성공: {} 건", response.get("totalCount"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("학생 출석 이력 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "학생 출석 이력 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }



    // 출석 기록 검색
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAttendance(@RequestParam Map<String, String> searchParams) {
        log.info("=== 출석 기록 검색 요청 === params: {}", searchParams);
        try {
            // Service 호출하여 실제 DB 데이터 조회
            Map<String, Object> response = attendanceManagementService.searchAttendance(searchParams);
            
            log.info("출석 기록 검색 성공: {} 건", response.get("totalCount"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("출석 기록 검색 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "출석 기록 검색 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 