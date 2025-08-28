package com.jakdang.labs.api.chanwook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.ClassroomEntity;

@Repository
public interface ClassroomRepository extends JpaRepository<ClassroomEntity, String> {
    
    // 모든 교실 조회 (활성화/비활성화 상관없이)
    @Query("SELECT c FROM ClassroomEntity c ORDER BY c.classCode")
    List<ClassroomEntity> findAllClassrooms();
    
    // 강의실 코드 존재 여부 확인
    boolean existsByClassCode(String classCode);

    // 활성 상태별 교실 수 조회
    long countByClassActive(int classActive);
    
    // 평균 수용 인원 조회 (VARCHAR를 숫자로 변환)
    @Query("SELECT AVG(CAST(c.classCapacity AS integer)) FROM ClassroomEntity c")
    Double findAverageCapacity();
    
    // 최대 수용 인원 조회 (VARCHAR를 숫자로 변환)
    @Query("SELECT MAX(CAST(c.classCapacity AS integer)) FROM ClassroomEntity c")
    Integer findMaxCapacity();
    
    // 최소 수용 인원 조회 (VARCHAR를 숫자로 변환)
    @Query("SELECT MIN(CAST(c.classCapacity AS integer)) FROM ClassroomEntity c")
    Integer findMinCapacity();
    
    // educationId로 활성화된 강의실 목록 조회
    List<ClassroomEntity> findByEducationIdAndClassActive(String educationId, int classActive);
    
    // educationId로 모든 강의실 목록 조회 (활성화/비활성화 상관없이)
    List<ClassroomEntity> findByEducationId(String educationId);

    List<ClassroomEntity> findByClassIdAndClassActive(String classId, int classActive);
    
    // 사용 가능한 빈 강의실 조회 (활성화 + 강의가 들어오지 않은 강의실)
    @Query("SELECT c FROM ClassroomEntity c WHERE c.classActive = 0 AND c.classRent = 0")
    List<ClassroomEntity> findAvailableClassrooms();
}
