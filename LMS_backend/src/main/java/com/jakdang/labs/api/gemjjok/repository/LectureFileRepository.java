package com.jakdang.labs.api.gemjjok.repository;

import com.jakdang.labs.api.gemjjok.entity.LectureFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureFileRepository extends JpaRepository<LectureFile, Long> {
    List<LectureFile> findByLectureId(String lectureId);
    List<LectureFile> findByLectureIdAndIsActive(String lectureId, Integer isActive);
    LectureFile findByMaterialId(String materialId);
    LectureFile findByFileKey(String fileKey);
} 