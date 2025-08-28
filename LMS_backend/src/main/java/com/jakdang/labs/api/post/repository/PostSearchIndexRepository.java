package com.jakdang.labs.api.post.repository;

import com.jakdang.labs.api.post.entity.PostSearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostSearchIndexRepository extends JpaRepository<PostSearchIndex, String> {
    
    // 제목 검색
    @Query("SELECT p FROM PostSearchIndex p WHERE " +
           "(:communityId IS NULL OR p.communityId = :communityId) AND " +
           "(:boardId IS NULL OR p.boardId = :boardId) AND " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PostSearchIndex> searchByTitle(@Param("keyword") String keyword,
                                       @Param("communityId") String communityId,
                                       @Param("boardId") String boardId,
                                       Pageable pageable);
    
    // 내용 검색
    @Query("SELECT p FROM PostSearchIndex p WHERE " +
           "(:communityId IS NULL OR p.communityId = :communityId) AND " +
           "(:boardId IS NULL OR p.boardId = :boardId) AND " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PostSearchIndex> searchByContent(@Param("keyword") String keyword,
                                         @Param("communityId") String communityId,
                                         @Param("boardId") String boardId,
                                         Pageable pageable);
    
    // 작성자 검색
    @Query("SELECT p FROM PostSearchIndex p WHERE " +
           "(:communityId IS NULL OR p.communityId = :communityId) AND " +
           "(:boardId IS NULL OR p.boardId = :boardId) AND " +
           "LOWER(p.authorName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PostSearchIndex> searchByAuthor(@Param("keyword") String keyword,
                                        @Param("communityId") String communityId,
                                        @Param("boardId") String boardId,
                                        Pageable pageable);
    
    // 전체 검색 (제목 + 내용 + 작성자)
    @Query("SELECT p FROM PostSearchIndex p WHERE " +
           "(:communityId IS NULL OR p.communityId = :communityId) AND " +
           "(:boardId IS NULL OR p.boardId = :boardId) AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PostSearchIndex> searchAll(@Param("keyword") String keyword,
                                   @Param("communityId") String communityId,
                                   @Param("boardId") String boardId,
                                   Pageable pageable);

    // 게시판 목록으로 검색 (여러 게시판 동시 검색)
    @Query("SELECT p FROM PostSearchIndex p WHERE " +
           "(:communityId IS NULL OR p.communityId = :communityId) AND " +
           "(:boardIds IS NULL OR p.boardId IN :boardIds) AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PostSearchIndex> searchAllByBoardIds(@Param("keyword") String keyword,
                                             @Param("communityId") String communityId,
                                             @Param("boardIds") List<String> boardIds,
                                             Pageable pageable);
} 