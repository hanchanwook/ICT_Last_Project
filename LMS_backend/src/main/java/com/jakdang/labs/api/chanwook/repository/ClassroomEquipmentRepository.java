package com.jakdang.labs.api.chanwook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.ClassroomEquipmentEntity;

@Repository
public interface ClassroomEquipmentRepository extends JpaRepository<ClassroomEquipmentEntity, String> {
    
    // 강의실별 장비 조회
    List<ClassroomEquipmentEntity> findByClassId(String classId);

    // 강의실별 장비명 중복 검사
    boolean existsByClassIdAndEquipName(String classId, String equipName);
}
