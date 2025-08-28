package com.jakdang.labs.api.gemjjok.util;

import com.jakdang.labs.entity.AssignmentEntity;
import com.jakdang.labs.api.gemjjok.repository.AssignmentRepository;

import java.util.List;

public class AssignmentUuidMapper {
    
    /**
     * UUID를 실제 데이터베이스의 과제 ID로 매핑
     * @param uuid 프론트엔드에서 전달받은 UUID
     * @param assignmentRepository 과제 리포지토리
     * @return 실제 데이터베이스의 과제 ID
     */
    public static String mapUuidToActualId(String uuid, AssignmentRepository assignmentRepository) {
        // 데이터베이스의 모든 과제를 조회하여 UUID와 매핑
        List<AssignmentEntity> allAssignments = assignmentRepository.findAll();
        
        // UUID 패턴이 아닌 ID들을 찾아서 매핑
        for (AssignmentEntity assignment : allAssignments) {
            String id = assignment.getAssignmentId();
            // UUID가 아닌 간단한 ID인 경우 (숫자나 짧은 문자열)
            if (!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                // 첫 번째 간단한 ID를 반환 (임시 매핑)
                // TODO: 더 정확한 매핑 로직 구현 (예: 과제 제목, 생성일 등으로 매핑)
                // System.out.println("UUID 매핑: " + uuid + " -> " + id);
                return id;
            }
        }
        
        // 매핑할 수 없는 경우 원본 UUID 반환
        // System.out.println("UUID 매핑 실패: " + uuid + " (매핑할 과제가 없음)");
        return uuid;
    }
} 