package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.entity.SubjectDetailEntity;

import org.springframework.stereotype.Repository;

@Repository
public interface SubDetailRepository extends JpaRepository<SubjectDetailEntity, String> {
    
    
    // ID로 세부과목 조회 (수정용)
    Optional<SubjectDetailEntity> findBySubDetailId(String subDetailId);

    // educationId와 subDetailActive로 세부과목 리스트 조회
    List<SubjectDetailEntity> findByEducationIdAndSubDetailActive(String educationId, int subDetailActive);
}
