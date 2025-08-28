package com.jakdang.labs.api.post.repository;

import com.jakdang.labs.api.post.entity.PostViewHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostViewHistoryRepository extends JpaRepository<PostViewHistory, String> {
    
    /**
     * 특정 기간 내 게시물별 조회수를 계산합니다.
     * 
     * @param startDate 시작 날짜
     * @param pageable 페이징 정보
     * @return [postId, viewCount] 형태의 결과
     */
    @Query("""
        SELECT pvh.postId, COUNT(pvh.id) as viewCount
        FROM PostViewHistory pvh 
        WHERE pvh.viewedAt >= :startDate
        GROUP BY pvh.postId
        ORDER BY viewCount DESC
        """)
    List<Object[]> findPopularPostsByPeriod(
        @Param("startDate") LocalDateTime startDate, 
        Pageable pageable
    );
    
    /**
     * 특정 기간 내 특정 커뮤니티의 게시물별 조회수를 계산합니다.
     * 
     * @param startDate 시작 날짜
     * @param ownerId 커뮤니티 ID
     * @param pageable 페이징 정보
     * @return [postId, viewCount] 형태의 결과
     */
    @Query("""
        SELECT pvh.postId, COUNT(pvh.id) as viewCount
        FROM PostViewHistory pvh 
        WHERE pvh.viewedAt >= :startDate
        AND pvh.ownerId = :ownerId
        GROUP BY pvh.postId
        ORDER BY viewCount DESC
        """)
    List<Object[]> findPopularPostsByPeriodAndOwnerId(
        @Param("startDate") LocalDateTime startDate,
        @Param("ownerId") String ownerId,
        Pageable pageable
    );
    
    /**
     * 특정 기간 내 특정 게시물 목록에 대한 조회수를 계산합니다.
     * 
     * @param startDate 시작 날짜
     * @param postIds 게시물 ID 목록
     * @param pageable 페이징 정보
     * @return [postId, viewCount] 형태의 결과
     */
    @Query("""
        SELECT pvh.postId, COUNT(pvh.id) as viewCount
        FROM PostViewHistory pvh 
        WHERE pvh.viewedAt >= :startDate
        AND pvh.postId IN :postIds
        GROUP BY pvh.postId
        ORDER BY viewCount DESC
        """)
    List<Object[]> findPopularPostsByPeriodAndPostIds(
        @Param("startDate") LocalDateTime startDate,
        @Param("postIds") List<String> postIds,
        Pageable pageable
    );
    
    /**
     * 특정 기간 내 게시물별 고유 사용자 조회수를 계산합니다.
     * 
     * @param startDate 시작 날짜
     * @param pageable 페이징 정보
     * @return [postId, uniqueViewCount] 형태의 결과
     */
    @Query("""
        SELECT pvh.postId, COUNT(DISTINCT pvh.userId) as uniqueViewCount
        FROM PostViewHistory pvh 
        WHERE pvh.viewedAt >= :startDate
        GROUP BY pvh.postId
        ORDER BY uniqueViewCount DESC
        """)
    List<Object[]> findPopularPostsByUniqueViewers(
        @Param("startDate") LocalDateTime startDate, 
        Pageable pageable
    );
    
    /**
     * 특정 기간 내 특정 커뮤니티의 게시물별 고유 사용자 조회수를 계산합니다.
     * 
     * @param startDate 시작 날짜
     * @param ownerId 커뮤니티 ID
     * @param pageable 페이징 정보
     * @return [postId, uniqueViewCount] 형태의 결과
     */
    @Query("""
        SELECT pvh.postId, COUNT(DISTINCT pvh.userId) as uniqueViewCount
        FROM PostViewHistory pvh 
        WHERE pvh.viewedAt >= :startDate
        AND pvh.ownerId = :ownerId
        GROUP BY pvh.postId
        ORDER BY uniqueViewCount DESC
        """)
    List<Object[]> findPopularPostsByUniqueViewersAndOwnerId(
        @Param("startDate") LocalDateTime startDate,
        @Param("ownerId") String ownerId,
        Pageable pageable
    );
    
    /**
     * 특정 기간 내 특정 게시물 목록에 대한 고유 사용자 조회수를 계산합니다.
     * 
     * @param startDate 시작 날짜
     * @param postIds 게시물 ID 목록
     * @param pageable 페이징 정보
     * @return [postId, uniqueViewCount] 형태의 결과
     */
    @Query("""
        SELECT pvh.postId, COUNT(DISTINCT pvh.userId) as uniqueViewCount
        FROM PostViewHistory pvh 
        WHERE pvh.viewedAt >= :startDate
        AND pvh.postId IN :postIds
        GROUP BY pvh.postId
        ORDER BY uniqueViewCount DESC
        """)
    List<Object[]> findPopularPostsByUniqueViewersAndPostIds(
        @Param("startDate") LocalDateTime startDate,
        @Param("postIds") List<String> postIds,
        Pageable pageable
    );
    
    /**
     * 특정 사용자의 특정 게시물 조회 기록 개수를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param postId 게시물 ID
     * @param startDate 시작 날짜
     * @return 조회 기록 개수
     */
    @Query("""
        SELECT COUNT(pvh.id)
        FROM PostViewHistory pvh 
        WHERE pvh.userId = :userId 
        AND pvh.postId = :postId 
        AND pvh.viewedAt >= :startDate
        """)
    Long countByUserIdAndPostIdAndViewedAtAfter(
        @Param("userId") String userId,
        @Param("postId") String postId,
        @Param("startDate") LocalDateTime startDate
    );
} 