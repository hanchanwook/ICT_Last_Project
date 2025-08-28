package com.jakdang.labs.api.jaegyeom.notice.repository;

import com.jakdang.labs.api.jaegyeom.notice.entity.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeEntity, String> {
    
    // 활성화된 공지사항만 조회
    List<NoticeEntity> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);
    
    // 특정 교육 ID의 공지사항을 생성일 기준 내림차순으로 조회
    List<NoticeEntity> findByEducationIdOrderByCreatedAtDesc(String educationId);
    
    // 제목이나 내용에 검색어가 포함된 공지사항 조회
    @Query("SELECT n FROM NoticeEntity n WHERE n.isActive = true AND (n.title LIKE %:searchTerm% OR n.content LIKE %:searchTerm%) ORDER BY n.createdAt DESC")
    List<NoticeEntity> findActiveNoticesBySearchTerm(@Param("searchTerm") String searchTerm);
}