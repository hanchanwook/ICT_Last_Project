package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.gemjjok.DTO.ClassInfoResponse;
import com.jakdang.labs.api.chanwook.service.ClassroomService;
import com.jakdang.labs.entity.ClassroomEntity;
import com.jakdang.labs.api.youngjae.dto.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/instructor/class")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5176"})
public class InstructorClassroomController {

    private static final Logger log = LoggerFactory.getLogger(InstructorClassroomController.class);

    @Autowired
    private ClassroomService classroomService;

    // 강의실 정보 조회 API
    @GetMapping("/{classId}")
    public ResponseEntity<ResponseDTO<?>> getClassInfo(
            @PathVariable(name = "classId") String classId) {
        log.info("=== 강의실 정보 조회 요청 === classId: {}", classId);
        
        try {
            // 1. classId 유효성 검증
            if (classId == null || classId.trim().isEmpty()) {
                log.error("강의실 정보 조회 실패: classId가 비어있습니다.");
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.createErrorResponse(400, "강의실 ID가 필요합니다."));
            }
            
            log.info("classId 유효성 검사 통과: {}", classId);
            
            // 3. 강의실 정보 조회
            ClassroomEntity classRoom = classroomService.findByClassId(classId);
            
            if (classRoom == null) {
                log.error("강의실 정보 조회 실패: 존재하지 않는 강의실입니다. classId: {}", classId);
                return ResponseEntity.status(404)
                    .body(ResponseDTO.createErrorResponse(404, "존재하지 않는 강의실입니다."));
            }
            log.info("강의실 정보 조회 성공: classCode={}, className={}", classRoom.getClassCode(), classRoom.getClassNumber());
            
            // 4. classCode에서 building, floor, roomNumber 추출
            String classCode = classRoom.getClassCode();
            String building = "";
            String floor = "";
            String roomNumber = "";
            
            if (classCode != null && !classCode.trim().isEmpty()) {
                // classCode 파싱 로직 (동적으로 처리)
                if (classCode.length() >= 2) {
                    // 첫 번째 문자를 건물로 처리
                    building = classCode.substring(0, 1);
                    if (classCode.length() >= 3) {
                        // 두 번째 문자를 층으로 처리
                        floor = classCode.substring(1, 2);
                        // 나머지를 호수로 처리
                        roomNumber = classCode.substring(2);
                    } else {
                        // 2자리인 경우
                        floor = classCode.substring(1);
                        roomNumber = "";
                    }
                } else {
                    // 1자리인 경우
                    building = classCode;
                    floor = "";
                    roomNumber = "";
                }
            }
            
            // 5. 응답 데이터 구성
            ClassInfoResponse classInfo = new ClassInfoResponse(
                classRoom.getClassId(),
                classRoom.getClassCode(),      // 원본 classCode
                String.valueOf(classRoom.getClassNumber()), // 원본 classNumber
                Integer.parseInt(classRoom.getClassCapacity()), // 수용 인원
                building,                      // 파싱된 건물
                floor,                         // 파싱된 층
                roomNumber                     // 파싱된 호수
            );
            
            log.info("강의실 정보 응답 구성 완료: classCode={}, building={}, floor={}, roomNumber={}", 
                classInfo.getClassCode(), classInfo.getBuilding(), classInfo.getFloor(), classInfo.getRoomNumber());
            
            // ResponseDTO로 감싸서 반환
            return ResponseEntity.ok()
                .body(ResponseDTO.createSuccessResponse("강의실 정보 조회 성공", classInfo));
                
        } catch (NumberFormatException e) {
            log.error("강의실 정보 조회 실패: 수용 인원 파싱 오류 - classId: {}, error: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.createErrorResponse(400, "강의실 정보 조회에 실패했습니다: 수용 인원 형식 오류"));
        } catch (IllegalArgumentException e) {
            log.error("강의실 정보 조회 실패: 잘못된 인자 - classId: {}, error: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.createErrorResponse(400, "강의실 정보 조회에 실패했습니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("강의실 정보 조회 중 예상치 못한 오류 발생: classId: {}, error: {}", classId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ResponseDTO.createErrorResponse(500, "강의실 정보 조회에 실패했습니다: " + e.getMessage()));
        }
    }
} 