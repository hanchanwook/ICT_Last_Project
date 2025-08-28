package com.jakdang.labs.api.gemjjok.service;

import com.jakdang.labs.api.gemjjok.entity.LectureFile;
import com.jakdang.labs.api.gemjjok.repository.LectureFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureFileService {

    private final LectureFileRepository lectureFileRepository;

    @Transactional
    public LectureFile saveLectureFile(LectureFile lectureFile) {
        log.info("강의 파일 저장: {}", lectureFile);
        return lectureFileRepository.save(lectureFile);
    }

    public List<LectureFile> findByLectureId(String lectureId) {
        log.info("강의 파일 조회 - lectureId: {}", lectureId);
        return lectureFileRepository.findByLectureId(lectureId);
    }

    public List<LectureFile> findByLectureIdAndIsActive(String lectureId, Integer isActive) {
        log.info("강의 파일 조회 - lectureId: {}, isActive: {}", lectureId, isActive);
        return lectureFileRepository.findByLectureIdAndIsActive(lectureId, isActive);
    }
} 